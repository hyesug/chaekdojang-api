package com.chaekdojang.api.domain.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookCategoryResolver {
    private final BookRepository bookRepository;

    public String resolve(Book book) {
        if (book == null) return null;
        return resolveAll(List.of(book)).get(book);
    }

    public Map<Book, String> resolveAll(Collection<Book> targetBooks) {
        Map<Book, String> resolved = new IdentityHashMap<>();
        if (targetBooks == null || targetBooks.isEmpty()) return resolved;

        List<Book> targets = targetBooks.stream().filter(book -> book != null).distinct().toList();
        List<Book> candidates = new ArrayList<>(bookRepository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc());
        targets.forEach(book -> {
            if (!candidates.contains(book)) candidates.add(book);
        });

        for (Book target : targets) {
            String direct = BookGenreClassifier.resolve(target);
            String consensus = consensus(candidates.stream()
                    .filter(candidate -> BookWorkKey.sameWork(target, candidate))
                    .toList());
            resolved.put(target, consensus != null ? consensus : direct);
        }
        return resolved;
    }

    private String consensus(List<Book> editions) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        Map<String, Integer> evidenceScores = new LinkedHashMap<>();
        for (Book edition : editions) {
            String inferred = BookGenreClassifier.resolve(
                    null, edition.getSource(), edition.getTitle(), edition.getAuthor(), edition.getDescription());
            String category = edition.isCategoryVerified()
                    ? BookGenreClassifier.resolve(edition)
                    : inferred != null ? inferred : BookGenreClassifier.resolve(edition);
            if (category == null) continue;
            int weight = edition.isCategoryVerified() ? 5 : inferred != null ? 3 : 2;
            String evidenceKey = category + "\u0000" + (edition.isCategoryVerified()
                    ? "verified"
                    : inferred != null ? "text:" + normalizedEvidence(edition.getDescription())
                    : "external:" + edition.getSource());
            evidenceScores.merge(evidenceKey, weight, Math::max);
        }
        evidenceScores.forEach((key, weight) -> scores.merge(key.substring(0, key.indexOf('\u0000')), weight, Integer::sum));
        if (scores.isEmpty()) return null;

        List<Map.Entry<String, Integer>> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        if (ranked.size() > 1 && ranked.get(0).getValue().equals(ranked.get(1).getValue())) return null;
        return ranked.get(0).getKey();
    }

    private String normalizedEvidence(String description) {
        if (description == null) return "";
        return description.toLowerCase().replaceAll("[^가-힣a-z0-9]", "");
    }
}
