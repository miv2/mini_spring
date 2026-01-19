package co.kr.mini_spring.member.dto.response;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

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

    public MemberResponse(Member member, String defaultProfileImage) {
        this.id = member.getId();
        this.email = member.getEmail();
        this.name = member.getName();
        this.nickname = member.getNickname();
        this.profileImageUrl = member.getProfileImageUrl(defaultProfileImage);
        this.role = member.getRole();
    }
}
