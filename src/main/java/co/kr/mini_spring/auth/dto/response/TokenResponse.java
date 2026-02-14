package co.kr.mini_spring.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "토큰 응답 정보")
public class TokenResponse {
    @Schema(description = "Access Token")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;

    @Schema(description = "Access Token 만료 일시")
    private LocalDateTime accessTokenExpiresAt;

    @Schema(description = "Refresh Token 만료 일시")
    private LocalDateTime refreshTokenExpiresAt;
}
