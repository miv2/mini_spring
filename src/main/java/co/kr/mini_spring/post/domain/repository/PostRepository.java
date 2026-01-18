package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long>, PostRepositoryCustom {

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    int incrementViewCount(@Param("id") Long id);

    /**
     * 게시글의 현재 조회수를 DB에서 조회합니다.
     * - 조회수 증가(UPDATE) 이후 응답에 최신 값을 반영할 때 사용합니다.
     */
    @Query("select p.viewCount from Post p where p.id = :id")
    Integer findViewCountById(@Param("id") Long id);

    /**
     * 댓글 카운트를 원자적으로 증가시킵니다.
     */
    @Modifying
    @Query("update Post p set p.commentCount = p.commentCount + 1 where p.id = :id")
    int incrementCommentCount(@Param("id") Long id);

    /**
     * 댓글 카운트를 0 이하로 내려가지 않도록 원자적으로 감소시킵니다.
     */
    @Modifying
    @Query("update Post p set p.commentCount = case when p.commentCount > 0 then p.commentCount - 1 else 0 end where p.id = :id")
    int decrementCommentCount(@Param("id") Long id);

    long countByPublished(boolean published);
}
