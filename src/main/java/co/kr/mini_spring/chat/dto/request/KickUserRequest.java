package co.kr.mini_spring.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "채팅방 강퇴 요청")
public class KickUserRequest {

    @Schema(description = "강퇴할 사용자 ID", example = "3")
    @NotNull(message = "대상 사용자 ID는 필수입니다.")
    private Long targetUserId;
}
