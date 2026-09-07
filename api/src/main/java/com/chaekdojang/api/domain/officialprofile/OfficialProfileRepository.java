package com.chaekdojang.api.domain.officialprofile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface OfficialProfileRepository extends JpaRepository<OfficialProfile, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from OfficialProfile p where p.id = :id")
    Optional<OfficialProfile> findForUpdate(@Param("id") Long id);

    boolean existsBySlug(String slug);

    Optional<OfficialProfile> findBySlugAndStatus(String slug, OfficialProfileStatus status);

    List<OfficialProfile> findAllByStatusOrderByFeaturedDescDisplayNameAsc(OfficialProfileStatus status);
}
