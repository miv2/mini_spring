package co.kr.mini_spring.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "채팅방 초대 요청")
public class InviteUserRequest {

    @Schema(description = "초대할 사용자 ID", example = "3")
    @NotNull(message = "대상 사용자 ID는 필수입니다.")
    private Long targetUserId;
}
