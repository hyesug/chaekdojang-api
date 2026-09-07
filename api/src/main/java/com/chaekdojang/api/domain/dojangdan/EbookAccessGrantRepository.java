package com.chaekdojang.api.domain.dojangdan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EbookAccessGrantRepository extends JpaRepository<EbookAccessGrant, Long> {

    Optional<EbookAccessGrant> findByApplicationId(Long applicationId);

    List<EbookAccessGrant> findByApplicationIdIn(Collection<Long> applicationIds);
}
