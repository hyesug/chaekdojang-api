package com.chaekdojang.api.domain.officialprofile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialProfileApplicationRepository extends JpaRepository<OfficialProfileApplication, Long> {

    List<OfficialProfileApplication> findAllByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    Page<OfficialProfileApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
