/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.repository;

import com.emberone.onboarding.domain.OnboardingInvitationStepData;
import com.emberone.onboarding.domain.OnboardingStepType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface OnboardingInvitationStepDataRepository
        extends JpaRepository<OnboardingInvitationStepData, Long> {

    List<OnboardingInvitationStepData> findByInvitationId(Long invitationId);

    Optional<OnboardingInvitationStepData> findByInvitationIdAndStepType(
            Long invitationId, OnboardingStepType stepType);
}
