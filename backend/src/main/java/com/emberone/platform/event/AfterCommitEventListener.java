/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.event;

import java.lang.annotation.*;
import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener that runs after the publishing transaction has committed, inside its own new
 * transaction.
 *
 * <p>Always use this instead of a bare {@code @TransactionalEventListener(phase = AFTER_COMMIT)}:
 * in the AFTER_COMMIT phase the original (already completed) transaction is still bound to the
 * thread, so any {@code @Transactional(REQUIRED)} work invoked from the listener joins it and is
 * silently never committed - repository saves appear to succeed but nothing is persisted. {@code
 * REQUIRES_NEW} gives the listener a live transaction of its own.
 *
 * <p>For listeners that only push realtime or evict caches (no DB), use {@link AfterCommitCallback}
 * instead to avoid a second pool connection.
 *
 * <p>Enforced by {@code TransactionalEventListenerArchitectureTest}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.REQUIRES_NEW)
public @interface AfterCommitEventListener {

    /**
     * Marks the listener transaction read-only; use for listeners that only read state and push
     * realtime updates.
     */
    @AliasFor(annotation = Transactional.class, attribute = "readOnly")
    boolean readOnly() default false;
}
