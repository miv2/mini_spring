package co.kr.mini_spring.post.domain;

import co.kr.mini_spring.member.domain.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comment", indexes = {
        @Index(name = "idx_post_created", columnList = "post_id, created_at DESC"),
        @Index(name = "idx_member_created", columnList = "member_id, created_at DESC"),
        @Index(name = "idx_parent_depth", columnList = "parent_comment_id, depth")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true) // 자식 댓글도 함께 관리
    @Builder.Default
    private List<Comment> children = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private int depth = 0;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean deleted = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    //== 비즈니스 로직 ==//
    public void updateContent(String content) {
        this.content = content;
    }

    public void setParent(Comment parent) {
        this.parent = parent;
        this.depth = parent == null ? 0 : 1;
    }

    public void markAsDeleted() {
        this.deleted = true;
        this.content = "삭제된 댓글입니다."; // 삭제된 댓글임을 표시
    }
}
