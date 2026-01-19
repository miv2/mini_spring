package co.kr.mini_spring.member.service;

import co.kr.mini_spring.global.common.file.domain.ImageFile;
import co.kr.mini_spring.global.common.file.service.FileService;
import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.member.domain.repository.MemberQueryRepository;
import co.kr.mini_spring.auth.token.domain.RefreshToken;
import co.kr.mini_spring.auth.token.repository.RefreshTokenRepository;
import co.kr.mini_spring.member.dto.request.SignUpRequest;
import co.kr.mini_spring.member.dto.response.SignUpResponse;
import co.kr.mini_spring.member.exception.EmailAlreadyExistsException;
import co.kr.mini_spring.member.exception.PasswordMismatchException;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import co.kr.mini_spring.global.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FileService fileService;

    @Value("${file.default-profile-image}")
    private String defaultProfileImage;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new PasswordMismatchException("비밀번호가 일치하지 않습니다.");
        }

        String email = normalizeEmail(request.getEmail());

        memberRepository.findByEmail(email).ifPresent(m -> {
            throw new EmailAlreadyExistsException();
        });

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        String nickname = resolveNickname(request.getNickname());

        Member member = Member.builder()
                .email(email)
                .passwordHash(encodedPassword)
                .name(request.getName())
                .nickname(nickname)
                .profileImage(null)
                .role(MemberRole.USER)
                .status(MemberStatus.ACTIVE)
                .oauthProvider(MemberProvider.LOCAL)
                .oauthId(null)
                .build();
        Member savedMember = memberRepository.save(member);

        JwtTokenProvider.TokenWithExpiry accessToken = jwtTokenProvider.generateAccessToken(savedMember.getEmail(), savedMember.getRole());
        JwtTokenProvider.TokenWithExpiry refreshToken = jwtTokenProvider.generateRefreshToken(savedMember.getEmail());

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .token(refreshToken.getToken())
                        .memberId(savedMember.getId())
                        .expiresAt(refreshToken.getExpiresAt())
                        .revoked(false)
                        .build()
        );

        return SignUpResponse.builder()
                .id(savedMember.getId())
                .email(savedMember.getEmail())
                .name(savedMember.getName())
                .nickname(savedMember.getNickname())
                .profileImageUrl(savedMember.getProfileImageUrl(defaultProfileImage))
                .role(savedMember.getRole())
                .accessToken(accessToken.getToken())
                .accessTokenExpiresAt(accessToken.getExpiresAt())
                .refreshToken(refreshToken.getToken())
                .refreshTokenExpiresAt(refreshToken.getExpiresAt())
                .build();
    }

    /**
     * 사용자의 프로필 이미지를 업데이트합니다.
     */
    @Transactional
    public String updateProfileImage(Long memberId, MultipartFile file) {
        // 최적화된 Querydsl 조회 사용
        Member member = memberQueryRepository.findByIdWithProfileImage(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 1. 새 이미지 파일 저장
        ImageFile imageFile = fileService.uploadImage(file);

        // 2. 멤버 엔티티의 프로필 이미지 업데이트
        member.updateProfileImage(imageFile);

        log.info("[프로필 이미지 업데이트 성공] memberId={}, fileId={}", memberId, imageFile.getId());

        return imageFile.getFullUrl();
    }

    /**
     * 중복되지 않는 랜덤 닉네임을 생성합니다.
     * @return "형용사+명사#0000" 형식의 닉네임
     */
    private String resolveNickname(String requestedNickname) {
        if (requestedNickname != null && !requestedNickname.isBlank()
                && memberRepository.findByNickname(requestedNickname.trim()).isEmpty()) {
            return requestedNickname.trim();
        }
        return NicknameGenerator.generateUniqueNickname(n -> memberRepository.findByNickname(n).isPresent());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}