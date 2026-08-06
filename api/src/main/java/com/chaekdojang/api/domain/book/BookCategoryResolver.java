package com.chaekdojang.api.domain.book;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<String> targetKeys = targets.stream().map(BookWorkKey::of).collect(Collectors.toSet());
        List<Book> candidates = new ArrayList<>(bookRepository.findTop1000ByDeletedAtIsNullAndIsPublicTrueOrderByUpdatedAtDesc());
        targets.forEach(book -> {
            if (!candidates.contains(book)) candidates.add(book);
        });

        Map<String, List<Book>> works = candidates.stream()
                .filter(book -> targetKeys.contains(BookWorkKey.of(book)))
                .collect(Collectors.groupingBy(BookWorkKey::of, LinkedHashMap::new, Collectors.toList()));

        for (Book target : targets) {
            String direct = BookGenreClassifier.resolve(target);
            String consensus = consensus(works.getOrDefault(BookWorkKey.of(target), List.of()));
            resolved.put(target, consensus != null ? consensus : direct);
        }
        return resolved;
    }

    private String consensus(List<Book> editions) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Book edition : editions) {
            String inferred = BookGenreClassifier.resolve(
                    null, edition.getSource(), edition.getTitle(), edition.getAuthor(), edition.getDescription());
            String category = inferred != null ? inferred : BookGenreClassifier.resolve(edition);
            if (category == null) continue;
            int weight = inferred != null ? 3 : 2;
            scores.merge(category, weight, Integer::sum);
        }
        if (scores.isEmpty()) return null;

        List<Map.Entry<String, Integer>> ranked = scores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .toList();
        if (ranked.size() > 1 && ranked.get(0).getValue().equals(ranked.get(1).getValue())) return null;
        return ranked.get(0).getKey();
    }
}
