package co.kr.mini_spring.chat.service;

import co.kr.mini_spring.chat.domain.UserBlock;
import co.kr.mini_spring.chat.domain.repository.UserBlockRepository;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatBlockService {

    private final UserBlockRepository userBlockRepository;
    private final SocialMemberRepository socialMemberRepository;

    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new BusinessException(ResponseCode.CHAT_INVALID_REQUEST, "자기 자신을 차단할 수 없습니다.");
        }
        if (!socialMemberRepository.existsById(blockedId)) {
            throw new BusinessException(ResponseCode.MEMBER_NOT_FOUND);
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return;
        }
        userBlockRepository.save(
                UserBlock.builder()
                        .blockerId(blockerId)
                        .blockedId(blockedId)
                        .build()
        );
        log.info("[채팅 차단] blockerId={}, blockedId={}", blockerId, blockedId);
    }

    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
        log.info("[채팅 차단 해제] blockerId={}, blockedId={}", blockerId, blockedId);
    }
}

