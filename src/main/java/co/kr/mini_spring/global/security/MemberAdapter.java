package co.kr.mini_spring.global.security;

import co.kr.mini_spring.member.domain.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Member 엔티티를 Spring Security의 UserDetails로 변환하는 어댑터 클래스
 * 도메인 엔티티가 프레임워크에 직접 의존하지 않도록 분리
 */
@Getter
@RequiredArgsConstructor
public class MemberAdapter implements UserDetails {

    private final Member member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(member.getRole().getKey()));
    }

    @Override
    public String getPassword() {
        return member.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return member.getStatus() != co.kr.mini_spring.member.domain.MemberStatus.WITHDRAWN;
    }

    @Override
    public boolean isAccountNonLocked() {
        return member.getStatus() != co.kr.mini_spring.member.domain.MemberStatus.SUSPENDED
            && member.getStatus() != co.kr.mini_spring.member.domain.MemberStatus.WITHDRAWN;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.getStatus() == co.kr.mini_spring.member.domain.MemberStatus.ACTIVE;
    }
}
