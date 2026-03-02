package co.kr.mini_spring.chat.domain;

import co.kr.mini_spring.member.domain.SocialMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conversations_last_message_at", columnList = "last_message_at DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ConversationType type;

    @Column(name = "unique_key", unique = true, length = 100)
    private String uniqueKey;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", insertable = false, updatable = false)
    private SocialMember owner;

    @Column(length = 100)
    private String title;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 255)
    private String lastMessagePreview;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isGroup() {
        return this.type == ConversationType.GROUP;
    }

    public boolean isDirect() {
        return this.type == ConversationType.DIRECT;
    }

    public void updateLastMessage(String preview) {
        updateLastMessage(LocalDateTime.now(), preview);
    }

    public void updateLastMessage(LocalDateTime messageAt, String preview) {
        this.lastMessageAt = messageAt;
        this.lastMessagePreview = preview;
    }

    public void clearLastMessage() {
        this.lastMessageAt = null;
        this.lastMessagePreview = null;
    }

    public void reactivate(Long newOwnerId) {
        this.deletedAt = null;
        if (newOwnerId != null) {
            this.ownerId = newOwnerId;
        }
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
