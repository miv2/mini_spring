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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

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

    @Value("${app.frontend-url}")
    private String frontendUrl;

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

        // 1. 쿠키에서 redirect_uri 추출
        String redirectUri = CookieUtil.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(cookie -> cookie.getValue())
                .orElse(null);

        // 2. 모바일/웹 분기 처리
        if (redirectUri != null && !redirectUri.isBlank()) {
            // [모바일 분기] 명시적인 redirect_uri가 있는 경우 (예: minispring://login-callback)
            // HttpOnly 쿠키 대신 URL 쿼리 파라미터로 토큰을 전달하여 모바일 네이티브 앱이 파싱할 수 있게 지원
            return UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("accessToken", tokenResponse.getAccessToken())
                    .queryParam("refreshToken", tokenResponse.getRefreshToken())
                    .build().toUriString();
        }

        // [웹 분기] 기본 웹 요청인 경우 (redirect_uri가 없는 경우)
        // 기존처럼 HttpOnly 보안 쿠키에 토큰을 담고, 프론트엔드 홈으로 리다이렉트
        CookieUtil.setAuthCookies(request, response, tokenResponse);

        return frontendUrl + "/home";
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
