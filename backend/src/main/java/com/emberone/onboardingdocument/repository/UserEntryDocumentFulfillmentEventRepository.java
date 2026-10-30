/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.repository;

import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillmentEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public interface UserEntryDocumentFulfillmentEventRepository
        extends JpaRepository<UserEntryDocumentFulfillmentEvent, Long> {

    List<UserEntryDocumentFulfillmentEvent> findByUserIdOrderByConfirmedAtAscIdAsc(long userId);

    boolean existsByTemplateSha256Hex(String templateSha256Hex);

    boolean existsByConfigKeyAndOrganizationIdIsNull(String configKey);

    boolean existsByOrganizationIdAndConfigKey(long organizationId, String configKey);
}
