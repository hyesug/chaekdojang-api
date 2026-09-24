package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FortuneProfileService {

    private final FortuneProfileRepository fortuneProfileRepository;
    private final UserRepository userRepository;

    /** 저장한 적이 없으면 null */
    public FortuneProfileResponse getMine(Long userId) {
        return fortuneProfileRepository.findByUserId(userId)
                .map(FortuneProfileResponse::from)
                .orElse(null);
    }

    @Transactional
    public FortuneProfileResponse saveMine(Long userId, FortuneProfileRequest req) {
        try {
            LocalDate.of(req.year(), req.month(), req.day());
        } catch (DateTimeException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }

        FortuneProfile profile = fortuneProfileRepository.findByUserId(userId)
                .orElseGet(() -> FortuneProfile.create(userRepository.findById(userId)
                        .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND))));
        profile.update(req.name().trim(), req.gender(), req.year(), req.month(), req.day(),
                req.hour(), req.minute() == null ? 0 : req.minute(),
                req.birthPlace().trim(), req.homePlace().trim(), req.dst());
        return FortuneProfileResponse.from(fortuneProfileRepository.save(profile));
    }

    @Transactional
    public void deleteMine(Long userId) {
        fortuneProfileRepository.deleteByUserId(userId);
    }
}
