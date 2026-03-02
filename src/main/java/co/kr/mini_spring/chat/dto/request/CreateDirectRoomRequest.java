package co.kr.mini_spring.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "1:1 채팅방 생성 요청")
public class CreateDirectRoomRequest {

    @Schema(description = "채팅 상대 사용자 ID", example = "2")
    @NotNull(message = "대상 사용자 ID는 필수입니다.")
    private Long targetUserId;
}
