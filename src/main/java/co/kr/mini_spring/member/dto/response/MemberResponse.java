package co.kr.mini_spring.member.dto.response;

import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberRole;
import co.kr.mini_spring.member.domain.SocialMember;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "회원 정보 응답")
public class MemberResponse {

    @Schema(description = "회원 ID")
    private final Long id;

    @Schema(description = "이메일")
    private final String email;

    @Schema(description = "이름")
    private final String name;

    @Schema(description = "닉네임")
    private final String nickname;

    @Schema(description = "프로필 이미지 URL")
    private final String profileImageUrl;

    @Schema(description = "권한")
    private final MemberRole role;

    @Schema(description = "가입일")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDateTime createdAt;

    @Schema(description = "소셜 로그인 제공자")
    private final MemberProvider provider;

    public MemberResponse(SocialMember member, String baseUrl, String defaultProfileImage) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.nickname = member.getNickname();
        this.profileImageUrl = member.getProfileImageUrl(baseUrl, defaultProfileImage);
        this.role = member.getRole();
        this.createdAt = member.getCreatedAt();
        this.provider = member.getProvider();
    }
}
