/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.platform.web.PageDto;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** WP-3.1 gate: enable when {@link #PENDING_MIGRATION} is empty. */
class BoundedListConventionTest {

    /** Remove handler ids as WP-2.x lands; must be empty before merge to development. */
    private static final Set<String> PENDING_MIGRATION = Set.of();

    @Test
    void collectionGetHandlersArePagedOrBounded() throws ClassNotFoundException {
        List<String> violations = new ArrayList<>();
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        for (var bean : scanner.findCandidateComponents("com.emberone")) {
            Class<?> type = Class.forName(bean.getBeanClassName());
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(GetMapping.class)) {
                    continue;
                }
                if (!returnsBareCollection(method)) {
                    continue;
                }
                if (method.isAnnotationPresent(BoundedList.class)) {
                    continue;
                }
                String id = type.getSimpleName() + "#" + method.getName();
                if (PENDING_MIGRATION.contains(id)) {
                    continue;
                }
                violations.add(id);
            }
        }
        assertThat(violations)
                .withFailMessage(
                        "Collection GET without @BoundedList or PageDto:%n%s",
                        String.join("\n", violations))
                .isEmpty();
    }

    private static boolean returnsBareCollection(Method method) {
        return analyzeType(method.getGenericReturnType());
    }

    private static boolean analyzeType(Type type) {
        if (type instanceof ParameterizedType parameterized) {
            Type raw = parameterized.getRawType();
            if (raw instanceof Class<?> rawClass) {
                if (rawClass.getName().equals("org.springframework.http.ResponseEntity")) {
                    return analyzeType(parameterized.getActualTypeArguments()[0]);
                }
                if (PageDto.class.isAssignableFrom(rawClass)
                        || rawClass.getSimpleName().endsWith("PageDto")) {
                    return false;
                }
                if (Collection.class.isAssignableFrom(rawClass)) {
                    return true;
                }
            }
        }
        if (type instanceof Class<?> clazz) {
            return Collection.class.isAssignableFrom(clazz);
        }
        return false;
    }
}
