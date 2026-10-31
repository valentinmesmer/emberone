/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.notification.channel.email;

import java.util.Map;

public record EmailRenderResult(
        String subject, String templateName, Map<String, Object> variables) {}
