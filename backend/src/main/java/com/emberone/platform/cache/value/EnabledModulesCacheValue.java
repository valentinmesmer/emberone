/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.cache.value;

import com.emberone.module.domain.Module;
import java.util.Set;

public record EnabledModulesCacheValue(Set<Module> modules) {}
