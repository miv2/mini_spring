package co.kr.mini_spring.chat.domain;

import co.kr.mini_spring.member.domain.SocialMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_blocks", indexes = {
        @Index(name = "idx_blocks_blocker_id", columnList = "blocker_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_blocks_blocker_blocked", columnNames = {"blocker_id", "blocked_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", insertable = false, updatable = false)
    private SocialMember blocker;

    @Column(name = "blocked_id", nullable = false)
    private Long blockedId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_id", insertable = false, updatable = false)
    private SocialMember blocked;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

