/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.event;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface CertificationEventGrantRecipientRepository
        extends JpaRepository<CertificationEventGrantRecipient, Long> {

    List<CertificationEventGrantRecipient> findByGrantId(Long grantId);

    @Transactional
    void deleteByGrantIdAndUserId(Long grantId, Long userId);
}
