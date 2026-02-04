package co.kr.mini_spring.post.dto.response;

import co.kr.mini_spring.member.domain.Member;
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
     * @param comment 댓글 엔티티
     * @param currentUser 현재 로그인한 사용자 (작성자 여부 확인용)
     */
    public CommentResponse(Comment comment, Member currentUser) {
        this.id = comment.getId();
        this.content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
        this.memberId = comment.getMember() != null ? comment.getMember().getId() : null;
        this.memberName = comment.getMember() != null ? comment.getMember().getNickname() : null;
        this.parentId = (comment.getParent() != null) ? comment.getParent().getId() : null;
        this.isDeleted = comment.isDeleted();
        this.createdAt = comment.getCreatedAt();
        this.updatedAt = comment.getUpdatedAt();
        this.isOwner = (currentUser != null && comment.getMember() != null) && Objects.equals(comment.getMember().getId(), currentUser.getId());
        
        // 대댓글이 있다면 생성일 기준 오름차순(오래된 순)으로 정렬하여 재귀적으로 변환
        this.children = comment.getChildren().stream()
                .sorted(Comparator.comparing(Comment::getCreatedAt))
                .map(child -> new CommentResponse(child, currentUser))
                .collect(Collectors.toList());
    }
}
