package com.chaekdojang.api.domain.contest;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ContestRepository extends JpaRepository<Contest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Contest c where c.id = :id")
    Optional<Contest> findForUpdate(@Param("id") Long id);

    List<Contest> findByStatusInOrderBySubmitEndAtDesc(Collection<ContestStatus> statuses);

    List<Contest> findByHostIdInOrderByCreatedAtDesc(Collection<Long> hostProfileIds);
}
