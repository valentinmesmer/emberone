/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.user.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPersonalNumberService {

    private final EntityManager entityManager;

    @Transactional
    public long allocateNext(Long tenantId) {
        Number allocated =
                (Number)
                        entityManager
                                .createNativeQuery(
                                        """
                INSERT INTO tenant_personal_number_sequences (tenant_id, next_personal_number)
                VALUES (:tenantId, 2)
                ON CONFLICT (tenant_id) DO UPDATE
                SET next_personal_number = tenant_personal_number_sequences.next_personal_number + 1
                RETURNING next_personal_number - 1
                """)
                                .setParameter("tenantId", tenantId)
                                .getSingleResult();
        return allocated.longValue();
    }
}
