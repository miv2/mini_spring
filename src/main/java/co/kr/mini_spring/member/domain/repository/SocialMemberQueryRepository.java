package co.kr.mini_spring.member.domain.repository;

import co.kr.mini_spring.member.domain.SocialMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import static co.kr.mini_spring.member.domain.QSocialMember.socialMember;

/**
 * SocialMember 도메인 전용 Querydsl 리포지토리
 * - 복잡한 회원 조회 및 연관관계 Fetch Join을 담당합니다.
 */
@Repository
@RequiredArgsConstructor
public class SocialMemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * ID로 회원 조회 (프로필 이미지 포함)
     * - Fetch Join을 사용하여 N+1 문제를 방지합니다.
     */
    public Optional<SocialMember> findByIdWithProfileImage(Long memberId) {
        SocialMember result = queryFactory
                .selectFrom(socialMember)
                .leftJoin(socialMember.profileImage).fetchJoin()
                .where(socialMember.id.eq(memberId))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    /**
     * 이메일로 회원 조회 (프로필 이미지 포함)
     */
    public Optional<SocialMember> findByEmailWithProfileImage(String email) {
        SocialMember result = queryFactory
                .selectFrom(socialMember)
                .leftJoin(socialMember.profileImage).fetchJoin()
                .where(socialMember.email.eq(email))
                .fetchOne();
        return Optional.ofNullable(result);
    }

    /**
     * 여러 ID에 해당하는 회원 목록 조회 (프로필 이미지 포함)
     * - 댓글 목록 조회 시 여러 작성자 정보를 효율적으로 가져오기 위해 사용합니다.
     */
    public java.util.List<SocialMember> findAllByIdWithProfileImage(java.util.Collection<Long> memberIds) {
        return queryFactory
                .selectFrom(socialMember)
                .leftJoin(socialMember.profileImage).fetchJoin()
                .where(socialMember.id.in(memberIds))
                .fetch();
    }
}
