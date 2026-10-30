/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.event;

/**
 * Published after a to-do has been archived, reopened, or otherwise updated for subscribers that
 * push realtime updates.
 */
public record TodoUpdatedEvent(Long todoId) {}
