package co.kr.mini_spring.member.domain.repository;

import co.kr.mini_spring.member.domain.QSocialMember;
import co.kr.mini_spring.member.domain.SocialMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SocialMemberQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QSocialMember socialMember = QSocialMember.socialMember;

    /**
     * ID로 회원 조회 (프로필 이미지 포함)
     */
    public Optional<SocialMember> findByIdWithProfileImage(Long memberId) {
        SocialMember result = queryFactory
                .selectFrom(socialMember)
                .leftJoin(socialMember.profileImage).fetchJoin()
                .where(socialMember.id.eq(memberId))
                .fetchOne();
        return Optional.ofNullable(result);
    }
}
