package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
}
