package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.post.domain.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@JsonPropertyOrder({
        "id", "title", "memberName", "likeCount", "viewCount", "createdAt"
})
public class PostListResponse {
    private final Long id;
    private final String title;
    private final String memberName;
    private final int likeCount;
    private final int viewCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    public PostListResponse(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.memberName = post.getMember() != null ? post.getMember().getNickname() : null;
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
    }
}
