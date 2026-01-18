package co.kr.mini_spring.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateRequest {

    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long postId;

    @NotBlank(message = "댓글 내용은 필수입니다.")
    private String content;

    private Long parentId; // 대댓글인 경우 부모 댓글 ID
}
