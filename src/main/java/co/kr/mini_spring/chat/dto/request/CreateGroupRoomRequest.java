package co.kr.mini_spring.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "그룹 채팅방 생성 요청")
public class CreateGroupRoomRequest {

    @Schema(description = "그룹 채팅방 제목", example = "백엔드 스터디")
    @NotBlank(message = "방 제목은 필수입니다.")
    @Size(max = 100, message = "방 제목은 100자를 초과할 수 없습니다.")
    private String title;
}
