/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.event;

import java.lang.annotation.*;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * After-commit hook that does not open a database transaction.
 *
 * <p>Use for listeners that only push realtime updates or evict caches. {@link
 * AfterCommitEventListener} opens {@code REQUIRES_NEW} and holds a second pool connection while the
 * publishing thread may still hold the first; prefer this annotation when no DB work is required.
 *
 * <p>Enforced by {@code TransactionalEventListenerArchitectureTest}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public @interface AfterCommitCallback {}
