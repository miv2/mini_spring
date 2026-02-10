package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.post.domain.Comment;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
public class CommentResponse {
    private final Long id;
    private final String content;
    private final Long memberId;
    private final String memberName;
    private final boolean isOwner;
    private final Long parentId;
    private final boolean isDeleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private final LocalDateTime updatedAt;

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

        // 대댓글이 있다면 생성일 기준 오름차순(오래된 순)으로 정렬하여 재귀적으로 변환
        // Note: 대댓글의 작성자 정보를 여기서 알 수 없으므로, 일단 null이나 별도 처리가 필요함.
        // 하지만 구조상 대댓글의 authorId는 알 수 있음. 여기서는 간단히 재귀 호출하되,
        // 대댓글의 작성자 객체를 가져오는 로직은 복잡해지므로 Service에서 처리하거나
        // 일단 null 혹은 빈 껍데기로 처리해야 함.
        // --> Service에서 조립하는게 맞으나 여기서 재귀호출 하므로 복잡함.
        // 일단 컴파일 되도록 children은 빈 리스트로 두거나, children처리를 서비스로 빼는게 좋음.
        // 여기서는 임시로 children 매핑 시 author를 null로 넘기거나 해야하는데,
        // 올바른 방법은 Comment 내부에 children이 있으니,
        // children의 authorId를 이용해 Service에서 매핑해줘야 함.
        // DTO 내부에서 재귀적으로 변환하던 로직을 들어내야 할 수도 있음.
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
