package com.chaekdojang.api.domain.inquiry;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    List<Inquiry> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId);

    List<Inquiry> findAllByGuestEmailAndDeletedAtIsNullOrderByCreatedAtDesc(String guestEmail);

    Page<Inquiry> findAllByDeletedAtIsNull(Pageable pageable);

    Optional<Inquiry> findByIdAndDeletedAtIsNull(Long id);

    List<Inquiry> findAllByUserIdAndDeletedAtIsNull(Long userId);
}
