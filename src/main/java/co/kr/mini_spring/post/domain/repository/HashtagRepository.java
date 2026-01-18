package co.kr.mini_spring.post.domain.repository;

import co.kr.mini_spring.post.domain.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HashtagRepository extends JpaRepository<Hashtag, Long> {
    Optional<Hashtag> findByName(String name);
    List<Hashtag> findByNameIn(List<String> names);
}
