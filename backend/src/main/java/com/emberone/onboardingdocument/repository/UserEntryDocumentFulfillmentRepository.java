/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.repository;

import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserEntryDocumentFulfillmentRepository
        extends JpaRepository<UserEntryDocumentFulfillment, Long> {

    Optional<UserEntryDocumentFulfillment> findByUserIdAndConfigKeyAndOrganizationIdIsNull(
            Long userId, String configKey);

    Optional<UserEntryDocumentFulfillment> findByOrganizationIdAndUserIdAndConfigKey(
            Long organizationId, Long userId, String configKey);

    List<UserEntryDocumentFulfillment> findByUserId(Long userId);

    List<UserEntryDocumentFulfillment> findByUserIdAndConfigKeyInAndOrganizationIdIsNull(
            Long userId, Collection<String> configKeys);

    List<UserEntryDocumentFulfillment> findByOrganizationIdAndUserIdAndConfigKeyIn(
            Long organizationId, Long userId, Collection<String> configKeys);

    boolean existsByTemplateSha256Hex(String templateSha256Hex);

    boolean existsByConfigKeyAndOrganizationIdIsNull(String configKey);

    boolean existsByOrganizationIdAndConfigKey(long organizationId, String configKey);
}
