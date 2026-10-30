/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

import com.emberone.event.domain.EventType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface CertificationEventGrantRepository
        extends JpaRepository<CertificationEventGrant, Long> {

    List<CertificationEventGrant> findByEventTypeAndEventId(EventType eventType, Long eventId);

    Optional<CertificationEventGrant> findByEventTypeAndEventIdAndTenantCertDefinitionId(
            EventType eventType, Long eventId, Long tenantCertDefinitionId);

    boolean existsByTenantCertDefinitionId(Long tenantCertDefinitionId);
}
