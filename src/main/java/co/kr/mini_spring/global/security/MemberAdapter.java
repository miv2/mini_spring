package co.kr.mini_spring.global.security;

import co.kr.mini_spring.member.domain.SocialMember;
import co.kr.mini_spring.member.domain.MemberStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * SocialMember 엔티티를 Spring Security의 UserDetails 및 OAuth2User로 변환하는 어댑터 클래스
 * - 일반 로그인(JWT)과 소셜 로그인(OAuth2) 모두에서 Principal로 사용됨
 */
@Getter
public class MemberAdapter implements UserDetails, OAuth2User {

    private final SocialMember member;
    private final Map<String, Object> attributes;

    // JWT 인증용 생성자 (attributes 없음)
    public MemberAdapter(SocialMember member) {
        this(member, Collections.emptyMap());
    }

    // OAuth2 인증용 생성자
    public MemberAdapter(SocialMember member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(member.getRole().getKey()));
    }

    @Override
    public String getPassword() {
        return null; // 소셜 로그인은 비밀번호 없음
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return member.getStatus() != MemberStatus.WITHDRAWN;
    }

    @Override
    public boolean isAccountNonLocked() {
        return member.getStatus() != MemberStatus.SUSPENDED
                && member.getStatus() != MemberStatus.WITHDRAWN;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.getStatus() == MemberStatus.ACTIVE;
    }

    // OAuth2User methods
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return member.getNickname();
    }
}
