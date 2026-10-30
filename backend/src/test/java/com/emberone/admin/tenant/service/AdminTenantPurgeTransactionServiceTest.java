/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminTenantPurgeTransactionServiceTest {

    @Mock AdminTenantPurgeBatchService purgeBatchService;

    @InjectMocks AdminTenantPurgeTransactionService purgeTransactionService;

    @Test
    void purgeTenantDataDeletesAllTablesInBatches() {
        when(purgeBatchService.deleteBatch(eq(7L), anyString())).thenReturn(1, 0);

        purgeTransactionService.purgeTenantData(7L);

        verify(purgeBatchService, atLeastOnce()).deleteBatch(7L, "action_log");
    }
}
