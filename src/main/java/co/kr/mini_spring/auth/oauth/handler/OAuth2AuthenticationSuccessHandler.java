package co.kr.mini_spring.auth.oauth.handler;

import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.auth.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import co.kr.mini_spring.auth.token.domain.RefreshToken;
import co.kr.mini_spring.auth.token.repository.RefreshTokenRepository;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import co.kr.mini_spring.global.util.CookieUtil;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.repository.SocialMemberRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final SocialMemberRepository socialMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Override
    @Transactional // Refresh Token 저장 로직을 포함하므로 트랜잭션 처리
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            logger.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
        String registrationId = authToken.getAuthorizedClientRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = resolveEmail(registrationId, attributes);
        String oauthId = resolveOauthId(registrationId, authToken.getName(), attributes);
        MemberProvider provider = resolveProvider(registrationId);

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        MemberRole role = isAdmin ? MemberRole.ADMIN : MemberRole.USER;

        JwtTokenProvider.TokenWithExpiry accessTokenInfo = jwtTokenProvider.generateAccessToken(email, role);
        JwtTokenProvider.TokenWithExpiry refreshTokenInfo = jwtTokenProvider.generateRefreshToken(email);

        SocialMember member = socialMemberRepository.findByProviderAndOauthId(provider, oauthId)
                .orElseGet(() -> socialMemberRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalStateException("사용자를 찾을 수 없습니다.")));

        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshTokenInfo.getToken())
                .authorId(member.getId())
                .expiresAt(refreshTokenInfo.getExpiresAt())
                .build());

        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken(accessTokenInfo.getToken())
                .refreshToken(refreshTokenInfo.getToken())
                .accessTokenExpiresAt(accessTokenInfo.getExpiresAt())
                .refreshTokenExpiresAt(refreshTokenInfo.getExpiresAt())
                .build();
        
        CookieUtil.setAuthCookies(request, response, tokenResponse);

        return "http://localhost:5173/home";
    }

    private String resolveEmail(String registrationId, Map<String, Object> attributes) {
        Object email = attributes.get("email");
        if (email != null) return email.toString();
        
        if ("kakao".equalsIgnoreCase(registrationId)) {
            Object account = attributes.get("kakao_account");
            if (account instanceof Map<?, ?> map && map.get("email") != null) {
                return map.get("email").toString();
            }
        }
        throw new IllegalStateException("이메일을 확인할 수 없습니다.");
    }

    private String resolveOauthId(String registrationId, String defaultName, Map<String, Object> attributes) {
        if ("kakao".equalsIgnoreCase(registrationId)) {
            Object id = attributes.get("id");
            if (id != null) return id.toString();
        }
        if ("google".equalsIgnoreCase(registrationId)) {
            Object sub = attributes.get("sub");
            if (sub != null) return sub.toString();
        }
        return defaultName;
    }

    private MemberProvider resolveProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> MemberProvider.GOOGLE;
            case "kakao" -> MemberProvider.KAKAO;
            default -> throw new IllegalStateException("지원하지 않는 OAuth 제공자: " + registrationId);
        };
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
