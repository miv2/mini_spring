package co.kr.mini_spring.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatParticipantResponse {
    private Long targetUserId;
    private String nickname;
    private String profileImageUrl;
}
