package co.kr.mini_spring.chat.domain;

import co.kr.mini_spring.member.domain.SocialMember;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_messages_conv_id_desc", columnList = "conversation_id, id DESC"),
        @Index(name = "idx_messages_sender_id", columnList = "sender_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_messages_conv_client_id", columnNames = {"conversation_id", "client_message_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", insertable = false, updatable = false)
    private Conversation conversation;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private SocialMember sender;

    @Column(name = "client_message_id", nullable = false, length = 100)
    private String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MessageType type = MessageType.TEXT;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}

