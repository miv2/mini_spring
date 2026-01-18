package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.PostView;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostViewRepository extends JpaRepository<PostView, PostView.PostViewId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pv from PostView pv where pv.id.memberId = :memberId and pv.id.postId = :postId")
    Optional<PostView> findByMemberIdAndPostIdWithLock(
            @Param("memberId") Long memberId,
            @Param("postId") Long postId
    );
}
