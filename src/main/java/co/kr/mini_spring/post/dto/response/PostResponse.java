package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.post.domain.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@JsonPropertyOrder({
        "id", "title", "content", "viewCount", "likeCount",
        "memberId", "memberName", "isOwner", "hashtags", "comments", "createdAt", "updatedAt"
})
public class PostResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final int viewCount;
    private final int likeCount;
    private final Long memberId;
    private final String memberName;
    private final boolean isOwner; // 작성자 여부 필드 추가
    private final Set<String> hashtags;
    private final List<CommentResponse> comments;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime updatedAt;

    // 비로그인 사용자를 위한 생성자
    public PostResponse(Post post) {
        this(post, null, null);
    }

    // 로그인한 사용자를 위한 생성자
    public PostResponse(Post post, Member currentUser) {
        this(post, currentUser, null);
    }

    /**
     * 로그인 사용자 + 조회수 보정이 필요한 경우 사용.
     * - 조회수는 DB에서 원자적으로 증가시키기 때문에, 응답에는 최신 값을 별도로 주입할 수 있다.
     */
    public PostResponse(Post post, Member currentUser, Integer viewCountOverride) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.viewCount = viewCountOverride != null ? viewCountOverride : post.getViewCount();
        this.likeCount = post.getLikeCount();
        Member author = post.getMember();
        this.memberId = author != null ? author.getId() : null;
        this.memberName = author != null ? author.getNickname() : null;
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();

        // isOwner 설정
        this.isOwner = (currentUser != null && author != null) && Objects.equals(author.getId(), currentUser.getId());

        this.hashtags = post.getPostHashtags().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toSet());

        // 댓글 목록을 CommentResponse DTO로 변환 (currentUser 정보 전달)
        this.comments = post.getComments().stream()
                .filter(comment -> comment.getParent() == null) // 최상위 댓글만 필터링
                .map(comment -> new CommentResponse(comment, currentUser))
                .collect(Collectors.toList());
    }
}
