package co.kr.mini_spring.auth.oauth;

import co.kr.mini_spring.global.util.CookieUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * OAuth2.0 인증 과정에서 인증 요청 정보를 세션 대신 쿠키에 저장하고 관리하는 클래스입니다.
 * Spring Security의 기본 구현체인 HttpSessionOAuth2AuthorizationRequestRepository를 대체하여
 * 서버가 상태를 저장하지 않는 STATELESS 환경에서도 OAuth2.0 로그인을 가능하게 합니다.
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    /**
     * OAuth2 인증 요청 정보를 저장할 쿠키의 이름입니다.
     */
    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";

    /**
     * 소셜 로그인 성공 후 리다이렉트할 최종 목적지 URI를 저장할 쿠키의 이름입니다.
     * (예: 프론트엔드의 특정 페이지)
     */
    public static final String REDIRECT_URI_PARAM_COOKIE_NAME = "redirect_uri";

    /**
     * 쿠키의 유효 시간(초)입니다. 180초(3분)로 설정되어 있으며,
     * 이 시간 안에 소셜 로그인을 완료하고 콜백으로 돌아와야 합니다.
     */
    private static final int COOKIE_EXPIRE_SECONDS = 180;

    /**
     * HttpServletRequest에서 쿠키를 역직렬화하여 OAuth2AuthorizationRequest 객체를 로드합니다.
     * Spring Security가 콜백 요청을 처리할 때, 이전에 저장된 인증 요청 정보를 찾기 위해 이 메서드를 호출합니다.
     *
     * @param request 현재 HTTP 요청
     * @return 쿠키에서 역직렬화된 OAuth2AuthorizationRequest 객체. 쿠키가 없으면 null을 반환합니다.
     */
    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        // 1. 요청에서 OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME을 가진 쿠키를 찾습니다.
        return CookieUtil.getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME)
                // 2. 쿠키가 존재하면, 쿠키의 값을 역직렬화하여 OAuth2AuthorizationRequest 객체로 변환합니다.
                .map(cookie -> CookieUtil.deserialize(cookie, OAuth2AuthorizationRequest.class))
                // 3. 쿠키가 없으면 null을 반환합니다.
                .orElse(null);
    }

    /**
     * OAuth2AuthorizationRequest를 직렬화하여 쿠키에 저장하고, 응답에 추가합니다.
     * Spring Security가 소셜 로그인 페이지로 리다이렉트하기 직전에 이 메서드를 호출하여
     * "인증 요청 정보"를 저장합니다.
     *
     * @param authorizationRequest 저장할 인증 요청 정보
     * @param request              현재 HTTP 요청
     * @param response             현재 HTTP 응답
     */
    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        // 1. authorizationRequest가 null이면, 기존의 관련 쿠키들을 모두 삭제합니다.
        if (authorizationRequest == null) {
            CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
            CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
            return;
        }

        // 2. authorizationRequest 객체를 직렬화하여 쿠키에 저장하고, 응답에 추가합니다.
        CookieUtil.addCookie(response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, CookieUtil.serialize(authorizationRequest), COOKIE_EXPIRE_SECONDS);

        // 3. 소셜 로그인 후 리다이렉트할 URI가 파라미터로 넘어왔는지 확인합니다.
        String redirectUriAfterLogin = request.getParameter(REDIRECT_URI_PARAM_COOKIE_NAME);
        // 4. 리다이렉트 URI가 존재하면, 이 정보도 쿠키에 저장합니다.
        if (StringUtils.hasText(redirectUriAfterLogin)) {
            CookieUtil.addCookie(response, REDIRECT_URI_PARAM_COOKIE_NAME, redirectUriAfterLogin, COOKIE_EXPIRE_SECONDS);
        }
    }

    /**
     * 인증이 완료된 후, 저장된 인증 요청 정보를 제거합니다.
     * Spring Security는 이 메서드를 호출하여 인증 과정에서 사용된 정보를 정리하려고 시도합니다.
     * 이 구현에서는 loadAuthorizationRequest를 호출하여 현재 요청의 쿠키 정보를 반환하고,
     * 실제 쿠키 삭제는 Success/Failure Handler에서 명시적으로 수행합니다.
     *
     * @param request  현재 HTTP 요청
     * @param response 현재 HTTP 응답
     * @return 제거되기 전의 OAuth2AuthorizationRequest 객체
     */
    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        return this.loadAuthorizationRequest(request);
    }

    /**
     * 인증 과정에서 사용된 모든 임시 쿠키를 삭제합니다.
     * 이 메서드는 OAuth2 인증 성공 또는 실패 핸들러에서 명시적으로 호출되어야 합니다.
     *
     * @param request  현재 HTTP 요청
     * @param response 현재 HTTP 응답
     */
    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request, response, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        CookieUtil.deleteCookie(request, response, REDIRECT_URI_PARAM_COOKIE_NAME);
    }
}
