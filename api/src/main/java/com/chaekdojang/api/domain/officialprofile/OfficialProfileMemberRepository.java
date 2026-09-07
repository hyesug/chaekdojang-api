package com.chaekdojang.api.domain.officialprofile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OfficialProfileMemberRepository extends JpaRepository<OfficialProfileMember, Long> {

    boolean existsByProfileIdAndUserId(Long profileId, Long userId);

    List<OfficialProfileMember> findByUserId(Long userId);
}
