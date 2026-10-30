/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Guards the repository transaction boundary that Postgres FORCE RLS depends on.
 *
 * <p>The tenant isolation GUC {@code app.tenant_id} is only set at the beginning of a transaction
 * (transaction-local {@code set_config}). Query methods declared on our repository interfaces get
 * no transaction of their own unless one is declared, so when they are called outside a
 * transactional bean (for example from security checks or event listeners) they silently run
 * without the GUC and RLS returns zero rows. Declaring the transaction on the repository itself
 * closes that gap:
 *
 * <ul>
 *   <li>every repository interface is {@code @Transactional(readOnly = true)} by default, and
 *   <li>every write method (see {@link #isWriteMethod(Method)}) must resolve to a writable
 *       {@code @Transactional}, either on the method itself or, failing that, on the interface.
 *       Lock methods may use {@code Propagation.MANDATORY} because they must join the caller's
 *       transaction; the readOnly flag is what is checked.
 * </ul>
 */
class RepositoryTransactionArchitectureTest {

    private static final String BASE_PACKAGE = "com.emberone";

    private static final Pattern DERIVED_DELETE = Pattern.compile("^(delete|remove)\\w*");

    private static final Pattern WRITE_QUERY =
            Pattern.compile("(?i)\\b(update|delete|insert)\\b|for\\s+update");

    @Test
    void everyRepositoryInterfaceIsReadOnlyTransactionalByDefault() throws Exception {
        List<String> repositories = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        for (Class<?> candidate : loadApplicationClasses()) {
            if (!candidate.isInterface()
                    || !Repository.class.isAssignableFrom(candidate)
                    || candidate.isAnnotationPresent(NoRepositoryBean.class)) {
                continue;
            }
            repositories.add(candidate.getName());
            Transactional transactional =
                    AnnotatedElementUtils.findMergedAnnotation(candidate, Transactional.class);
            if (transactional == null || !transactional.readOnly()) {
                violations.add(candidate.getName());
            }
        }
        assertThat(repositories)
                .withFailMessage(
                        "Expected to find at least one repository interface; the classpath scan"
                                + " appears to be broken.")
                .isNotEmpty();
        assertThat(violations)
                .withFailMessage(
                        """
                    Repository interfaces must be annotated with @Transactional(readOnly = true) \
                    so that query methods run inside a transaction and the RLS tenant GUC \
                    (app.tenant_id) is applied even when called outside a transactional bean. \
                    Offending interfaces: %s""",
                        violations)
                .isEmpty();
    }

    @Test
    void repositoryWriteMethodsRunInWritableTransaction() throws Exception {
        List<String> writeMethods = new ArrayList<>();
        List<String> violations = new ArrayList<>();
        for (Class<?> candidate : loadApplicationClasses()) {
            if (!candidate.isInterface() || !Repository.class.isAssignableFrom(candidate)) {
                continue;
            }
            for (Method method : candidate.getDeclaredMethods()) {
                if (!isWriteMethod(method)) {
                    continue;
                }
                String id = candidate.getName() + "#" + method.getName();
                writeMethods.add(id);
                Transactional transactional = effectiveTransactional(candidate, method);
                if (transactional == null || transactional.readOnly()) {
                    violations.add(id);
                }
            }
        }
        assertThat(writeMethods)
                .withFailMessage(
                        "Expected to find at least one repository write method; the classpath"
                                + " scan appears to be broken.")
                .isNotEmpty();
        assertThat(violations)
                .withFailMessage(
                        """
                    Repository write methods (@Modifying, derived delete*/remove*, @Lock, and \
                    write @Query) must declare a writable method-level @Transactional. \
                    JPQL/native @Modifying queries run in the transaction they are called in and \
                    fail there when it is read-only. Entity flushes (derived deletes, managed \
                    entity changes) are silently skipped in a read-only transaction, so no error \
                    is raised. Offending methods: %s""",
                        violations)
                .isEmpty();
    }

    private static boolean isWriteMethod(Method method) {
        if (AnnotatedElementUtils.hasAnnotation(method, Modifying.class)) {
            return true;
        }
        if (DERIVED_DELETE.matcher(method.getName()).lookingAt()) {
            return true;
        }
        if (AnnotatedElementUtils.hasAnnotation(method, Lock.class)) {
            return true;
        }
        Query query = AnnotatedElementUtils.findMergedAnnotation(method, Query.class);
        return query != null && WRITE_QUERY.matcher(query.value()).find();
    }

    /** The method-level annotation wins; otherwise the interface-level one applies. */
    private static Transactional effectiveTransactional(Class<?> repository, Method method) {
        Transactional methodLevel =
                AnnotatedElementUtils.findMergedAnnotation(method, Transactional.class);
        if (methodLevel != null) {
            return methodLevel;
        }
        return AnnotatedElementUtils.findMergedAnnotation(repository, Transactional.class);
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
