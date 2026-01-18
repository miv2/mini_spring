package co.kr.mini_spring.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SocialLoginLinkResponse {
    private final String provider;
    private final String displayName;
    private final String authorizationUrl;
}
