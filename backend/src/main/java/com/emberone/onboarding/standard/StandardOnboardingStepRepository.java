/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.standard;

import com.emberone.onboarding.domain.OnboardingStepType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface StandardOnboardingStepRepository
        extends JpaRepository<StandardOnboardingStep, Long> {

    List<StandardOnboardingStep> findByStandardIdOrderBySortOrderAscIdAsc(Long standardId);

    List<StandardOnboardingStep> findByStepType(OnboardingStepType stepType);

    boolean existsByStandardIdAndStepType(Long standardId, OnboardingStepType stepType);

    @Transactional
    void deleteByStandardId(Long standardId);
}
