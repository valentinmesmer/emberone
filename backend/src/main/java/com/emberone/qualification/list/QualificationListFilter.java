/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.list;

import com.emberone.qualification.domain.QualificationExerciseStatus;
import org.springframework.web.bind.annotation.RequestParam;

public record QualificationListFilter(
        @RequestParam(required = false) QualificationExerciseStatus status) {}
