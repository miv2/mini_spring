package co.kr.mini_spring.member.service;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import co.kr.mini_spring.global.common.file.service.FileService;
import co.kr.mini_spring.global.common.file.util.FileUrlResolver;
import co.kr.mini_spring.global.common.response.ResponseCode;
import co.kr.mini_spring.global.common.exception.BusinessException;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import co.kr.mini_spring.member.domain.repository.SocialMemberQueryRepository;
import co.kr.mini_spring.member.dto.response.MemberResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;

/**
 * 회원 서비스 (OAuth2 전용)
 * - 프로필 조회
 * - 프로필 이미지 업데이트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final SocialMemberRepository socialMemberRepository;
    private final SocialMemberQueryRepository socialMemberQueryRepository;
    private final FileService fileService;

    @Value("${file.public-base-url}")
    private String publicBaseUrl;

    @Value("${file.default-profile-image}")
    private String defaultProfileImage;

    /**
     * 내 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public MemberResponse getMyInfo(Long memberId) {
        SocialMember member = socialMemberQueryRepository.findByIdWithProfileImage(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));
        return new MemberResponse(member, publicBaseUrl, defaultProfileImage);
    }

    /**
     * 사용자의 프로필 이미지를 업데이트합니다.
     */
    @Transactional
    public String updateProfileImage(Long memberId, MultipartFile file) {
        // 최적화된 Querydsl 조회 사용
        SocialMember member = socialMemberQueryRepository.findByIdWithProfileImage(memberId)
                .orElseThrow(() -> new BusinessException(ResponseCode.MEMBER_NOT_FOUND));

        StoredFile oldImage = member.getProfileImage();
        // 1. 새 이미지 파일 저장
        StoredFile imageFile = fileService.uploadImage(file);

        // 2. 멤버 엔티티의 프로필 이미지 업데이트
        member.updateProfileImage(imageFile);

        // 3. 기존 이미지는 커밋 이후에 물리 삭제 (기본 프로필 이미지는 제외)
        if (oldImage != null && !isDefaultProfileImage(oldImage)) {
            fileService.deleteFile(oldImage);
        }

        log.info("[프로필 이미지 업데이트 성공] memberId={}, fileId={}", memberId, imageFile.getId());

        return FileUrlResolver.resolve(publicBaseUrl, imageFile);
    }

    private boolean isDefaultProfileImage(StoredFile image) {
        String defaultFileName = Paths.get(defaultProfileImage).getFileName().toString();
        return defaultFileName.equalsIgnoreCase(image.getStoredName());
    }
}
