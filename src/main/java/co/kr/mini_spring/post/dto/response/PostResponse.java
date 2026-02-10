package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.post.domain.Post;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@JsonPropertyOrder({
        "id", "title", "content", "viewCount", "likeCount",
        "memberId", "memberName", "isOwner", "hashtags", "createdAt", "updatedAt"
})
@Schema(description = "게시글 상세 정보 응답")
public class PostResponse {

    @Schema(description = "게시글 ID")
    private final Long id;

    @Schema(description = "제목")
    private final String title;

    @Schema(description = "본문 내용")
    private final String content;

    @Schema(description = "조회수")
    private final int viewCount;

    @Schema(description = "좋아요 수")
    private final int likeCount;

    @Schema(description = "작성자 ID")
    private final Long memberId;

    @Schema(description = "작성자 닉네임")
    private final String memberName;

    @Schema(description = "본인 글 여부 (수정/삭제 권한 확인용)")
    private final boolean isOwner;

    @Schema(description = "해시태그 목록")
    private final Set<String> hashtags;

    @Schema(description = "생성 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime updatedAt;

    /**
     * 게시글 상세 응답 생성자
     * 
     * @param post        게시글 엔티티
     * @param currentUser 현재 로그인한 사용자 (작성자 여부 확인용)
     */
    public PostResponse(Post post, SocialMember author, SocialMember currentUser, Integer viewCountOverride) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.viewCount = viewCountOverride != null ? viewCountOverride : post.getViewCount();
        this.likeCount = post.getLikeCount();
        this.memberId = post.getAuthorId();
        this.memberName = author != null ? author.getNickname() : null;
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();

        this.isOwner = (currentUser != null && author != null) && Objects.equals(author.getId(), currentUser.getId());

        this.hashtags = post.getPostHashtags().stream()
                .map(postHashtag -> postHashtag.getHashtag().getName())
                .collect(Collectors.toSet());
    }
}
