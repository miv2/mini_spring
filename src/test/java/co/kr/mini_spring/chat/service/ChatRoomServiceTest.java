package co.kr.mini_spring.chat.service;

import co.kr.mini_spring.chat.cache.ChatRoomAccessCacheService;
import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationParticipant;
import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.repository.*;
import co.kr.mini_spring.chat.dto.request.CreateDirectRoomRequest;
import co.kr.mini_spring.chat.dto.response.ChatRoomResponse;
import co.kr.mini_spring.chat.dto.response.ChatRoomSliceResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.dao.DataIntegrityViolationException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;
    @Mock
    private ConversationBanRepository conversationBanRepository;
    @Mock
    private ChatRoomQueryRepository chatRoomQueryRepository;
    @Mock
    private ChatPermissionQueryRepository chatPermissionQueryRepository;
    @Mock
    private SocialMemberRepository socialMemberRepository;
    @Mock
    private ChatRoomAccessCacheService chatRoomAccessCacheService;

    @InjectMocks
    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatRoomService, "maxGroupMembers", 50);
    }

    @Test
    void createDirectRoom_자기자신이면_실패한다() {
        Long userId = 1L;
        CreateDirectRoomRequest request = new CreateDirectRoomRequest();
        ReflectionTestUtils.setField(request, "targetUserId", userId);
        when(socialMemberRepository.existsById(userId)).thenReturn(true);

        assertThatThrownBy(() -> chatRoomService.createDirectRoom(userId, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getResponseCode()).isEqualTo(ResponseCode.CHAT_INVALID_REQUEST));
    }

    @Test
    void joinGroupRoom_정원이_가득차면_실패한다() {
        Long roomId = 100L;
        Long userId = 200L;
        Conversation room = Conversation.builder()
                .id(roomId)
                .type(ConversationType.GROUP)
                .ownerId(1L)
                .title("group")
                .build();
        when(conversationRepository.findByIdAndDeletedAtIsNullForUpdate(roomId)).thenReturn(Optional.of(room));
        when(conversationBanRepository.existsByConversationIdAndUserId(roomId, userId)).thenReturn(false);
        when(conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, userId))
                .thenReturn(Optional.empty());
        when(conversationParticipantRepository.countByConversationIdAndDeletedAtIsNull(roomId)).thenReturn(50L);

        assertThatThrownBy(() -> chatRoomService.joinGroupRoom(roomId, userId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getResponseCode()).isEqualTo(ResponseCode.CHAT_ROOM_FULL));
    }

    @Test
    void invite_방장이_아니면_실패한다() {
        Long roomId = 10L;
        Long requesterId = 20L;
        Long targetUserId = 30L;
        Conversation room = Conversation.builder()
                .id(roomId)
                .type(ConversationType.GROUP)
                .ownerId(999L)
                .title("group")
                .build();
        when(conversationRepository.findByIdAndDeletedAtIsNullForUpdate(roomId)).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatRoomService.invite(roomId, requesterId, targetUserId))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getResponseCode()).isEqualTo(ResponseCode.CHAT_FORBIDDEN));
    }

    @Test
    void createDirectRoom_삭제된_기존방_uniqueKey_충돌이면_복구하여_재사용한다() {
        Long requesterId = 1L;
        Long targetUserId = 2L;
        String uniqueKey = "1_2";

        CreateDirectRoomRequest request = new CreateDirectRoomRequest();
        ReflectionTestUtils.setField(request, "targetUserId", targetUserId);

        Conversation deletedConversation = Conversation.builder()
                .id(77L)
                .type(ConversationType.DIRECT)
                .uniqueKey(uniqueKey)
                .ownerId(targetUserId)
                .deletedAt(LocalDateTime.now())
                .build();

        when(socialMemberRepository.existsById(requesterId)).thenReturn(true);
        when(socialMemberRepository.existsById(targetUserId)).thenReturn(true);
        when(conversationRepository.findByUniqueKey(uniqueKey)).thenReturn(Optional.empty());
        when(conversationRepository.save(org.mockito.ArgumentMatchers.any(Conversation.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));
        when(conversationRepository.findByUniqueKeyIncludingDeleted(uniqueKey))
                .thenReturn(Optional.of(deletedConversation));

        when(conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(77L, requesterId))
                .thenReturn(Optional.empty());
        when(conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(77L, targetUserId))
                .thenReturn(Optional.empty());
        when(conversationParticipantRepository.findByConversationIdAndUserId(77L, requesterId))
                .thenReturn(Optional.empty());
        when(conversationParticipantRepository.findByConversationIdAndUserId(77L, targetUserId))
                .thenReturn(Optional.empty());
        when(conversationParticipantRepository.save(org.mockito.ArgumentMatchers.any(ConversationParticipant.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ChatRoomResponse response = chatRoomService.createDirectRoom(requesterId, request);

        assertThat(response.getRoomId()).isEqualTo(77L);
        assertThat(deletedConversation.getDeletedAt()).isNull();
    }

    @Test
    void joinGroupRoom_참여자_insert_유니크충돌이면_무시하고_성공한다() {
        Long roomId = 100L;
        Long userId = 200L;
        Conversation room = Conversation.builder()
                .id(roomId)
                .type(ConversationType.GROUP)
                .ownerId(1L)
                .title("group")
                .build();
        ConversationParticipant activeParticipant = ConversationParticipant.builder()
                .id(999L)
                .conversationId(roomId)
                .userId(userId)
                .build();

        when(conversationRepository.findByIdAndDeletedAtIsNullForUpdate(roomId)).thenReturn(Optional.of(room));
        when(conversationBanRepository.existsByConversationIdAndUserId(roomId, userId)).thenReturn(false);
        when(conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, userId))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.of(activeParticipant));
        when(conversationParticipantRepository.findByConversationIdAndUserId(roomId, userId))
                .thenReturn(Optional.empty());
        when(conversationParticipantRepository.countByConversationIdAndDeletedAtIsNull(roomId)).thenReturn(1L);
        when(conversationParticipantRepository.save(org.mockito.ArgumentMatchers.any(ConversationParticipant.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate participant"));

        assertThatCode(() -> chatRoomService.joinGroupRoom(roomId, userId))
                .doesNotThrowAnyException();
    }

    @Test
    void getMyRooms_커서사이즈기반으로_슬라이스를_반환한다() {
        Long userId = 1L;
        ChatRoomResponse room1 = new ChatRoomResponse(30L, ConversationType.GROUP, "r1", 1L, "m1", LocalDateTime.now(), 0L, 3L);
        ChatRoomResponse room2 = new ChatRoomResponse(20L, ConversationType.GROUP, "r2", 1L, "m2", LocalDateTime.now().minusMinutes(1), 0L, 2L);
        ChatRoomResponse room3 = new ChatRoomResponse(10L, ConversationType.GROUP, "r3", 1L, "m3", LocalDateTime.now().minusMinutes(2), 0L, 2L);

        when(chatRoomQueryRepository.findMyRooms(userId, null, 3)).thenReturn(List.of(room1, room2, room3));

        ChatRoomSliceResponse response = chatRoomService.getMyRooms(userId, null, 2);

        assertThat(response.isHasNext()).isTrue();
        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getNextCursor()).isEqualTo(20L);
        verify(chatRoomQueryRepository).findMyRooms(userId, null, 3);
    }

    @Test
    void getPublicGroupRooms_사이즈가_없으면_기본값20을_사용한다() {
        Long userId = 1L;
        when(chatRoomQueryRepository.findPublicGroupRooms(userId, 50, null, 21)).thenReturn(List.of());

        ChatRoomSliceResponse response = chatRoomService.getPublicGroupRooms(userId, null, null);

        assertThat(response.isHasNext()).isFalse();
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        verify(chatRoomQueryRepository).findPublicGroupRooms(userId, 50, null, 21);
    }
}
