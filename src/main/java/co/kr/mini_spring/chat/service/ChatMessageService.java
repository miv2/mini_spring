package co.kr.mini_spring.chat.service;

import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.Message;
import co.kr.mini_spring.chat.domain.repository.*;
import co.kr.mini_spring.chat.dto.request.ChatSendMessageRequest;
import co.kr.mini_spring.chat.dto.request.MarkReadRequest;
import co.kr.mini_spring.chat.dto.response.ChatMessageResponse;
import co.kr.mini_spring.chat.dto.response.ChatMessageSliceResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private static final int RATE_LIMIT_PER_SECOND = 3;
    private static final Duration RATE_LIMIT_WINDOW = Duration.ofSeconds(2);
    private static final Duration DUPLICATE_TTL = Duration.ofMinutes(5);
    private static final Duration DELETE_WINDOW = Duration.ofMinutes(5);
    private static final int DEFAULT_MESSAGE_SIZE = 50;
    private static final int MAX_MESSAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageRepository messageRepository;
    private final UserBlockRepository userBlockRepository;
    private final ChatMessageQueryRepository chatMessageQueryRepository;
    private final ChatPermissionQueryRepository chatPermissionQueryRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${file.public-base-url}")
    private String filePublicBaseUrl;

    @Value("${file.default-profile-image:/uploads/default-profile.png}")
    private String defaultProfileImage;

    @Transactional
    public ChatMessageResponse sendMessage(Long roomId, Long senderId, ChatSendMessageRequest request) {
        ChatPermissionQueryRepository.RoomAccess access = requireParticipantAccess(roomId, senderId);
        validateRateLimit(senderId);
        validateDuplicate(roomId, request.getClientMessageId());
        validateBlockedRelationship(access.type(), roomId, senderId);

        Conversation room = conversationRepository.findByIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_ROOM_NOT_FOUND));
        try {
            if (messageRepository.existsByConversationIdAndClientMessageId(roomId, request.getClientMessageId())) {
                throw new BusinessException(ResponseCode.CHAT_DUPLICATE_MESSAGE);
            }
            Message message = messageRepository.save(
                    Message.builder()
                            .conversationId(roomId)
                            .senderId(senderId)
                            .clientMessageId(request.getClientMessageId())
                            .type(request.getType())
                            .content(request.getContent())
                            .build()
            );
            room.updateLastMessage(buildPreview(request.getContent()));
            log.info("[채팅 메시지 전송] roomId={}, senderId={}, messageId={}", roomId, senderId, message.getId());
            return toResponse(message);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ResponseCode.CHAT_DUPLICATE_MESSAGE);
        }
    }

    public ChatMessageSliceResponse getMessages(Long roomId, Long userId, Long cursor, Integer size) {
        ChatPermissionQueryRepository.RoomAccess access = requireParticipantAccess(roomId, userId);
        int safeSize = sanitizeSize(size);
        List<Message> fetched = access.type() == ConversationType.GROUP
                ? chatMessageQueryRepository.findMessagesByCursorExcludingBlocked(roomId, userId, cursor, safeSize + 1)
                : chatMessageQueryRepository.findMessagesByCursor(roomId, cursor, safeSize + 1);
        boolean hasNext = fetched.size() > safeSize;
        List<Message> content = hasNext ? fetched.subList(0, safeSize) : fetched;
        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size() - 1).getId() : null;
        List<ChatMessageResponse> responses = content.stream()
                .map(this::toResponse)
                .toList();
        return new ChatMessageSliceResponse(responses, nextCursor, hasNext);
    }

    @Transactional
    public void markAsRead(Long roomId, Long userId, MarkReadRequest request) {
        requireParticipantAccess(roomId, userId);

        Long readMessageId = request.getLastReadMessageId();
        Message message = messageRepository.findByIdAndDeletedAtIsNull(readMessageId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_INVALID_REQUEST, "유효하지 않은 메시지 ID입니다."));
        if (!roomId.equals(message.getConversationId())) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_REQUEST, "요청한 메시지가 해당 채팅방에 속하지 않습니다.");
        }

        var participant = conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_NOT_PARTICIPANT));
        participant.markRead(readMessageId);
    }

    @Transactional
    public ChatMessageResponse deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findByIdAndDeletedAtIsNull(messageId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_INVALID_REQUEST, "삭제할 메시지를 찾을 수 없습니다."));
        if (!userId.equals(message.getSenderId())) {
            throw new BusinessException(ResponseCode.CHAT_FORBIDDEN, "본인이 보낸 메시지만 삭제할 수 있습니다.");
        }

        LocalDateTime deadline = message.getCreatedAt().plus(DELETE_WINDOW);
        if (LocalDateTime.now().isAfter(deadline)) {
            throw new BusinessException(ResponseCode.CHAT_DELETE_WINDOW_EXPIRED);
        }

        message.softDelete();
        refreshRoomLastMessage(message.getConversationId());
        log.info("[채팅 메시지 삭제] messageId={}, roomId={}, userId={}",
                messageId, message.getConversationId(), userId);
        ProfileInfo senderProfile = resolveSenderProfile(message);
        return ChatMessageResponse.deleted(
                message,
                userId,
                senderProfile.nickname(),
                senderProfile.profileImageUrl()
        );
    }

    private void validateBlockedRelationship(ConversationType type, Long roomId, Long senderId) {
        if (type != ConversationType.DIRECT) {
            return;
        }
        Long peerId = chatPermissionQueryRepository.findDirectPeerId(roomId, senderId).orElse(null);
        if (peerId == null) {
            return;
        }
        boolean blocked = userBlockRepository.existsByBlockerIdAndBlockedId(senderId, peerId)
                || userBlockRepository.existsByBlockerIdAndBlockedId(peerId, senderId);
        if (blocked) {
            throw new BusinessException(ResponseCode.CHAT_BLOCKED);
        }
    }

    private void validateRateLimit(Long userId) {
        try {
            long epochSecond = System.currentTimeMillis() / 1000;
            String key = "chat:rate:" + userId + ":" + epochSecond;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, RATE_LIMIT_WINDOW);
            }
            if (count != null && count > RATE_LIMIT_PER_SECOND) {
                throw new BusinessException(ResponseCode.CHAT_RATE_LIMIT_EXCEEDED);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 장애 시 전송 가용성을 우선하고 DB 유니크 제약으로 정합성을 보완합니다.
            log.warn("[채팅 rate-limit 스킵] userId={}, reason={}", userId, e.getMessage());
        }
    }

    private void validateDuplicate(Long roomId, String clientMessageId) {
        try {
            String key = "dup:msg:" + roomId + ":" + clientMessageId;
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", DUPLICATE_TTL);
            if (!Boolean.TRUE.equals(first)) {
                throw new BusinessException(ResponseCode.CHAT_DUPLICATE_MESSAGE);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // Redis 장애 시 중복은 DB unique(conversation_id, client_message_id)가 최종 보장합니다.
            log.warn("[채팅 중복키 캐시 스킵] roomId={}, clientMessageId={}, reason={}",
                    roomId, clientMessageId, e.getMessage());
        }
    }

    private ChatPermissionQueryRepository.RoomAccess requireParticipantAccess(Long roomId, Long userId) {
        ChatPermissionQueryRepository.RoomAccess access = chatPermissionQueryRepository.getRoomAccess(roomId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_ROOM_NOT_FOUND));
        if (access.banned()) {
            throw new BusinessException(ResponseCode.CHAT_BANNED);
        }
        if (!access.participant()) {
            throw new BusinessException(ResponseCode.CHAT_NOT_PARTICIPANT);
        }
        return access;
    }

    private void refreshRoomLastMessage(Long roomId) {
        Conversation room = conversationRepository.findByIdAndDeletedAtIsNull(roomId).orElse(null);
        if (room == null) {
            return;
        }
        chatMessageQueryRepository.findLatestMessageId(roomId)
                .flatMap(messageRepository::findById)
                .ifPresentOrElse(
                        latest -> room.updateLastMessage(latest.getCreatedAt(), buildPreview(latest.getContent())),
                        room::clearLastMessage
                );
    }

    private int sanitizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_MESSAGE_SIZE;
        }
        if (size < 1) {
            return DEFAULT_MESSAGE_SIZE;
        }
        return Math.min(size, MAX_MESSAGE_SIZE);
    }

    private String buildPreview(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        String trimmed = content.trim();
        return trimmed.length() > 255 ? trimmed.substring(0, 255) : trimmed;
    }

    private ChatMessageResponse toResponse(Message message) {
        ProfileInfo senderProfile = resolveSenderProfile(message);
        return ChatMessageResponse.of(message, senderProfile.nickname(), senderProfile.profileImageUrl());
    }

    private ProfileInfo resolveSenderProfile(Message message) {
        SocialMember sender = message.getSender();
        if (sender == null) {
            sender = socialMemberRepository.findById(message.getSenderId()).orElse(null);
        }
        if (sender == null) {
            return new ProfileInfo(null, defaultProfileImage);
        }
        return new ProfileInfo(
                sender.getNickname(),
                sender.getProfileImageUrl(filePublicBaseUrl, defaultProfileImage)
        );
    }

    private record ProfileInfo(String nickname, String profileImageUrl) {
    }
}
