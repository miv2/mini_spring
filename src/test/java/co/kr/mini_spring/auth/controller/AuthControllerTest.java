package co.kr.mini_spring.auth.controller;

import co.kr.mini_spring.auth.dto.response.TokenResponse;
import co.kr.mini_spring.auth.service.AuthService;
import co.kr.mini_spring.global.common.exception.GlobalExceptionHandler;
import co.kr.mini_spring.global.util.CookieUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void refresh_shouldUseCookieToken_whenBodyIsEmptyJson() throws Exception {
        TokenResponse tokenResponse = TokenResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .accessTokenExpiresAt(LocalDateTime.now().plusMinutes(30))
                .refreshTokenExpiresAt(LocalDateTime.now().plusDays(7))
                .build();

        when(authService.refreshToken(eq("cookie-refresh-token"))).thenReturn(tokenResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .cookie(new Cookie(CookieUtil.REFRESH_TOKEN_NAME, "cookie-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("S001"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(cookie().exists(CookieUtil.ACCESS_TOKEN_NAME))
                .andExpect(cookie().exists(CookieUtil.REFRESH_TOKEN_NAME));

        verify(authService).refreshToken("cookie-refresh-token");
    }

    @Test
    void refresh_shouldReturnUnauthorized_whenRefreshCookieMissing() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("A004"));

        verifyNoInteractions(authService);
    }
}
