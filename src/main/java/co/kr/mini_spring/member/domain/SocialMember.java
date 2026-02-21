package co.kr.mini_spring.member.domain;

import co.kr.mini_spring.global.common.file.domain.StoredFile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 소셜 회원 엔티티 (OAuth2 기반)
 */
@Entity
@Table(name = "social_member", indexes = {
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_nickname", columnList = "nickname"),
        @Index(name = "idx_status_created", columnList = "status, created_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_provider_oauth_id", columnNames = { "provider", "oauth_id" })
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class SocialMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberProvider provider;

    @Column(name = "oauth_id", nullable = false, length = 255)
    private String oauthId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String nickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_image_id")
    private StoredFile profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MemberRole role = MemberRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.status = MemberStatus.WITHDRAWN;
    }

    public void updateProfileImage(StoredFile profileImage) {
        this.profileImage = profileImage;
    }

    public String getProfileImageUrl(String baseUrl, String defaultUrl) {
        return profileImage != null ? profileImage.getFullUrl(baseUrl) : defaultUrl;
    }

    public void updateProfile(String name, String nickname) {
        this.name = name;
        this.nickname = nickname;
    }
}
