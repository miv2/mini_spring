package co.kr.mini_spring.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "채팅 참여자 응답")
public class ChatParticipantResponse {
    @Schema(description = "사용자 ID", example = "1")
    private Long targetUserId;
    
    @Schema(description = "사용자 닉네임", example = "miv")
    private String nickname;
    
    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;
}
