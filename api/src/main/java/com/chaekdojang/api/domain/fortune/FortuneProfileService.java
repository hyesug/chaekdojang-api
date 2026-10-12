package com.chaekdojang.api.domain.fortune;

import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FortuneProfileService {

    /** 한 사람이 저장할 수 있는 프로필 수 (본인 포함) */
    static final int MAX_PROFILES = 20;

    private final FortuneProfileRepository fortuneProfileRepository;
    private final UserRepository userRepository;

    /** 내 프로필. 저장한 적이 없으면 null */
    public FortuneProfileResponse getMine(Long userId) {
        return fortuneProfileRepository.findByUserIdAndSelfTrue(userId)
                .map(FortuneProfileResponse::from)
                .orElse(null);
    }

    @Transactional
    public FortuneProfileResponse saveMine(Long userId, FortuneProfileRequest req) {
        validateDate(req);
        FortuneProfile profile = fortuneProfileRepository.findByUserIdAndSelfTrue(userId)
                .orElseGet(() -> newProfile(userId, true));
        apply(profile, req);
        return FortuneProfileResponse.from(fortuneProfileRepository.save(profile));
    }

    @Transactional
    public void deleteMine(Long userId) {
        fortuneProfileRepository.findByUserIdAndSelfTrue(userId).ifPresent(fortuneProfileRepository::delete);
    }

    /** 저장한 프로필 전부 — 본인이 맨 앞 */
    public List<FortuneProfileResponse> list(Long userId) {
        return fortuneProfileRepository.findAllByUserIdOrderBySelfDescCreatedAtAscIdAsc(userId).stream()
                .map(FortuneProfileResponse::from)
                .toList();
    }

    /** 새로 하나 더 저장한다. 아직 아무것도 없으면 그것이 본인 프로필이 된다 */
    @Transactional
    public FortuneProfileResponse create(Long userId, FortuneProfileRequest req) {
        validateDate(req);
        long count = fortuneProfileRepository.countByUserId(userId);
        if (count >= MAX_PROFILES) throw new CustomException(ErrorCode.FORTUNE_PROFILE_LIMIT);
        FortuneProfile profile = newProfile(userId, count == 0);
        apply(profile, req);
        return FortuneProfileResponse.from(fortuneProfileRepository.save(profile));
    }

    @Transactional
    public FortuneProfileResponse update(Long userId, Long id, FortuneProfileRequest req) {
        validateDate(req);
        FortuneProfile profile = owned(userId, id);
        apply(profile, req);
        return FortuneProfileResponse.from(profile);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        fortuneProfileRepository.delete(owned(userId, id));
    }

    /** 남의 프로필 id 를 넣어도 '없음'으로 답한다 — 있는지조차 알려 주지 않는다 */
    private FortuneProfile owned(Long userId, Long id) {
        return fortuneProfileRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.FORTUNE_PROFILE_NOT_FOUND));
    }

    private FortuneProfile newProfile(Long userId, boolean self) {
        return FortuneProfile.create(userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)), self);
    }

    private static void validateDate(FortuneProfileRequest req) {
        try {
            LocalDate.of(req.year(), req.month(), req.day());
        } catch (DateTimeException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static void apply(FortuneProfile profile, FortuneProfileRequest req) {
        profile.update(req.name().trim(), req.gender(), req.year(), req.month(), req.day(),
                req.hour(), req.minute() == null ? 0 : req.minute(),
                req.birthPlace().trim(), req.homePlace().trim(), req.dst());
    }
}
