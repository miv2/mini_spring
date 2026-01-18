package co.kr.mini_spring.member.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "member",
        indexes = {
                @Index(name = "idx_email", columnList = "email", unique = true),
                @Index(name = "idx_status_created", columnList = "status, created_at"),
                @Index(name = "idx_nickname", columnList = "nickname"),
                @Index(name = "idx_oauth_lookup", columnList = "oauth_provider, oauth_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100, unique = true)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private MemberProvider oauthProvider;

    @Column(name = "oauth_id", length = 255, unique = true)
    private String oauthId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Member(String email,
                  String passwordHash,
                  String name,
                  String nickname,
                  String profileImageUrl,
                  MemberRole role,
                  MemberStatus status,
                  MemberProvider oauthProvider,
                  String oauthId) {
        this.email = email == null ? null : email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.role = role == null ? MemberRole.USER : role;
        this.status = status == null ? MemberStatus.ACTIVE : status;
        this.oauthProvider = oauthProvider == null ? MemberProvider.LOCAL : oauthProvider;
        this.oauthId = oauthId;
    }

    public Member update(String name, String nickname, String profileImageUrl) {
        this.name = name;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        return this;
    }

    public void changeRole(MemberRole role) {
        this.role = role;
    }

    public void changeProvider(MemberProvider provider, String oauthId) {
        this.oauthProvider = provider;
        this.oauthId = oauthId;
    }

    public void changeStatus(MemberStatus status) {
        this.status = status;
    }
}
