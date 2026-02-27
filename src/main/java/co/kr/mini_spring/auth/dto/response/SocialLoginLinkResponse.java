package co.kr.mini_spring.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "소셜 로그인 링크 응답")
public class SocialLoginLinkResponse {
    @Schema(description = "제공자 (google, kakao 등)", example = "google")
    private final String provider;

    @Schema(description = "표시 이름", example = "Google")
    private final String displayName;

    @Schema(description = "인증 페이지 진입 URL", example = "https://minispring.duckdns.org/oauth2/authorization/google")
    private final String authorizationUrl;
}
