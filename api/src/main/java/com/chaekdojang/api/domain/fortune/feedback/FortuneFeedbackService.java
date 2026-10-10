package com.chaekdojang.api.domain.fortune.feedback;

import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.CreateRequest;
import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.Item;
import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.SectionCount;
import com.chaekdojang.api.domain.fortune.feedback.dto.FortuneFeedbackDtos.Summary;
import com.chaekdojang.api.global.exception.CustomException;
import com.chaekdojang.api.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FortuneFeedbackService {

    private static final Set<String> MODES = Set.of("solo", "pair");
    private static final Set<String> VERDICTS = Set.of("up", "down");

    // 한 줄 입력에 들어올 수 있는 개인정보 — 이메일, 전화번호, 출생일처럼 보이는 숫자
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final Pattern PHONE = Pattern.compile("(?:\\+?82[-\\s]?)?0?1[016789][-\\s.]?\\d{3,4}[-\\s.]?\\d{4}");
    private static final Pattern DATE = Pattern.compile("(?:19|20)\\d{2}\\s*[-./년]\\s*\\d{1,2}\\s*[-./월]\\s*\\d{1,2}\\s*일?");
    private static final Pattern DIGITS = Pattern.compile("\\d{6,}");

    private final FortuneFeedbackRepository repository;

    @Transactional
    public void create(CreateRequest req) {
        String mode = req.mode() == null ? "" : req.mode().trim();
        String verdict = req.verdict() == null ? "" : req.verdict().trim();
        if (!MODES.contains(mode) || !VERDICTS.contains(verdict)) {
            throw new CustomException(ErrorCode.FORTUNE_FEEDBACK_INVALID);
        }
        String section = clip(req.section(), 80);
        if (section == null) throw new CustomException(ErrorCode.FORTUNE_FEEDBACK_INVALID);
        repository.save(new FortuneFeedback(mode, section, verdict, clip(req.snippet(), 600), clip(scrub(req.comment()), 300)));
    }

    @Transactional(readOnly = true)
    public Summary summary(int limit) {
        Map<String, long[]> counts = new LinkedHashMap<>();
        for (Object[] row : repository.countBySectionAndVerdict()) {
            long[] c = counts.computeIfAbsent((String) row[0], k -> new long[2]);
            c["up".equals(row[1]) ? 0 : 1] += (Long) row[2];
        }
        List<SectionCount> sections = counts.entrySet().stream()
                .map(e -> new SectionCount(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .sorted((a, b) -> Long.compare(b.down(), a.down()))
                .toList();
        List<Item> recent = repository.findAllByOrderByIdDesc(PageRequest.of(0, Math.max(1, Math.min(limit, 500)))).stream()
                .map(Item::from)
                .toList();
        return new Summary(sections, recent);
    }

    /** 이메일·전화번호·출생일처럼 보이는 것을 지운다 */
    static String scrub(String s) {
        if (s == null) return null;
        String out = EMAIL.matcher(s).replaceAll("[생략]");
        out = PHONE.matcher(out).replaceAll("[생략]");
        out = DATE.matcher(out).replaceAll("[생략]");
        out = DIGITS.matcher(out).replaceAll("[생략]");
        return out;
    }

    private static String clip(String s, int max) {
        if (s == null) return null;
        String t = s.strip();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }
}
