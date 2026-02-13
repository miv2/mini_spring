package co.kr.mini_spring.auth.controller;

import co.kr.mini_spring.auth.service.AuthService;
import co.kr.mini_spring.global.security.JwtTokenProvider;
import co.kr.mini_spring.global.security.MemberAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthControllerLogoutTest {

    @Test
    void logout_blacklists_access_token_and_calls_service() {
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthController controller = new AuthController(authService, jwtTokenProvider, redisTemplate);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        MemberAdapter memberAdapter = mock(MemberAdapter.class);

        when(memberAdapter.getUsername()).thenReturn("user@example.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(jwtTokenProvider.getExpiresAt("access-token"))
                .thenReturn(LocalDateTime.now().plusSeconds(120));

        controller.logout(memberAdapter, request, response);

        verify(authService).logout("user@example.com");
        verify(valueOps).set(startsWith("bl:access:"), eq("1"), any(Duration.class));
    }

    @Test
    void logout_sets_blacklist_with_ttl() {
        AuthService authService = mock(AuthService.class);
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        AuthController controller = new AuthController(authService, jwtTokenProvider, redisTemplate);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        MemberAdapter memberAdapter = mock(MemberAdapter.class);

        when(memberAdapter.getUsername()).thenReturn("user@example.com");
        when(request.getHeader("Authorization")).thenReturn("Bearer access-token");
        when(jwtTokenProvider.getExpiresAt("access-token"))
                .thenReturn(LocalDateTime.now().plusSeconds(120));

        controller.logout(memberAdapter, request, response);

        verify(valueOps).set(startsWith("bl:access:"), eq("1"), argThat(ttl -> ttl != null && ttl.getSeconds() > 0));
    }
}
