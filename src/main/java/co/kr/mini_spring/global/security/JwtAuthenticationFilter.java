package co.kr.mini_spring.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 * - 요청 헤더에서 JWT 토큰을 추출하고 검증
 * - 유효한 토큰인 경우 SecurityContext에 인증 정보 설정
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. 요청에서 JWT 토큰 추출
            String jwt = extractTokenFromRequest(request);

            // 2. 토큰이 존재하면 유효성을 검증하고, 실패 사유는 request attribute로 남깁니다.
            if (StringUtils.hasText(jwt)) {
                JwtTokenProvider.JwtValidationResult validation = jwtTokenProvider.validateTokenWithResult(jwt);
                if (!validation.isValid()) {
                    request.setAttribute(JwtTokenProvider.JWT_ERROR_CODE_ATTRIBUTE, validation.getErrorCode());
                    filterChain.doFilter(request, response);
                    return;
                }
                
                // 3. 토큰에서 이메일 추출
                String email = jwtTokenProvider.getEmail(jwt);

                // 4. UserDetails 조회
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 5. Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // 6. 요청 상세 정보 설정
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // 7. SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("[JWT 인증 성공] email={}, uri={}", email, request.getRequestURI());
            }

        } catch (Exception e) {
            // 토큰 검증 실패 시 로그만 남기고 다음 필터로 진행
            // 실제 인증 실패 처리는 AuthenticationEntryPoint에서 담당
            log.error("[JWT 인증 실패] uri={}, error={}", request.getRequestURI(), e.getMessage());
        }

        // 다음 필터로 진행
        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에서 JWT 토큰 추출
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        // Bearer 타입 토큰인 경우 접두사 제거
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}
