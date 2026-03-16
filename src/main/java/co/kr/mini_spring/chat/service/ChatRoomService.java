package co.kr.mini_spring.chat.service;

import co.kr.mini_spring.chat.cache.ChatRoomAccessCacheService;
import co.kr.mini_spring.chat.domain.Conversation;
import co.kr.mini_spring.chat.domain.ConversationBan;
import co.kr.mini_spring.chat.domain.ConversationParticipant;
import co.kr.mini_spring.chat.domain.ConversationType;
import co.kr.mini_spring.chat.domain.repository.*;
import co.kr.mini_spring.chat.dto.request.CreateDirectRoomRequest;
import co.kr.mini_spring.chat.dto.request.CreateGroupRoomRequest;
import co.kr.mini_spring.chat.dto.response.ChatRoomResponse;
import co.kr.mini_spring.chat.dto.response.ChatRoomSliceResponse;
import co.kr.mini_spring.chat.dto.response.ChatParticipantResponse;
import co.kr.mini_spring.chat.dto.response.ChatBanResponse;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {
    private static final int DEFAULT_ROOM_PAGE_SIZE = 20;
    private static final int MAX_ROOM_PAGE_SIZE = 100;


    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationBanRepository conversationBanRepository;
    private final ChatRoomQueryRepository chatRoomQueryRepository;
    private final ChatPermissionQueryRepository chatPermissionQueryRepository;
    private final SocialMemberRepository socialMemberRepository;
    private final ChatRoomAccessCacheService chatRoomAccessCacheService;

    @Value("${chat.room.max-group-members:50}")
    private int maxGroupMembers;

    @Value("${file.public-base-url}")
    private String filePublicBaseUrl;

    @Value("${file.default-profile-image:/uploads/default-profile.png}")
    private String defaultProfileImage;

    @Transactional
    public ChatRoomResponse createDirectRoom(Long requesterId, CreateDirectRoomRequest request) {
        ensureMemberExists(requesterId);
        Long targetUserId = request.getTargetUserId();
        if (requesterId.equals(targetUserId)) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_REQUEST, "자기 자신과의 채팅방은 생성할 수 없습니다.");
        }
        ensureMemberExists(targetUserId);

        String uniqueKey = buildDirectUniqueKey(requesterId, targetUserId);
        Conversation existing = conversationRepository.findByUniqueKey(uniqueKey).orElse(null);
        if (existing != null) {
            ensureParticipant(existing.getId(), requesterId);
            ensureParticipant(existing.getId(), targetUserId);
            chatRoomAccessCacheService.grantAfterCommit(existing.getId(), requesterId);
            chatRoomAccessCacheService.grantAfterCommit(existing.getId(), targetUserId);
            return enrichSingleRoom(requesterId, new ChatRoomResponse(existing));
        }

        Conversation conversation;
        try {
            conversation = conversationRepository.save(
                    Conversation.builder()
                            .type(ConversationType.DIRECT)
                            .uniqueKey(uniqueKey)
                            .ownerId(requesterId)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            conversation = conversationRepository.findByUniqueKeyIncludingDeleted(uniqueKey)
                    .map(found -> {
                        found.reactivate(requesterId);
                        return found;
                    })
                    .orElseThrow(() -> e);
        }

        ensureParticipant(conversation.getId(), requesterId);
        ensureParticipant(conversation.getId(), targetUserId);
        chatRoomAccessCacheService.grantAfterCommit(conversation.getId(), requesterId);
        chatRoomAccessCacheService.grantAfterCommit(conversation.getId(), targetUserId);

        log.info("[채팅방 생성] type=DIRECT roomId={}, requester={}, target={}",
                conversation.getId(), requesterId, targetUserId);
        return enrichSingleRoom(requesterId, new ChatRoomResponse(conversation));
    }

    @Transactional
    public ChatRoomResponse createGroupRoom(Long requesterId, CreateGroupRoomRequest request) {
        String title = request.getTitle().trim();
        if (title.isEmpty()) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_REQUEST, "그룹 채팅방 제목은 비어 있을 수 없습니다.");
        }
        Conversation conversation = conversationRepository.save(
                Conversation.builder()
                        .type(ConversationType.GROUP)
                        .ownerId(requesterId)
                        .title(title)
                        .build()
        );
        ensureParticipant(conversation.getId(), requesterId);
        chatRoomAccessCacheService.grantAfterCommit(conversation.getId(), requesterId);
        log.info("[채팅방 생성] type=GROUP roomId={}, ownerId={}", conversation.getId(), requesterId);
        return enrichSingleRoom(requesterId, new ChatRoomResponse(conversation));
    }

    public ChatRoomSliceResponse getMyRooms(Long userId, Long cursor, Integer size) {
        int safeSize = sanitizePageSize(size);
        Long safeCursor = sanitizeCursor(cursor);
        List<ChatRoomResponse> fetched = chatRoomQueryRepository.findMyRooms(userId, safeCursor, safeSize + 1);
        boolean hasNext = fetched.size() > safeSize;
        List<ChatRoomResponse> content = hasNext ? fetched.subList(0, safeSize) : fetched;
        List<ChatRoomResponse> enriched = enrichRoomsForDisplay(userId, content);
        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size() - 1).getRoomId() : null;
        return new ChatRoomSliceResponse(enriched, nextCursor, hasNext);
    }

    public ChatRoomSliceResponse getPublicGroupRooms(Long userId, Long cursor, Integer size) {
        int safeSize = sanitizePageSize(size);
        Long safeCursor = sanitizeCursor(cursor);
        List<ChatRoomResponse> fetched =
                chatRoomQueryRepository.findPublicGroupRooms(userId, maxGroupMembers, safeCursor, safeSize + 1);
        boolean hasNext = fetched.size() > safeSize;
        List<ChatRoomResponse> content = hasNext ? fetched.subList(0, safeSize) : fetched;
        List<ChatRoomResponse> enriched = enrichRoomsForDisplay(userId, content);
        Long nextCursor = hasNext && !content.isEmpty() ? content.get(content.size() - 1).getRoomId() : null;
        return new ChatRoomSliceResponse(enriched, nextCursor, hasNext);
    }

    @Transactional
    public void joinGroupRoom(Long roomId, Long userId) {
        Conversation room = getRoomForUpdate(roomId);
        if (!room.isGroup()) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_ROOM_TYPE);
        }
        if (conversationBanRepository.existsByConversationIdAndUserId(roomId, userId)) {
            throw new BusinessException(ResponseCode.CHAT_BANNED);
        }
        if (conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, userId).isPresent()) {
            return;
        }
        validateRoomCapacity(roomId);
        ensureParticipant(roomId, userId);
        chatRoomAccessCacheService.grantAfterCommit(roomId, userId);
        log.info("[채팅방 입장] roomId={}, userId={}", roomId, userId);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        ConversationParticipant participant = conversationParticipantRepository
                .findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, userId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_NOT_PARTICIPANT));
        participant.leave();
        chatRoomAccessCacheService.revokeAfterCommit(roomId, userId);
        log.info("[채팅방 나가기] roomId={}, userId={}", roomId, userId);
    }

    @Transactional
    public void invite(Long roomId, Long requesterId, Long targetUserId) {
        Conversation room = getRoomForUpdate(roomId);
        if (!requesterId.equals(room.getOwnerId())) {
            throw new BusinessException(ResponseCode.CHAT_FORBIDDEN);
        }
        if (!room.isGroup()) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_ROOM_TYPE);
        }
        ensureMemberExists(targetUserId);
        if (conversationBanRepository.existsByConversationIdAndUserId(roomId, targetUserId)) {
            throw new BusinessException(ResponseCode.CHAT_BANNED);
        }
        if (conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, targetUserId).isPresent()) {
            return;
        }
        validateRoomCapacity(roomId);
        ensureParticipant(roomId, targetUserId);
        chatRoomAccessCacheService.grantAfterCommit(roomId, targetUserId);
        log.info("[채팅방 초대] roomId={}, ownerId={}, targetUserId={}", roomId, requesterId, targetUserId);
    }

    @Transactional
    public void kick(Long roomId, Long requesterId, Long targetUserId) {
        ensureOwner(roomId, requesterId);
        Conversation room = getRoom(roomId);
        if (!room.isGroup()) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_ROOM_TYPE);
        }
        if (requesterId.equals(targetUserId)) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_REQUEST, "방장은 자기 자신을 강퇴할 수 없습니다.");
        }

        ConversationParticipant target = conversationParticipantRepository
                .findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, targetUserId)
                .orElse(null);
        if (target != null) {
            target.leave();
        }

        if (!conversationBanRepository.existsByConversationIdAndUserId(roomId, targetUserId)) {
            conversationBanRepository.save(
                    ConversationBan.builder()
                            .conversationId(roomId)
                            .userId(targetUserId)
                            .bannedBy(requesterId)
                            .build()
            );
        }
        chatRoomAccessCacheService.revokeAfterCommit(roomId, targetUserId);

        log.info("[채팅방 강퇴] roomId={}, ownerId={}, targetUserId={}", roomId, requesterId, targetUserId);
    }

    @Transactional
    public void unban(Long roomId, Long requesterId, Long targetUserId) {
        ensureOwner(roomId, requesterId);
        conversationBanRepository.deleteByConversationIdAndUserId(roomId, targetUserId);
        log.info("[채팅방 밴 해제] roomId={}, ownerId={}, targetUserId={}", roomId, requesterId, targetUserId);
    }

    public List<ChatParticipantResponse> getParticipants(Long roomId, Long requesterId) {
        var access = chatPermissionQueryRepository.getRoomAccess(roomId, requesterId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_ROOM_NOT_FOUND));
        if (!access.participant() && !access.owner()) {
            throw new BusinessException(ResponseCode.CHAT_FORBIDDEN);
        }

        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversationIdAndDeletedAtIsNull(roomId);
        List<Long> userIds = participants.stream()
                .map(ConversationParticipant::getUserId)
                .collect(Collectors.toList());

        return socialMemberRepository.findAllById(userIds).stream()
                .map(member -> new ChatParticipantResponse(
                        member.getId(),
                        member.getNickname(),
                        normalizeProfileImageUrl(member.getProfileImageUrl(filePublicBaseUrl, defaultProfileImage))))
                .collect(Collectors.toList());
    }

    public List<ChatBanResponse> getBans(Long roomId, Long requesterId) {
        ensureOwner(roomId, requesterId);
        Conversation room = getRoom(roomId);
        if (!room.isGroup()) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_ROOM_TYPE);
        }
        List<ConversationBan> bans = conversationBanRepository.findByConversationId(roomId);
        List<Long> userIds = bans.stream()
                .map(ConversationBan::getUserId)
                .collect(Collectors.toList());

        return socialMemberRepository.findAllById(userIds).stream()
                .map(member -> new ChatBanResponse(
                        member.getId(),
                        member.getNickname(),
                        normalizeProfileImageUrl(member.getProfileImageUrl(filePublicBaseUrl, defaultProfileImage))))
                .collect(Collectors.toList());
    }

    private void ensureOwner(Long roomId, Long requesterId) {
        var access = chatPermissionQueryRepository.getRoomAccess(roomId, requesterId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_ROOM_NOT_FOUND));
        if (!access.owner()) {
            throw new BusinessException(ResponseCode.CHAT_FORBIDDEN);
        }
    }

    private Conversation getRoom(Long roomId) {
        return conversationRepository.findByIdAndDeletedAtIsNull(roomId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_ROOM_NOT_FOUND));
    }

    private Conversation getRoomForUpdate(Long roomId) {
        return conversationRepository.findByIdAndDeletedAtIsNullForUpdate(roomId)
                .orElseThrow(() -> new BusinessException(ResponseCode.CHAT_ROOM_NOT_FOUND));
    }

    private void ensureParticipant(Long roomId, Long userId) {
        if (conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, userId).isPresent()) {
            return;
        }
        ConversationParticipant existing = conversationParticipantRepository.findByConversationIdAndUserId(roomId, userId)
                .orElse(null);
        if (existing != null) {
            existing.rejoin();
            return;
        }
        try {
            conversationParticipantRepository.save(
                    ConversationParticipant.builder()
                            .conversationId(roomId)
                            .userId(userId)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // 동시 요청으로 이미 등록된 경우 idempotent 하게 성공 처리
            if (conversationParticipantRepository.findByConversationIdAndUserIdAndDeletedAtIsNull(roomId, userId).isPresent()) {
                return;
            }
            throw e;
        }
    }

    private void validateRoomCapacity(Long roomId) {
        long currentSize = conversationParticipantRepository.countByConversationIdAndDeletedAtIsNull(roomId);
        if (currentSize >= maxGroupMembers) {
            throw new BusinessException(ResponseCode.CHAT_ROOM_FULL);
        }
    }

    private void ensureMemberExists(Long userId) {
        if (!socialMemberRepository.existsById(userId)) {
            throw new BusinessException(ResponseCode.MEMBER_NOT_FOUND);
        }
    }

    private String buildDirectUniqueKey(Long userA, Long userB) {
        return List.of(userA, userB).stream()
                .sorted(Comparator.naturalOrder())
                .map(String::valueOf)
                .reduce((a, b) -> a + "_" + b)
                .orElseThrow();
    }

    private int sanitizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_ROOM_PAGE_SIZE;
        }
        return Math.min(size, MAX_ROOM_PAGE_SIZE);
    }

    private Long sanitizeCursor(Long cursor) {
        if (cursor == null) {
            return null;
        }
        return conversationRepository.existsByIdAndDeletedAtIsNull(cursor) ? cursor : null;
    }

    private ChatRoomResponse enrichSingleRoom(Long userId, ChatRoomResponse room) {
        List<ChatRoomResponse> enriched = enrichRoomsForDisplay(userId, List.of(room));
        return enriched.isEmpty() ? room : enriched.get(0);
    }

    private List<ChatRoomResponse> enrichRoomsForDisplay(Long userId, List<ChatRoomResponse> rooms) {
        if (rooms == null || rooms.isEmpty()) {
            return List.of();
        }

        List<Long> directRoomIds = rooms.stream()
                .filter(room -> room.getType() == ConversationType.DIRECT)
                .map(ChatRoomResponse::getRoomId)
                .toList();
        if (directRoomIds.isEmpty()) {
            return rooms.stream()
                    .map(this::withFallbackDisplayNameForGroup)
                    .toList();
        }

        Map<Long, Long> roomPeerIds = resolvePeerIds(userId, directRoomIds);
        List<Long> peerIds = roomPeerIds.values().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, SocialMember> peerMembers = socialMemberRepository.findAllById(peerIds).stream()
                .collect(Collectors.toMap(SocialMember::getId, member -> member));

        return rooms.stream()
                .map(room -> enrichRoomDisplay(room, roomPeerIds, peerMembers))
                .toList();
    }

    private ChatRoomResponse withFallbackDisplayNameForGroup(ChatRoomResponse room) {
        String displayName = room.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = room.getTitle();
        }
        return new ChatRoomResponse(
                room.getRoomId(),
                room.getType(),
                room.getTitle(),
                room.getOwnerId(),
                room.getLastMessagePreview(),
                room.getLastMessageAt(),
                room.getUnreadCount(),
                room.getParticipantCount(),
                displayName,
                room.getProfileImageUrl()
        );
    }

    private ChatRoomResponse enrichRoomDisplay(ChatRoomResponse room,
                                               Map<Long, Long> roomPeerIds,
                                               Map<Long, SocialMember> peerMembers) {
        if (room.getType() != ConversationType.DIRECT) {
            return withFallbackDisplayNameForGroup(room);
        }

        Long peerId = roomPeerIds.get(room.getRoomId());
        SocialMember peer = peerId == null ? null : peerMembers.get(peerId);

        String displayName = room.getDisplayName();
        String profileImageUrl = room.getProfileImageUrl();
        if (peer != null) {
            displayName = peer.getNickname();
            profileImageUrl = normalizeProfileImageUrl(peer.getProfileImageUrl(filePublicBaseUrl, defaultProfileImage));
        } else {
            if (displayName == null || displayName.isBlank()) {
                displayName = room.getTitle();
            }
            profileImageUrl = normalizeProfileImageUrl(defaultProfileImage);
        }

        return new ChatRoomResponse(
                room.getRoomId(),
                room.getType(),
                room.getTitle(),
                room.getOwnerId(),
                room.getLastMessagePreview(),
                room.getLastMessageAt(),
                room.getUnreadCount(),
                room.getParticipantCount(),
                displayName,
                profileImageUrl
        );
    }

    private Map<Long, Long> resolvePeerIds(Long userId, List<Long> roomIds) {
        List<ConversationParticipant> participants =
                conversationParticipantRepository.findByConversationIdInAndDeletedAtIsNull(roomIds);
        Map<Long, Long> roomPeerIds = new HashMap<>();
        for (ConversationParticipant participant : participants) {
            if (userId.equals(participant.getUserId())) {
                continue;
            }
            roomPeerIds.putIfAbsent(participant.getConversationId(), participant.getUserId());
        }
        return roomPeerIds;
    }

    private String normalizeProfileImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return imageUrl;
        }
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl;
        }
        if (filePublicBaseUrl == null || filePublicBaseUrl.isBlank()) {
            return imageUrl;
        }
        try {
            URI uri = URI.create(filePublicBaseUrl);
            String scheme = uri.getScheme();
            String authority = uri.getAuthority();
            if (scheme == null || authority == null) {
                return imageUrl;
            }
            String origin = scheme + "://" + authority;
            if (imageUrl.startsWith("/")) {
                return origin + imageUrl;
            }
            return origin + "/" + imageUrl;
        } catch (IllegalArgumentException e) {
            return imageUrl;
        }
    }
}
