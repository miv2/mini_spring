package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.post.domain.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@JsonPropertyOrder({
        "id", "title", "memberId", "memberName", "likeCount", "viewCount", "commentCount", "hashtags", "createdAt"
})
@Schema(description = "게시글 목록 요약 응답")
public class PostSummaryResponse {

    @Schema(description = "게시글 ID")
    private final Long id;

    @Schema(description = "제목")
    private final String title;

    @Schema(description = "작성자 ID")
    private final Long memberId;

    @Schema(description = "작성자 닉네임")
    private final String memberName;

    @Schema(description = "좋아요 수")
    private final int likeCount;

    @Schema(description = "조회수")
    private final int viewCount;

    @Schema(description = "댓글 수")
    private final int commentCount;

    @Schema(description = "해시태그 목록")
    private final Set<String> hashtags;

    @Schema(description = "생성 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    public PostSummaryResponse(Post post, SocialMember author) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.memberId = post.getAuthorId();
        this.memberName = author != null ? author.getNickname() : null;
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
        this.createdAt = post.getCreatedAt();
        this.hashtags = post.getPostHashtags().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toSet());
    }
}
