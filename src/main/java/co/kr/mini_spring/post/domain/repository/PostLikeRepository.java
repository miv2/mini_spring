package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {
}
