package co.kr.mini_spring.post.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "댓글 생성 요청")
public class CommentCreateRequest {

    @Schema(description = "게시글 ID", example = "1")
    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long postId;

    @Schema(description = "댓글 내용", example = "정말 좋은 글이네요!")
    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;

    @Schema(description = "부모 댓글 ID (대댓글인 경우)", example = "10")
    private Long parentId;
}
