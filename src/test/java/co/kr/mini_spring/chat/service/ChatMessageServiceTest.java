package co.kr.mini_spring.chat.service;

import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.Message;
import co.kr.mini_spring.chat.domain.MessageType;
import co.kr.mini_spring.chat.domain.repository.*;
import co.kr.mini_spring.chat.dto.request.ChatSendMessageRequest;
import co.kr.mini_spring.chat.dto.request.MarkReadRequest;
import co.kr.mini_spring.chat.dto.response.ChatMessageResponse;
import co.kr.mini_spring.chat.dto.response.ChatMessageSliceResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
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
import java.util.List;
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
    private SocialMemberRepository socialMemberRepository;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatMessageService, "filePublicBaseUrl", "https://minispring.duckdns.org/uploads/");
        ReflectionTestUtils.setField(chatMessageService, "defaultProfileImage", "/uploads/default-profile.png");
    }

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
    void deleteMessage_성공시_삭제이벤트용_응답을_반환한다() {
        Long messageId = 6L;
        Long roomId = 99L;
        Long userId = 7L;
        Message message = Message.builder()
                .id(messageId)
                .conversationId(roomId)
                .senderId(userId)
                .clientMessageId("delete-msg")
                .type(MessageType.TEXT)
                .content("삭제 전 메시지")
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();
        Conversation room = Conversation.builder()
                .id(roomId)
                .type(ConversationType.DIRECT)
                .ownerId(userId)
                .build();

        when(messageRepository.findByIdAndDeletedAtIsNull(messageId)).thenReturn(Optional.of(message));
        when(conversationRepository.findByIdAndDeletedAtIsNull(roomId)).thenReturn(Optional.of(room));
        when(chatMessageQueryRepository.findLatestMessageId(roomId)).thenReturn(Optional.empty());

        ChatMessageResponse response = chatMessageService.deleteMessage(messageId, userId);

        assertThat(response.isDeleted()).isTrue();
        assertThat(response.getContent()).isEqualTo("삭제된 메시지입니다.");
        assertThat(response.getEventType()).isEqualTo("MESSAGE_DELETED");
        assertThat(response.getDeletedBy()).isEqualTo(userId);
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
    void getMessages_그룹채팅에서_차단한사용자_메시지는_제외조회한다() {
        Long roomId = 100L;
        Long userId = 200L;

        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(false, true, false, ConversationType.GROUP)));

        Message visible = Message.builder()
                .id(300L)
                .conversationId(roomId)
                .senderId(201L)
                .clientMessageId("m-visible")
                .type(MessageType.TEXT)
                .content("보이는 메시지")
                .createdAt(LocalDateTime.now())
                .build();

        when(chatMessageQueryRepository.findMessagesByCursorExcludingBlocked(roomId, userId, null, 51))
                .thenReturn(List.of(visible));

        ChatMessageSliceResponse response = chatMessageService.getMessages(roomId, userId, null, 50);

        assertThat(response.getMessages()).hasSize(1);
        verify(chatMessageQueryRepository, times(1))
                .findMessagesByCursorExcludingBlocked(roomId, userId, null, 51);
        verify(chatMessageQueryRepository, never())
                .findMessagesByCursor(anyLong(), any(), anyInt());
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

    @Test
    void sendMessage_응답에_발신자_닉네임과_프로필이미지가_포함된다() {
        Long roomId = 55L;
        Long userId = 66L;
        ChatSendMessageRequest request = sendMessageRequest("with-profile", "프로필 포함");

        Conversation room = Conversation.builder()
                .id(roomId)
                .type(ConversationType.GROUP)
                .ownerId(userId)
                .title("room")
                .build();
        Message saved = Message.builder()
                .id(303L)
                .conversationId(roomId)
                .senderId(userId)
                .clientMessageId("with-profile")
                .type(MessageType.TEXT)
                .content("프로필 포함")
                .createdAt(LocalDateTime.now())
                .build();
        SocialMember sender = SocialMember.builder()
                .id(userId)
                .email("sender@test.com")
                .provider(MemberProvider.GOOGLE)
                .oauthId("oauth-1")
                .name("sender")
                .nickname("보낸이")
                .build();

        when(chatPermissionQueryRepository.getRoomAccess(roomId, userId))
                .thenReturn(Optional.of(new ChatPermissionQueryRepository.RoomAccess(false, true, false, ConversationType.GROUP)));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.setIfAbsent(anyString(), eq("1"), any(Duration.class))).thenReturn(true);
        when(conversationRepository.findByIdAndDeletedAtIsNull(roomId)).thenReturn(Optional.of(room));
        when(messageRepository.existsByConversationIdAndClientMessageId(roomId, "with-profile")).thenReturn(false);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(socialMemberRepository.findById(userId)).thenReturn(Optional.of(sender));

        ChatMessageResponse response = chatMessageService.sendMessage(roomId, userId, request);

        assertThat(response.getSenderNickname()).isEqualTo("보낸이");
        assertThat(response.getSenderProfileImageUrl()).isEqualTo("/uploads/default-profile.png");
    }

    private ChatSendMessageRequest sendMessageRequest(String clientMessageId, String content) {
        ChatSendMessageRequest request = new ChatSendMessageRequest();
        ReflectionTestUtils.setField(request, "clientMessageId", clientMessageId);
        ReflectionTestUtils.setField(request, "type", MessageType.TEXT);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}
