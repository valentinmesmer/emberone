/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.platform.cache.value;

import com.emberone.module.domain.Module;
import java.util.Set;

public record EnabledModulesCacheValue(Set<Module> modules) {}
