package com.chaekdojang.api.domain.officialprofile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OfficialProfileRepository extends JpaRepository<OfficialProfile, Long> {

    boolean existsBySlug(String slug);

    Optional<OfficialProfile> findBySlugAndStatus(String slug, OfficialProfileStatus status);

    List<OfficialProfile> findAllByStatusOrderByFeaturedDescDisplayNameAsc(OfficialProfileStatus status);
}
