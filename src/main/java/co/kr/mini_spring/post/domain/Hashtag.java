package co.kr.mini_spring.post.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "hashtag", indexes = {
        @Index(name = "idx_name", columnList = "name")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @Column(name = "usage_count")
    @Builder.Default
    private int usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void increaseUsage() {
        this.usageCount++;
        this.lastUsedAt = LocalDateTime.now();
    }

    public void decreaseUsage() {
        if (this.usageCount > 0) {
            this.usageCount--;
        }
        this.lastUsedAt = LocalDateTime.now();
    }
}
