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
        "memberId", "memberName", "isOwner", "hashtags", "createdAt", "updatedAt"
})
public class PostResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final int viewCount;
    private final int likeCount;
    private final Long memberId;
    private final String memberName;
    private final boolean isOwner;
    private final Set<String> hashtags;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime updatedAt;

    /**
     * 게시글 상세 응답 생성자
     * @param post 게시글 엔티티
     * @param currentUser 현재 로그인한 사용자 (작성자 여부 확인용)
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

        this.isOwner = (currentUser != null && author != null) && Objects.equals(author.getId(), currentUser.getId());

        this.hashtags = post.getPostHashtags().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toSet());
    }
}
