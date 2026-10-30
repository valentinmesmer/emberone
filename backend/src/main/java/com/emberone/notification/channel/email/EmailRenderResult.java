/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.channel.email;

import java.util.Map;

public record EmailRenderResult(
        String subject, String templateName, Map<String, Object> variables) {}
