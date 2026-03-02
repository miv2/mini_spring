package co.kr.mini_spring.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CreateGroupRoomRequest {

    @NotBlank(message = "방 제목은 필수입니다.")
    @Size(max = 100, message = "방 제목은 100자를 초과할 수 없습니다.")
    private String title;
}

