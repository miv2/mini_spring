package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface PostRepositoryCustom {

    Optional<Post> findByIdWithPessimisticLock(Long id);

    Optional<Post> findByIdWithAllRelations(Long id);

    Page<Post> findAllByPublished(boolean published, Pageable pageable, String keyword, List<String> hashtags, Long authorId);
}
