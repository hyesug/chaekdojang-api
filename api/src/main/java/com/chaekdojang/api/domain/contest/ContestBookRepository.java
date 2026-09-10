package com.chaekdojang.api.domain.contest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContestBookRepository extends JpaRepository<ContestBook, Long> {

    List<ContestBook> findByContestIdOrderByIdAsc(Long contestId);

    void deleteByContestId(Long contestId);
}
