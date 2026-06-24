package com.chaekdojang.api.domain.officialprofile;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficialProfileMemberRepository extends JpaRepository<OfficialProfileMember, Long> {

    boolean existsByProfileIdAndUserId(Long profileId, Long userId);
}
