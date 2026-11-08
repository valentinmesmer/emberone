/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

/** Organization the user can pick for a help chat link (never sent to the model). */
public record HelpChatOrganizationDto(long id, String name) {}
