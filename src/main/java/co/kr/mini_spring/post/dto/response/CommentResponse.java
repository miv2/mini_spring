package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.post.domain.Comment;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@JsonPropertyOrder({
        "id", "content", "memberId", "memberName", "isOwner", "parentId",
        "isDeleted", "createdAt", "updatedAt", "children"
})
@Schema(description = "댓글 정보 응답")
public class CommentResponse {

    @Schema(description = "댓글 ID")
    private final Long id;

    @Schema(description = "본문 내용 (삭제된 경우 '삭제된 댓글입니다.'로 표시)")
    private final String content;

    @Schema(description = "작성자 ID")
    private final Long memberId;

    @Schema(description = "작성자 닉네임")
    private final String memberName;

    @Schema(description = "본인 댓글 여부 (수정/삭제 권한 확인용)")
    private final boolean isOwner;

    @Schema(description = "부모 댓글 ID (null인 경우 최상위 댓글)")
    private final Long parentId;

    @Schema(description = "삭제 여부")
    private final boolean isDeleted;

    @Schema(description = "생성 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime updatedAt;

    @Schema(description = "대댓글(자식 댓글) 목록")
    private final List<CommentResponse> children; // 대댓글 목록

    /**
     * 댓글 응답 생성자
     * 
     * @param comment     댓글 엔티티
     * @param currentUser 현재 로그인한 사용자 (작성자 여부 확인용)
     */
    public CommentResponse(Comment comment, SocialMember author, SocialMember currentUser) {
        this.id = comment.getId();
        this.content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
        this.memberId = comment.getAuthorId();
        this.memberName = author != null ? author.getNickname() : null;
        this.parentId = (comment.getParent() != null) ? comment.getParent().getId() : null;
        this.isDeleted = comment.isDeleted();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.isOwner = (currentUser != null && author != null) && Objects.equals(author.getId(), currentUser.getId());

        this.children = List.of();
    }

    // Children을 외부에서 주입받기 위한 생성자 혹은 Setter 필요.
    // 혹은 Service에서 트리를 조립하도록 변경 권장.
    public CommentResponse(Comment comment, SocialMember author, SocialMember currentUser,
            List<CommentResponse> children) {
        this.id = comment.getId();
        this.content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
        this.memberId = comment.getAuthorId();
        this.memberName = author != null ? author.getNickname() : null;
        this.parentId = (comment.getParent() != null) ? comment.getParent().getId() : null;
        this.isDeleted = comment.isDeleted();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.isOwner = (currentUser != null && author != null) && Objects.equals(author.getId(), currentUser.getId());
        this.children = children;
    }
}
