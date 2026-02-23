package co.kr.mini_spring.global.util;

import co.kr.mini_spring.auth.dto.response.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.util.SerializationUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

public class CookieUtil {

    public static final String ACCESS_TOKEN_NAME = "accessToken";
    public static final String REFRESH_TOKEN_NAME = "refreshToken";

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(name)) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 요청 헤더 또는 쿠키에서 Access Token을 추출합니다.
     */
    public static String resolveAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return getCookie(request, ACCESS_TOKEN_NAME)
                .map(Cookie::getValue)
                .orElse(null);
    }

    /**
     * 응답 헤더에 인증 관련 보안 쿠키들을 설정합니다.
     */
    public static void setAuthCookies(HttpServletRequest request, HttpServletResponse response, TokenResponse tokenResponse) {
        boolean secure = isSecureRequest(request);
        
        long accessMaxAge = Duration.between(LocalDateTime.now(), tokenResponse.getAccessTokenExpiresAt()).getSeconds();
        long refreshMaxAge = Duration.between(LocalDateTime.now(), tokenResponse.getRefreshTokenExpiresAt()).getSeconds();

        addCookie(response, ACCESS_TOKEN_NAME, tokenResponse.getAccessToken(), accessMaxAge, secure);
        addCookie(response, REFRESH_TOKEN_NAME, tokenResponse.getRefreshToken(), refreshMaxAge, secure);
    }

    /**
     * 인증 관련 쿠키들을 즉시 만료시켜 삭제합니다.
     */
    public static void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = isSecureRequest(request);
        addCookie(response, ACCESS_TOKEN_NAME, "", 0, secure);
        addCookie(response, REFRESH_TOKEN_NAME, "", 0, secure);
    }

    /**
     * 범용 쿠키 추가 메서드
     */
    public static void addCookie(HttpServletResponse response, String name, String value, long maxAge, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(secure ? "None" : "Lax")
                .maxAge(Math.max(0, maxAge))
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        boolean secure = isSecureRequest(request);
        addCookie(response, name, "", 0, secure);
    }

    private static boolean isSecureRequest(HttpServletRequest request) {
        return request.isSecure() || "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
    }

    public static String serialize(Object object) {
        return Base64.getUrlEncoder()
                .encodeToString(SerializationUtils.serialize(object));
    }

    public static <T> T deserialize(Cookie cookie, Class<T> cls) {
        return cls.cast(SerializationUtils.deserialize(
                Base64.getUrlDecoder().decode(cookie.getValue())));
    }
}
