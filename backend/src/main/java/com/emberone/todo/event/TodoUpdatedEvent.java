/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.todo.event;

/**
 * Published after a to-do has been archived, reopened, or otherwise updated for subscribers that
 * push realtime updates.
 */
public record TodoUpdatedEvent(Long todoId) {}
