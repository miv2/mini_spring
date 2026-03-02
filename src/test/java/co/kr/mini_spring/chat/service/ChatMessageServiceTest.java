package co.kr.mini_spring.chat.service;

import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.Message;
import co.kr.mini_spring.chat.domain.MessageType;
import co.kr.mini_spring.chat.domain.repository.*;
import co.kr.mini_spring.chat.dto.request.ChatSendMessageRequest;
import co.kr.mini_spring.chat.dto.request.MarkReadRequest;
import co.kr.mini_spring.chat.dto.response.ChatMessageResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private UserBlockRepository userBlockRepository;
    @Mock
    private ChatMessageQueryRepository chatMessageQueryRepository;
    @Mock
    private ChatPermissionQueryRepository chatPermissionQueryRepository;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    void sendMessage_초당_3건_초과시_예외가_발생한다() {
        Long roomId = 1L;
        Long userId = 2L;
        ChatSendMessageRequest request = sendMessageRequest("c1", "hello");

        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(false, true, false, ConversationType.GROUP)));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(4L);

        assertThatThrownBy(() -> chatMessageService.sendMessage(roomId, userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getResponseCode()).isEqualTo(ResponseCode.CHAT_RATE_LIMIT_EXCEEDED));
    }

    @Test
    void sendMessage_중복메시지키가_존재하면_예외가_발생한다() {
        Long roomId = 10L;
        Long userId = 20L;
        ChatSendMessageRequest request = sendMessageRequest("dup-1", "중복");

        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(false, true, false, ConversationType.GROUP)));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.sendMessage(roomId, userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getResponseCode()).isEqualTo(ResponseCode.CHAT_DUPLICATE_MESSAGE));
    }

    @Test
    void deleteMessage_5분이_지나면_삭제할_수_없다() {
        Long messageId = 5L;
        Long userId = 7L;
        Message oldMessage = Message.builder()
                .id(messageId)
                .conversationId(99L)
                .senderId(userId)
                .clientMessageId("old-msg")
                .type(MessageType.TEXT)
                .content("오래된 메시지")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();
        when(messageRepository.findByIdAndDeletedAtIsNull(messageId)).thenReturn(Optional.of(oldMessage));

        assertThatThrownBy(() -> chatMessageService.deleteMessage(messageId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getResponseCode()).isEqualTo(ResponseCode.CHAT_DELETE_WINDOW_EXPIRED));
    }

    @Test
    void markRead_다른_방의_메시지를_요청하면_실패한다() {
        Long roomId = 100L;
        Long userId = 200L;
        Long readMessageId = 300L;

        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(false, true, false, ConversationType.GROUP)));
        Message message = Message.builder()
                .id(readMessageId)
                .conversationId(999L)
                .senderId(400L)
                .clientMessageId("m1")
                .type(MessageType.TEXT)
                .content("text")
                .createdAt(LocalDateTime.now())
                .build();
        when(messageRepository.findByIdAndDeletedAtIsNull(readMessageId)).thenReturn(Optional.of(message));

        MarkReadRequest request = new MarkReadRequest();
        ReflectionTestUtils.setField(request, "lastReadMessageId", readMessageId);

        assertThatThrownBy(() -> chatMessageService.markAsRead(roomId, userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getResponseCode()).isEqualTo(ResponseCode.CHAT_INVALID_REQUEST));
    }

    @Test
    void sendMessage_레디스_rateLimit_장애여도_메시지저장은_성공한다() {
        Long roomId = 11L;
        Long userId = 22L;
        ChatSendMessageRequest request = sendMessageRequest("rate-fail-open", "메시지");

        Conversation room = Conversation.builder()
                .id(roomId)
                .type(ConversationType.GROUP)
                .ownerId(userId)
                .title("room")
                .build();
        Message saved = Message.builder()
                .id(101L)
                .conversationId(roomId)
                .senderId(userId)
                .clientMessageId("rate-fail-open")
                .type(MessageType.TEXT)
                .content("메시지")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(false, true, false, ConversationType.GROUP)));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenThrow(new RuntimeException("redis down"));
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(conversationRepository.findByIdAndDeletedAtIsNull(roomId)).thenReturn(Optional.of(room));
        when(messageRepository.existsByConversationIdAndClientMessageId(roomId, "rate-fail-open")).thenReturn(false);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        ChatMessageResponse response = chatMessageService.sendMessage(roomId, userId, request);

        assertThat(response.getMessageId()).isEqualTo(101L);
    }

    @Test
    void sendMessage_레디스_duplicate_장애여도_DB유니크로_진행된다() {
        Long roomId = 33L;
        Long userId = 44L;
        ChatSendMessageRequest request = sendMessageRequest("dup-fail-open", "메시지");

        Conversation room = Conversation.builder()
                .id(roomId)
                .type(ConversationType.GROUP)
                .ownerId(userId)
                .title("room")
                .build();
        Message saved = Message.builder()
                .id(202L)
                .conversationId(roomId)
                .senderId(userId)
                .clientMessageId("dup-fail-open")
                .type(MessageType.TEXT)
                .content("메시지")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(false, true, false, ConversationType.GROUP)));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenThrow(new RuntimeException("redis down"));
        when(conversationRepository.findByIdAndDeletedAtIsNull(roomId)).thenReturn(Optional.of(room));
        when(messageRepository.existsByConversationIdAndClientMessageId(roomId, "dup-fail-open")).thenReturn(false);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        ChatMessageResponse response = chatMessageService.sendMessage(roomId, userId, request);

        assertThat(response.getMessageId()).isEqualTo(202L);
    }

    private ChatSendMessageRequest sendMessageRequest(String clientMessageId, String content) {
        ChatSendMessageRequest request = new ChatSendMessageRequest();
        ReflectionTestUtils.setField(request, "clientMessageId", clientMessageId);
        ReflectionTestUtils.setField(request, "type", MessageType.TEXT);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
