package co.kr.mini_spring.global.security;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.repository.MemberRepository;
import co.kr.mini_spring.member.domain.repository.MemberQueryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService 구현
 * - 인증 시 사용자 정보 로드
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;
    private final MemberQueryRepository memberQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("[UserDetailsService] 사용자 조회 email={}", email);

        // 최적화된 Querydsl 조회 사용 (프로필 이미지 Fetch Join)
        Member member = memberQueryRepository.findByEmailWithProfileImage(email)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + email));

        return new MemberAdapter(member);
    }
}