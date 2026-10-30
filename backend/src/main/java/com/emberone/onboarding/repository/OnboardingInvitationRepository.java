/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.repository;

import com.emberone.onboarding.domain.OnboardingInvitation;
import com.emberone.onboarding.domain.OnboardingInvitationStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface OnboardingInvitationRepository
        extends JpaRepository<OnboardingInvitation, Long>,
                JpaSpecificationExecutor<OnboardingInvitation> {

    List<OnboardingInvitation> findByTargetOrganizationIdAndStatusIn(
            Long targetOrganizationId, Collection<OnboardingInvitationStatus> statuses);

    List<OnboardingInvitation> findByUserIdAndStatusIn(
            Long userId, Collection<OnboardingInvitationStatus> statuses);

    Optional<OnboardingInvitation> findByIdAndUserId(Long id, Long userId);

    Optional<OnboardingInvitation> findByIdAndTargetOrganizationId(
            Long id, Long targetOrganizationId);

    List<OnboardingInvitation> findByUserId(Long userId);

    Optional<OnboardingInvitation> findByUserIdAndTargetOrganizationId(
            Long userId, Long targetOrganizationId);

    boolean existsByUserIdAndTargetOrganizationIdAndStatusIn(
            Long userId,
            Long targetOrganizationId,
            Collection<OnboardingInvitationStatus> statuses);

    Optional<OnboardingInvitation> findFirstByUserIdAndStatusOrderByIdAsc(
            Long userId, OnboardingInvitationStatus status);
}
