package co.kr.mini_spring.chat.websocket;

import co.kr.mini_spring.chat.cache.ChatRoomAccessCacheService;
import co.kr.mini_spring.chat.domain.repository.ChatPermissionQueryRepository;
import co.kr.mini_spring.chat.dto.response.ChatWsErrorResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.security.MemberAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomAuthorizationChannelInterceptor implements ChannelInterceptor {

    private static final Pattern SEND_DESTINATION_PATTERN = Pattern.compile("^/app/chat/rooms/(\\d+)/send$");
    private static final Pattern SUBSCRIBE_DESTINATION_PATTERN = Pattern.compile("^/topic/chat/rooms/(\\d+)$");
    private static final String SESSION_AUTHORIZED_ROOM_IDS = "chatAuthorizedRoomIds";

    private final ChatPermissionQueryRepository chatPermissionQueryRepository;
    private final ChatRoomAccessCacheService chatRoomAccessCacheService;
    private final ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        Long roomId = extractRoomId(command, destination);
        if (roomId == null) {
            return message;
        }

        Long userId = extractUserId(accessor.getUser());
        if (userId == null) {
            deny(accessor.getUser(), command, destination, roomId, null, ResponseCode.UNAUTHENTICATED, "인증 정보가 없습니다.");
            return null;
        }

        if (isAuthorizedByCache(accessor, command, roomId, userId)) {
            return message;
        }

        var access = chatPermissionQueryRepository.getRoomAccess(roomId, userId).orElse(null);
        if (access == null) {
            deny(accessor.getUser(), command, destination, roomId, userId, ResponseCode.CHAT_ROOM_NOT_FOUND, null);
            return null;
        }
        if (access.banned()) {
            deny(accessor.getUser(), command, destination, roomId, userId, ResponseCode.CHAT_BANNED, null);
            return null;
        }
        if (!access.participant()) {
            deny(accessor.getUser(), command, destination, roomId, userId, ResponseCode.CHAT_NOT_PARTICIPANT, null);
            return null;
        }

        rememberAuthorizedRoom(accessor, roomId);
        chatRoomAccessCacheService.grant(roomId, userId);
        log.debug("[STOMP 권한 허용] command={}, destination={}, roomId={}, userId={}, source=db_check",
                command, destination, roomId, userId);

        return message;
    }

    private Long extractRoomId(StompCommand command, String destination) {
        if (destination == null || destination.isBlank()) {
            return null;
        }
        Matcher matcher = (command == StompCommand.SEND ? SEND_DESTINATION_PATTERN : SUBSCRIBE_DESTINATION_PATTERN)
                .matcher(destination);
        if (!matcher.matches()) {
            return null;
        }
        return Long.parseLong(matcher.group(1));
    }

    private Long extractUserId(Principal principal) {
        if (!(principal instanceof Authentication authentication)) {
            return null;
        }
        Object principalObject = authentication.getPrincipal();
        if (!(principalObject instanceof MemberAdapter memberAdapter)) {
            return null;
        }
        return memberAdapter.getMember().getId();
    }

    @SuppressWarnings("unchecked")
    private boolean isAuthorizedByCache(StompHeaderAccessor accessor,
                                        StompCommand command,
                                        Long roomId,
                                        Long userId) {
        if (command != StompCommand.SEND && command != StompCommand.SUBSCRIBE) {
            return false;
        }

        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        Set<Long> authorizedRoomIds = null;
        if (sessionAttributes != null) {
            Object value = sessionAttributes.get(SESSION_AUTHORIZED_ROOM_IDS);
            if (value instanceof Set<?> set) {
                authorizedRoomIds = (Set<Long>) set;
            }
        }

        boolean sessionHit = authorizedRoomIds != null && authorizedRoomIds.contains(roomId);
        boolean redisHit = chatRoomAccessCacheService.isAuthorized(roomId, userId);

        if (!sessionHit && !redisHit) {
            return false;
        }

        if (redisHit) {
            rememberAuthorizedRoom(accessor, roomId);
            log.debug("[STOMP 권한 캐시 허용] command={}, roomId={}, userId={}, sessionHit={}, redisHit={}",
                    command, roomId, userId, sessionHit, redisHit);
            return true;
        }

        // 세션만 있고 Redis 캐시가 사라진 경우(밴/퇴장/TTL 만료), DB 재검증으로 내려보냅니다.
        log.debug("[STOMP 권한 캐시 미스 재검증] command={}, roomId={}, userId={}, sessionHit={}, redisHit={}",
                command, roomId, userId, sessionHit, redisHit);
        return false;
    }

    @SuppressWarnings("unchecked")
    private void rememberAuthorizedRoom(StompHeaderAccessor accessor, Long roomId) {
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes == null) {
            return;
        }
        Object value = sessionAttributes.get(SESSION_AUTHORIZED_ROOM_IDS);
        Set<Long> roomIds;
        if (value instanceof Set<?> set) {
            roomIds = (Set<Long>) set;
        } else {
            roomIds = new HashSet<>();
            sessionAttributes.put(SESSION_AUTHORIZED_ROOM_IDS, roomIds);
        }
        roomIds.add(roomId);
    }

    private void deny(Principal principal,
                      StompCommand command,
                      String destination,
                      Long roomId,
                      Long userId,
                      ResponseCode responseCode,
                      String message) {
        if (principal == null) {
            log.warn("[STOMP 권한 차단] command={}, destination={}, roomId={}, userId={}, code={}, reason={}",
                    command, destination, roomId, userId, responseCode.getCode(),
                    message == null ? responseCode.getMessage() : message);
            return;
        }
        SimpMessagingTemplate messagingTemplate = messagingTemplateProvider.getIfAvailable();
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    new ChatWsErrorResponse(responseCode, message)
            );
        }
        log.warn("[STOMP 권한 차단] user={}, command={}, destination={}, roomId={}, userId={}, code={}, reason={}",
                principal.getName(), command, destination, roomId, userId, responseCode.getCode(),
                message == null ? responseCode.getMessage() : message);
    }
}
