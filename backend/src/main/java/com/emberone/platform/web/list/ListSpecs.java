/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/** Reusable JPA {@link Specification} fragments for list endpoints. */
public final class ListSpecs {

    public static final int MAX_SEARCH_LENGTH = 100;
    public static final int MAX_SEARCH_TOKENS = 5;

    private ListSpecs() {}

    public static List<String> searchTokens(String search) {
        if (!StringUtils.hasText(search)) {
            return List.of();
        }
        String trimmed = search.trim();
        if (trimmed.length() > MAX_SEARCH_LENGTH) {
            throw new IllegalArgumentException("search exceeds maximum length");
        }
        String[] parts = trimmed.toLowerCase(Locale.ROOT).split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            tokens.add(part);
            if (tokens.size() > MAX_SEARCH_TOKENS) {
                throw new IllegalArgumentException("search has too many words");
            }
        }
        return tokens;
    }

    public static <T> Specification<T> searchTokens(String search, List<String> attributePaths) {
        List<String> tokens = searchTokens(search);
        if (tokens.isEmpty() || attributePaths.isEmpty()) {
            return unconstrained();
        }
        return (root, query, cb) -> {
            List<Predicate> tokenPredicates = new ArrayList<>();
            for (String token : tokens) {
                String pattern = "%" + escapeLike(token) + "%";
                List<Predicate> fieldMatches = new ArrayList<>();
                for (String attributePath : attributePaths) {
                    Expression<String> field = stringPath(root, attributePath);
                    fieldMatches.add(cb.like(cb.lower(field), pattern, '\\'));
                }
                tokenPredicates.add(cb.or(fieldMatches.toArray(Predicate[]::new)));
            }
            return cb.and(tokenPredicates.toArray(Predicate[]::new));
        };
    }

    public static <T> Specification<T> unconstrained() {
        return (root, query, cb) -> cb.conjunction();
    }

    public static <T, V> Specification<T> eq(Path<V> path, V value) {
        if (value == null) {
            return unconstrained();
        }
        return (root, query, cb) -> cb.equal(path, value);
    }

    public static <T, V> Specification<T> in(Path<V> path, Collection<V> values) {
        if (values == null || values.isEmpty()) {
            return unconstrained();
        }
        return (root, query, cb) -> path.in(values);
    }

    /** {@code from} inclusive, {@code to} exclusive. */
    public static <T> Specification<T> range(
            Path<Instant> path, Instant fromInclusive, Instant toExclusive) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (fromInclusive != null) {
                predicates.add(cb.greaterThanOrEqualTo(path, fromInclusive));
            }
            if (toExclusive != null) {
                predicates.add(cb.lessThan(path, toExclusive));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static <T> Specification<T> isNull(Path<?> path) {
        return (root, query, cb) -> cb.isNull(path);
    }

    public static <T> Specification<T> isNotNull(Path<?> path) {
        return (root, query, cb) -> cb.isNotNull(path);
    }

    public static String escapeLike(String raw) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '%' || c == '_' || c == '\\') {
                escaped.append('\\');
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    public static <T> Expression<String> stringPath(Root<T> root, String attributePath) {
        Path<?> path = resolvePath(root, attributePath);
        return path.as(String.class);
    }

    private static Path<?> resolvePath(Root<?> root, String attributePath) {
        String[] segments = attributePath.split("\\.");
        Path<?> current = root.get(segments[0]);
        for (int i = 1; i < segments.length; i++) {
            current = current.get(segments[i]);
        }
        return current;
    }
}
