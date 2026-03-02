package co.kr.mini_spring.chat.websocket;

import co.kr.mini_spring.chat.cache.ChatRoomAccessCacheService;
import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.repository.ChatPermissionQueryRepository;
import co.kr.mini_spring.global.security.MemberAdapter;
import co.kr.mini_spring.member.domain.SocialMember;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomAuthorizationChannelInterceptorTest {

    private static final String SESSION_AUTHORIZED_ROOM_IDS = "chatAuthorizedRoomIds";

    @Mock
    private ChatPermissionQueryRepository chatPermissionQueryRepository;
    @Mock
    private ChatRoomAccessCacheService chatRoomAccessCacheService;
    @Mock
    private ObjectProvider<SimpMessagingTemplate> messagingTemplateProvider;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private MessageChannel messageChannel;

    @InjectMocks
    private ChatRoomAuthorizationChannelInterceptor interceptor;

    @Test
    void subscribe_최초요청이면_DB검증후_권한캐시를_기록한다() {
        Long roomId = 10L;
        Long userId = 1L;
        Message<byte[]> message = createMessage(StompCommand.SUBSCRIBE, "/topic/chat/rooms/10", userId, null);

        when(chatRoomAccessCacheService.isAuthorized(roomId, userId)).thenReturn(false);
        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(
                        false, true, false, ConversationType.GROUP
                )));

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
        verify(chatPermissionQueryRepository, times(1)).getRoomAccess(roomId, userId);
        verify(chatRoomAccessCacheService, times(1)).grant(roomId, userId);

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(accessor).isNotNull();
        @SuppressWarnings("unchecked")
        Set<Long> roomIds = (Set<Long>) accessor.getSessionAttributes().get(SESSION_AUTHORIZED_ROOM_IDS);
        assertThat(roomIds).contains(roomId);
    }

    @Test
    void send_세션과_redis_캐시가_있으면_DB조회없이_통과한다() {
        Long roomId = 10L;
        Long userId = 1L;
        Message<byte[]> message = createMessage(StompCommand.SEND, "/app/chat/rooms/10/send", userId, Set.of(roomId));

        when(chatRoomAccessCacheService.isAuthorized(roomId, userId)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
        verify(chatRoomAccessCacheService, times(1)).isAuthorized(roomId, userId);
        verifyNoInteractions(chatPermissionQueryRepository);
        verify(chatRoomAccessCacheService, never()).grant(anyLong(), anyLong());
    }

    @Test
    void send_세션에만_권한이_있고_redis_캐시가_없으면_DB로_재검증한다() {
        Long roomId = 10L;
        Long userId = 1L;
        Message<byte[]> message = createMessage(StompCommand.SEND, "/app/chat/rooms/10/send", userId, Set.of(roomId));

        when(chatRoomAccessCacheService.isAuthorized(roomId, userId)).thenReturn(false);
        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(
                        false, true, false, ConversationType.GROUP
                )));

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isNotNull();
        verify(chatPermissionQueryRepository, times(1)).getRoomAccess(roomId, userId);
        verify(chatRoomAccessCacheService, times(1)).grant(roomId, userId);
    }

    private Message<byte[]> createMessage(StompCommand command,
                                          String destination,
                                          Long userId,
                                          Set<Long> authorizedRooms) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(createAuthentication(userId));
        accessor.setSessionAttributes(new HashMap<>());
        if (authorizedRooms != null) {
            accessor.getSessionAttributes().put(SESSION_AUTHORIZED_ROOM_IDS, new HashSet<>(authorizedRooms));
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Authentication createAuthentication(Long userId) {
        SocialMember member = mock(SocialMember.class);
        when(member.getId()).thenReturn(userId);
        MemberAdapter memberAdapter = mock(MemberAdapter.class);
        when(memberAdapter.getMember()).thenReturn(member);
        return new UsernamePasswordAuthenticationToken(memberAdapter, null);
    }
}
