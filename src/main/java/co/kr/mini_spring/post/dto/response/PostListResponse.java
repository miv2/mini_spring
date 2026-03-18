package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.post.domain.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonPropertyOrder({
        "id", "title", "memberName", "likeCount", "viewCount", "createdAt"
})
@Schema(description = "게시글 목록 아이템 응답")
public class PostListResponse {
    @Schema(description = "게시글 ID", example = "1")
    private final Long id;
    
    @Schema(description = "제목", example = "새로운 게시글 제목")
    private final String title;
    
    @Schema(description = "작성자 닉네임", example = "miv")
    private final String memberName;
    
    @Schema(description = "좋아요 수", example = "10")
    private final int likeCount;
    
    @Schema(description = "조회수", example = "150")
    private final int viewCount;

    @Schema(description = "생성 일시", example = "2026-03-02 10:15")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    public PostListResponse(Post post, co.kr.mini_spring.member.domain.SocialMember author) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.memberName = author != null ? author.getNickname() : null;
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
    }
}
