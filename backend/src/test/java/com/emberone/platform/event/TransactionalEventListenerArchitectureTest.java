/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Guards against a silent-data-loss pitfall: a {@code @TransactionalEventListener} running in an
 * after-completion phase (AFTER_COMMIT etc.) still has the original, already-completed transaction
 * bound to the thread. Any {@code @Transactional(REQUIRED)} work invoked from such a listener joins
 * that dead transaction - repository saves succeed without error but are never committed.
 *
 * <p>Every after-phase listener must therefore open its own transaction ({@code REQUIRES_NEW}) or
 * opt out entirely ({@code NOT_SUPPORTED}). Prefer {@link AfterCommitEventListener} for DB work or
 * {@link AfterCommitCallback} for realtime/cache-only hooks.
 */
class TransactionalEventListenerArchitectureTest {

    private static final String BASE_PACKAGE = "com.emberone";
    private static final Set<TransactionPhase> AFTER_PHASES =
            EnumSet.of(
                    TransactionPhase.AFTER_COMMIT,
                    TransactionPhase.AFTER_ROLLBACK,
                    TransactionPhase.AFTER_COMPLETION);
    private static final Set<Propagation> SAFE_PROPAGATIONS =
            EnumSet.of(Propagation.REQUIRES_NEW, Propagation.NOT_SUPPORTED);

    @Test
    void afterPhaseListenersMustNotJoinTheCompletedTransaction() throws Exception {
        List<String> afterPhaseListeners = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        for (Class<?> candidate : loadApplicationClasses()) {
            for (Method method : candidate.getDeclaredMethods()) {
                TransactionalEventListener listener =
                        AnnotatedElementUtils.findMergedAnnotation(
                                method, TransactionalEventListener.class);
                if (listener == null || !AFTER_PHASES.contains(listener.phase())) {
                    continue;
                }
                afterPhaseListeners.add(candidate.getName() + "#" + method.getName());
                Transactional transactional =
                        AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
                if (transactional == null
                        || !SAFE_PROPAGATIONS.contains(transactional.propagation())) {
                    violations.add(candidate.getName() + "#" + method.getName());
                }
            }
        }
        assertThat(afterPhaseListeners)
                .withFailMessage(
                        "Expected to find at least one after-phase @TransactionalEventListener; "
                                + "the classpath scan appears to be broken.")
                .isNotEmpty();
        assertThat(violations)
                .withFailMessage(
                        """
                    After-phase @TransactionalEventListener methods must declare \
                    @Transactional(propagation = REQUIRES_NEW) (or NOT_SUPPORTED), otherwise any \
                    persistence they trigger silently joins the already-committed transaction and is \
                    never written. Use @AfterCommitEventListener or @AfterCommitCallback. \
                    Offending methods: %s""",
                        violations)
                .isEmpty();
    }

    private List<Class<?>> loadApplicationClasses() throws Exception {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
        Resource[] resources =
                resolver.getResources(
                        "classpath*:" + BASE_PACKAGE.replace('.', '/') + "/**/*.class");
        List<Class<?>> classes = new ArrayList<>();
        for (Resource resource : resources) {
            String className =
                    metadataReaderFactory
                            .getMetadataReader(resource)
                            .getClassMetadata()
                            .getClassName();
            try {
                classes.add(Class.forName(className, false, getClass().getClassLoader()));
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                // Skip classes whose optional dependencies are absent on the test classpath.
            }
        }
        assertThat(classes).isNotEmpty();
        return classes;
    }
}
