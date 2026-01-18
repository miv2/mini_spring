package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.PostHashtag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, PostHashtag.PostHashtagId> {
}
