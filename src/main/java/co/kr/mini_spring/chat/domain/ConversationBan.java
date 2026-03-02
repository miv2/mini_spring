package co.kr.mini_spring.chat.domain;

import co.kr.mini_spring.member.domain.SocialMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_bans", indexes = {
        @Index(name = "idx_conversation_bans_user_id", columnList = "user_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_conversation_bans_conv_user", columnNames = {"conversation_id", "user_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConversationBan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)
    private Conversation conversation;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private SocialMember user;

    @Column(name = "banned_by", nullable = false)
    private Long bannedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by", insertable = false, updatable = false)
    private SocialMember bannedByMember;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

