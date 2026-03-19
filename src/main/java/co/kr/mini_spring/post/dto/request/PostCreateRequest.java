package co.kr.mini_spring.post.dto.request;

import co.kr.mini_spring.post.validation.ValidHashtags;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "게시글 생성 요청")
public class PostCreateRequest {

    @Schema(description = "제목", example = "새로운 게시글 제목")
    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @Schema(description = "내용", example = "게시글 본문 내용입니다. 마크다운 등을 지원할 수 있습니다.")
    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @Schema(description = "해시태그 목록 (영문, 숫자, 완성형 한글만 허용. `ㅋㅋ`, `ㅎㅎ` 같은 자모-only 태그는 불가)", example = "[\"spring\", \"react\"]")
    @ValidHashtags
    private List<String> hashtags;
}
