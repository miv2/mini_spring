package co.kr.mini_spring.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class InviteUserRequest {

    @NotNull(message = "대상 사용자 ID는 필수입니다.")
    private Long targetUserId;
}

