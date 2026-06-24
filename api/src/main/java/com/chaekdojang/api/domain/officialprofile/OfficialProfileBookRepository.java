package com.chaekdojang.api.domain.officialprofile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialProfileBookRepository extends JpaRepository<OfficialProfileBook, Long> {

    List<OfficialProfileBook> findAllByProfileIdOrderByCreatedAtDesc(Long profileId);

    boolean existsByProfileIdAndBookId(Long profileId, Long bookId);

    void deleteByProfileIdAndBookId(Long profileId, Long bookId);
}
