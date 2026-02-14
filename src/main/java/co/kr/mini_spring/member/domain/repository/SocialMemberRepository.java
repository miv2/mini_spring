package co.kr.mini_spring.member.domain.repository;

import co.kr.mini_spring.member.domain.MemberProvider;
import co.kr.mini_spring.member.domain.SocialMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialMemberRepository extends JpaRepository<SocialMember, Long> {

    Optional<SocialMember> findByEmail(String email);

    Optional<SocialMember> findByNickname(String nickname);

    Optional<SocialMember> findByProviderAndOauthId(MemberProvider provider, String oauthId);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    long countByStatus(co.kr.mini_spring.member.domain.MemberStatus status);
}
