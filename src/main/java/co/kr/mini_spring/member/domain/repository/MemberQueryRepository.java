package co.kr.mini_spring.member.domain.repository;

import co.kr.mini_spring.global.common.file.domain.QImageFile;
import co.kr.mini_spring.member.domain.Member;
import co.kr.mini_spring.member.domain.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Member 도메인 전용 Querydsl 리포지토리
 * - 회원 정보와 연관된 프로필 이미지 등을 최적화하여 조회합니다.
 */
@Repository
@RequiredArgsConstructor
public class MemberQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 이메일로 회원을 조회하며 프로필 이미지 정보를 Fetch Join합니다.
     * - 로그인 및 인증 시 N+1 문제를 방지하기 위해 사용합니다.
     */
    public Optional<Member> findByEmailWithProfileImage(String email) {
        QMember member = QMember.member;
        QImageFile profileImage = QImageFile.imageFile;

        return Optional.ofNullable(
                queryFactory.selectFrom(member)
                        .leftJoin(member.profileImage, profileImage).fetchJoin()
                        .where(member.email.eq(email))
                        .fetchOne()
        );
    }

    /**
     * ID로 회원을 조회하며 프로필 이미지 정보를 Fetch Join합니다.
     * - 회원 상세 정보 조회 및 프로필 업데이트 시 사용합니다.
     */
    public Optional<Member> findByIdWithProfileImage(Long id) {
        QMember member = QMember.member;
        QImageFile profileImage = QImageFile.imageFile;

        return Optional.ofNullable(
                queryFactory.selectFrom(member)
                        .leftJoin(member.profileImage, profileImage).fetchJoin()
                        .where(member.id.eq(id))
                        .fetchOne()
        );
    }
}
