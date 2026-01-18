package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.post.domain.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@JsonPropertyOrder({
        "id", "title", "memberId"
        , "memberName", "likeCount", "viewCount"
        , "commentCount", "hashtags", "createdAt"
})
public class PostSummaryResponse {
    private final Long id;
    private final String title;
    private final Long memberId;
    private final String memberName;
    private final int likeCount;
    private final int viewCount;
    private final int commentCount;
    private final Set<String> hashtags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    public PostSummaryResponse(Post post) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.memberId = post.getMember() != null ? post.getMember().getId() : null;
        this.memberName = post.getMember() != null ? post.getMember().getNickname() : null;
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
        this.createdAt = post.getCreatedAt();
        this.hashtags = post.getPostHashtags().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toSet());
    }
}
