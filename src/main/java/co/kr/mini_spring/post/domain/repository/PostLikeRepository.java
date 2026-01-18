package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {
    Optional<PostLike> findByMemberIdAndPostId(Long memberId, Long postId);
}
