package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    Optional<UserBlock> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}

