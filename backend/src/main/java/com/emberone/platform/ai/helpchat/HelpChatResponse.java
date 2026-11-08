/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.ai.helpchat;

import java.util.List;

/** AI answer text (shown as such in the UI) plus validated start actions. */
public record HelpChatResponse(String text, List<HelpChatActionDto> actions) {}
