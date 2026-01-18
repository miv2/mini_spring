package co.kr.mini_spring.member.domain.repository;

import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findById(Long id);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByNickname(String nickname);
    Optional<Member> findByOauthProviderAndOauthId(MemberProvider provider, String oauthId);

    long countByStatus(MemberStatus status);
}
