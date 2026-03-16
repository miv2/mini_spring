package co.kr.mini_spring.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatBanResponse {
    private Long targetUserId;
    private String nickname;
    private String profileImageUrl;
}
