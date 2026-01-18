package co.kr.mini_spring.member.service;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.MemberStatus;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.auth.token.domain.RefreshToken;
import co.kr.mini_spring.auth.token.repository.RefreshTokenRepository;
import co.kr.mini_spring.member.dto.request.SignUpRequest;
import co.kr.mini_spring.member.dto.response.SignUpResponse;
import co.kr.mini_spring.global.common.exception.EmailAlreadyExistsException;
import co.kr.mini_spring.global.common.exception.PasswordMismatchException;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import co.kr.mini_spring.global.util.NicknameGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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
                .profileImageUrl(null)
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
                .role(savedMember.getRole())
                .accessToken(accessToken.getToken())
                .accessTokenExpiresAt(accessToken.getExpiresAt())
                .refreshToken(refreshToken.getToken())
                .refreshTokenExpiresAt(refreshToken.getExpiresAt())
                .build();
    }

    /**
     * 중복되지 않는 랜덤 닉네임을 생성합니다.
     * @return "형용사+명사+숫자" 형식의 닉네임
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
