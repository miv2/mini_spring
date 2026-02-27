package co.kr.mini_spring.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "댓글 수정 요청")
public class CommentUpdateRequest {

    @Schema(description = "수정할 댓글 내용", example = "수정된 댓글 내용입니다.")
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;
}
