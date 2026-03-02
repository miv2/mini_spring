package co.kr.mini_spring.chat.domain;

import co.kr.mini_spring.member.domain.SocialMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_participants", indexes = {
        @Index(name = "idx_participants_user_id", columnList = "user_id, deleted_at"),
        @Index(name = "idx_participants_conv_id", columnList = "conversation_id, deleted_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class ConversationParticipant {

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

    @Column(name = "last_read_message_id")
    private Long lastReadMessageId;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void markRead(Long messageId) {
        if (messageId == null) {
            return;
        }
        if (this.lastReadMessageId == null || this.lastReadMessageId < messageId) {
            this.lastReadMessageId = messageId;
        }
    }

    public void leave() {
        this.deletedAt = LocalDateTime.now();
    }

    public void rejoin() {
        this.deletedAt = null;
    }
}

