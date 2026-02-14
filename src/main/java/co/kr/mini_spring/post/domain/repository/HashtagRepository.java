package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    List<Hashtag> findByNameIn(List<String> names);
}
