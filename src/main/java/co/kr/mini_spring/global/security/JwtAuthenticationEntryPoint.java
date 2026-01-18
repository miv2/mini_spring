package co.kr.mini_spring.global.security;

import co.kr.mini_spring.global.common.response.ApiResponse;
import co.kr.mini_spring.global.common.response.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 인증 실패 처리 핸들러
 * - 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        log.warn("[인증 실패] uri={}, error={}", request.getRequestURI(), authException.getMessage());

        ResponseCode responseCode = (ResponseCode) request.getAttribute(JwtTokenProvider.JWT_ERROR_CODE_ATTRIBUTE);
        if (responseCode == null) {
            responseCode = ResponseCode.UNAUTHENTICATED;
        }

        // 응답 설정
        response.setStatus(responseCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 에러 응답 생성
        ApiResponse<Void> errorResponse = ApiResponse.fail(responseCode);

        // JSON 응답 작성
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
