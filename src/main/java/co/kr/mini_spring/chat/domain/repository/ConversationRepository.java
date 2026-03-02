package co.kr.mini_spring.chat.domain.repository;

import co.kr.mini_spring.chat.domain.Conversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUniqueKey(String uniqueKey);

    @Query(value = "SELECT * FROM conversations WHERE unique_key = :uniqueKey LIMIT 1", nativeQuery = true)
    Optional<Conversation> findByUniqueKeyIncludingDeleted(@Param("uniqueKey") String uniqueKey);

    Optional<Conversation> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Conversation c where c.id = :id and c.deletedAt is null")
    Optional<Conversation> findByIdAndDeletedAtIsNullForUpdate(@Param("id") Long id);
}
