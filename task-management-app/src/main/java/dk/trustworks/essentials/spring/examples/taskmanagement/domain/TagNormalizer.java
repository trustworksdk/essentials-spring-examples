package dk.trustworks.essentials.spring.examples.taskmanagement.domain;

import java.util.Optional;

public final class TagNormalizer {
    private TagNormalizer() {
    }

    public static Optional<String> normalize(String rawTag) {
        if (rawTag == null) {
            return Optional.empty();
        }

        var normalized = rawTag.trim().toLowerCase();
        if (normalized.isBlank() || normalized.length() > 30) {
            return Optional.empty();
        }

        for (int i = 0; i < normalized.length(); i++) {
            var ch = normalized.charAt(i);
            if (!Character.isLetterOrDigit(ch) && ch != '-' && ch != '_') {
                return Optional.empty();
            }
        }

        return Optional.of(normalized);
    }
}
