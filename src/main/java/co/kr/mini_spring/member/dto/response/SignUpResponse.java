package co.kr.mini_spring.member.dto.response;

import co.kr.mini_spring.member.domain.MemberRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SignUpResponse {
    private final Long id;
    private final String email;
    private final String name;
    private final String nickname;
    private final MemberRole role;
    private final String accessToken;
    private final LocalDateTime accessTokenExpiresAt;
    private final String refreshToken;
    private final LocalDateTime refreshTokenExpiresAt;
}
