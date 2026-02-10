package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLike.PostLikeId> {
    @Query("SELECT pl FROM PostLike pl WHERE pl.id.authorId = :authorId AND pl.id.postId = :postId")
    Optional<PostLike> findLike(@Param("authorId") Long authorId, @Param("postId") Long postId);
}
