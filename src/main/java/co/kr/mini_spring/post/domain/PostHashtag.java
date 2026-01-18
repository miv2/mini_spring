package co.kr.mini_spring.post.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_hashtag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostHashtag {

    @EmbeddedId
    private PostHashtagId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId") // 복합 키 클래스의 postId 필드에 매핑
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("hashtagId") // 복합 키 클래스의 hashtagId 필드에 매핑
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 복합 키 클래스
    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PostHashtagId implements Serializable {
        @Column(name = "post_id")
        private Long postId;

        @Column(name = "hashtag_id")
        private Long hashtagId;
    }
}
