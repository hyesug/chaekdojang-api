package com.chaekdojang.api.domain.contest;

import com.chaekdojang.api.domain.book.Book;
import com.chaekdojang.api.domain.book.BookRepository;
import com.chaekdojang.api.domain.contest.dto.*;
import com.chaekdojang.api.domain.notification.NotificationService;
import com.chaekdojang.api.domain.notification.NotificationType;
import com.chaekdojang.api.domain.officialprofile.OfficialProfile;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileMember;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileMemberRepository;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileRepository;
import com.chaekdojang.api.domain.officialprofile.OfficialProfileType;
import com.chaekdojang.api.domain.user.User;
import com.chaekdojang.api.domain.user.UserRepository;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import com.chaekdojang.api.global.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 도서관·출판사 등 공식 프로필과 책도장 운영진이 공모전을 여는 기능.
 * 책도장이 직접 여는 공모전은 PLATFORM 타입 공식 프로필로 주최하며, 관리자만 그 프로필을 쓸 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContestManageService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private static final Map<ContestStatus, Set<ContestStatus>> ALLOWED_TRANSITIONS = Map.of(
            ContestStatus.DRAFT, Set.of(ContestStatus.OPEN),
            ContestStatus.OPEN, Set.of(ContestStatus.DRAFT, ContestStatus.CLOSED),
            ContestStatus.CLOSED, Set.of(ContestStatus.OPEN, ContestStatus.ANNOUNCED),
            ContestStatus.ANNOUNCED, Set.of()
    );

    private final ContestRepository contestRepository;
    private final ContestBookRepository contestBookRepository;
    private final ContestEntryRepository entryRepository;
    private final OfficialProfileRepository profileRepository;
    private final OfficialProfileMemberRepository profileMemberRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ContestAccessGuard accessGuard;

    /** 내가 공모전을 열 수 있는 프로필 목록. 관리자에게는 책도장 주최 프로필도 함께 보인다. */
    public List<HostProfileResponse> getHostProfiles() {
        return hostProfiles().stream().map(HostProfileResponse::from).toList();
    }

    @Transactional
    public ManageContestDetailResponse createContest(Long profileId, ContestCreateRequest request) {
        accessGuard.requireProfileAccess(profileId);
        validatePeriod(request.submitStartAt(), request.submitEndAt(), request.announceAt());

        OfficialProfile host = profileRepository.findById(profileId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        Contest contest = contestRepository.save(Contest.builder()
                .host(host)
                .title(request.title())
                .description(request.description())
                .prizeDescription(request.prizeDescription())
                .entryType(request.entryType())
                .submitStartAt(request.submitStartAt())
                .submitEndAt(request.submitEndAt())
                .announceAt(request.announceAt())
                .build());
        replaceBooks(contest, request.bookIds());
        return detailOf(contest);
    }

    public List<ContestSummaryResponse> getMyContests() {
        List<Long> profileIds = hostProfiles().stream().map(OfficialProfile::getId).toList();
        if (profileIds.isEmpty()) return List.of();

        LocalDateTime now = LocalDateTime.now(KST);
        return contestRepository.findByHostIdInOrderByCreatedAtDesc(profileIds).stream()
                .map(contest -> ContestSummaryResponse.of(
                        contest, entryCount(contest.getId()), books(contest.getId()), now))
                .toList();
    }

    public ManageContestDetailResponse getContestDetail(Long contestId) {
        return detailOf(accessGuard.requireContestAccess(contestId));
    }

    @Transactional
    public ManageContestDetailResponse updateContest(Long contestId, ContestUpdateRequest request) {
        Contest contest = accessGuard.requireContestWriteAccess(contestId);
        validatePeriod(request.submitStartAt(), request.submitEndAt(), request.announceAt());
        contest.update(request.title(), request.description(), request.prizeDescription(),
                request.entryType(), request.submitStartAt(), request.submitEndAt(), request.announceAt());
        replaceBooks(contest, request.bookIds());
        return detailOf(contest);
    }

    @Transactional
    public ManageContestDetailResponse updateStatus(Long contestId, ContestStatusUpdateRequest request) {
        Contest contest = accessGuard.requireContestWriteAccess(contestId);
        if (!ALLOWED_TRANSITIONS.getOrDefault(contest.getStatus(), Set.of()).contains(request.status())) {
            throw new CustomException(ErrorCode.CONTEST_STATUS_TRANSITION_NOT_ALLOWED);
        }

        if (request.status() == ContestStatus.ANNOUNCED) {
            announce(contest);
        } else {
            contest.changeStatus(request.status());
        }
        return detailOf(contest);
    }

    public List<ContestEntryResponse> getEntries(Long contestId) {
        accessGuard.requireContestAccess(contestId);
        return entryRepository.findByContestIdOrderBySubmittedAtAsc(contestId).stream()
                .map(ContestEntryResponse::from)
                .toList();
    }

    /** 수상자 지정. 심사(접수 마감) 단계에서만 바꿀 수 있고, 발표 시점에 그대로 확정된다. */
    @Transactional
    public List<ContestEntryResponse> saveAwards(Long contestId, ContestAwardSaveRequest request) {
        Contest contest = accessGuard.requireContestWriteAccess(contestId);
        if (contest.getStatus() != ContestStatus.CLOSED) {
            throw new CustomException(ErrorCode.CONTEST_AWARD_STAGE_INVALID);
        }

        Map<Long, ContestAwardSaveRequest.AwardItem> byEntryId = new LinkedHashMap<>();
        Set<Integer> ranks = new HashSet<>();
        for (ContestAwardSaveRequest.AwardItem item : request.awards()) {
            if (byEntryId.put(item.entryId(), item) != null || !ranks.add(item.awardRank())) {
                throw new CustomException(ErrorCode.CONTEST_AWARD_DUPLICATED);
            }
        }

        for (ContestEntry entry : entryRepository.findByContestIdOrderBySubmittedAtAsc(contestId)) {
            ContestAwardSaveRequest.AwardItem item = byEntryId.remove(entry.getId());
            if (item != null) {
                if (entry.getStatus() == ContestEntryStatus.WITHDRAWN) {
                    throw new CustomException(ErrorCode.CONTEST_AWARD_ENTRY_INVALID);
                }
                entry.award(item.awardRank(), item.awardName().trim());
            } else if (entry.getStatus() == ContestEntryStatus.AWARDED) {
                entry.clearAward();
            }
        }
        // 이 공모전에 없는 응모작 id를 보낸 경우
        if (!byEntryId.isEmpty()) {
            throw new CustomException(ErrorCode.CONTEST_AWARD_ENTRY_INVALID);
        }
        return getEntries(contestId);
    }

    /** 수상 발표. 지정된 수상작은 그대로 확정하고, 나머지 응모작은 미수상으로 확정하며 양쪽 모두에게 알린다. */
    private void announce(Contest contest) {
        List<ContestEntry> entries = entryRepository.findByContestIdOrderBySubmittedAtAsc(contest.getId());
        boolean hasAward = entries.stream().anyMatch(entry -> entry.getStatus() == ContestEntryStatus.AWARDED);
        if (!hasAward) {
            throw new CustomException(ErrorCode.CONTEST_AWARD_REQUIRED);
        }

        User actor = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        for (ContestEntry entry : entries) {
            if (entry.getStatus() == ContestEntryStatus.AWARDED) {
                notificationService.send(entry.getUser(), actor,
                        NotificationType.CONTEST_AWARDED, contest.getId());
            } else if (entry.getStatus() == ContestEntryStatus.SUBMITTED) {
                entry.markNotAwarded();
                notificationService.send(entry.getUser(), actor,
                        NotificationType.CONTEST_NOT_AWARDED, contest.getId());
            }
        }
        contest.changeStatus(ContestStatus.ANNOUNCED);
    }

    private List<OfficialProfile> hostProfiles() {
        List<OfficialProfile> profiles = new ArrayList<>(
                profileMemberRepository.findByUserId(SecurityUtils.getCurrentUserId()).stream()
                        .map(OfficialProfileMember::getProfile)
                        .toList());
        if (SecurityUtils.hasAnyRole("ADMIN", "SUPER_ADMIN")) {
            Set<Long> ids = profiles.stream().map(OfficialProfile::getId).collect(Collectors.toSet());
            profileRepository.findAllByType(OfficialProfileType.PLATFORM).stream()
                    .filter(profile -> !ids.contains(profile.getId()))
                    .forEach(profiles::add);
        }
        return profiles;
    }

    /** 지정 도서는 항상 요청받은 목록으로 통째로 바꾼다. 순서·중복을 신경 쓸 필요가 없어 단순하다. */
    private void replaceBooks(Contest contest, List<Long> bookIds) {
        contestBookRepository.deleteByContestId(contest.getId());
        if (bookIds == null || bookIds.isEmpty()) return;

        for (Long bookId : bookIds.stream().distinct().toList()) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new CustomException(ErrorCode.BOOK_NOT_FOUND));
            contestBookRepository.save(ContestBook.of(contest, book));
        }
    }

    private ManageContestDetailResponse detailOf(Contest contest) {
        Long contestId = contest.getId();
        return ManageContestDetailResponse.of(
                contest,
                books(contestId),
                LocalDateTime.now(KST),
                entryCount(contestId),
                entryRepository.countByContestIdAndStatus(contestId, ContestEntryStatus.AWARDED),
                entryRepository.countByContestIdAndStatus(contestId, ContestEntryStatus.NOT_AWARDED),
                entryRepository.countByContestIdAndStatus(contestId, ContestEntryStatus.WITHDRAWN)
        );
    }

    private List<ContestBookResponse> books(Long contestId) {
        return contestBookRepository.findByContestIdOrderByIdAsc(contestId).stream()
                .map(contestBook -> ContestBookResponse.from(contestBook.getBook()))
                .toList();
    }

    private long entryCount(Long contestId) {
        return entryRepository.countByContestIdAndStatusNot(contestId, ContestEntryStatus.WITHDRAWN);
    }

    private void validatePeriod(LocalDateTime start, LocalDateTime end, LocalDateTime announce) {
        if (!start.isBefore(end) || announce.isBefore(end)) {
            throw new CustomException(ErrorCode.CONTEST_INVALID_PERIOD);
        }
    }
}
