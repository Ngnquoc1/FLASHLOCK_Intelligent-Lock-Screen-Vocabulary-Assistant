package com.nhom18.flashlock.ui.vocabulary;

import com.nhom18.flashlock.data.model.Word;
import java.util.ArrayList;
import java.util.List;

public final class WordFilter {
    private WordFilter() {
    }

    public static List<Word> apply(List<Word> source, String statusFilter, String searchQuery, boolean applyQuery) {
        List<Word> filtered = new ArrayList<>();
        if (source == null) {
            return filtered;
        }
        String normalizedQuery = applyQuery ? normalize(searchQuery) : "";
        for (Word word : source) {
            if (statusFilter != null) {
                if (word == null) {
                    continue;
                }
                String status = word.getStatus();
                boolean match = statusFilter.equals(status);
                if (!match && Word.STATUS_LEARNING.equals(statusFilter)) {
                    match = Word.STATUS_REVIEW.equals(status);
                }
                if (!match) {
                    continue;
                }
            }
            if (word != null && matchesQuery(word, normalizedQuery)) {
                filtered.add(word);
            }
        }
        return filtered;
    }

    public static List<Word> apply(List<Word> source, String statusFilter, String searchQuery) {
        return apply(source, statusFilter, searchQuery, true);
    }

    private static boolean matchesQuery(Word word, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }
        String term = normalize(word.getTerm());
        String definition = normalize(word.getDefinition());
        return term.contains(normalizedQuery) || definition.contains(normalizedQuery);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
