package co.kr.mini_spring.chat.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "채팅방 강퇴(밴) 응답")
public class ChatBanResponse {
    @Schema(description = "대상 사용자 ID", example = "2")
    private Long targetUserId;
    
    @Schema(description = "대상 사용자 닉네임", example = "gildong")
    private String nickname;
    
    @Schema(description = "대상 사용자 프로필 이미지 URL")
    private String profileImageUrl;
}
