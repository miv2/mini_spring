package co.kr.mini_spring.post.domain;

import co.kr.mini_spring.member.domain.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_view", indexes = {
        @Index(name = "idx_last_viewed_at", columnList = "last_viewed_at"),
        @Index(name = "idx_post_id", columnList = "post_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostView {

    @EmbeddedId
    private PostViewId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("memberId")
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private Post post;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @Column(name = "view_count")
    @Builder.Default
    private int viewCount = 1;

    public boolean isViewCountable(LocalDateTime now, Duration interval) {
        return !lastViewedAt.isAfter(now.minus(interval));
    }

    public void updateLastViewedAt(LocalDateTime now) {
        this.lastViewedAt = now;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PostViewId implements Serializable {
        @Column(name = "member_id")
        private Long memberId;

        @Column(name = "post_id")
        private Long postId;
    }
}
