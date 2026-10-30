/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.RetentionRegistry;
import com.emberone.retention.spi.RetentionHandler;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetentionExecutionServiceTest {

    @Mock RetentionRegistry retentionRegistry;
    @Mock RetentionHandlerInvoker retentionHandlerInvoker;
    @Mock RetentionHandler firstHandler;
    @Mock RetentionHandler secondHandler;

    @InjectMocks RetentionExecutionService retentionExecutionService;

    @BeforeEach
    void setTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void continuesWhenHandlerFails() {
        when(retentionRegistry.all()).thenReturn(List.of(firstHandler, secondHandler));
        doThrow(new RuntimeException("boom")).when(retentionHandlerInvoker).invoke(firstHandler);
        when(retentionHandlerInvoker.invoke(secondHandler)).thenReturn(0);

        retentionExecutionService.runForCurrentTenant();

        verify(retentionHandlerInvoker).invoke(secondHandler);
    }

    @Test
    void invokesEveryHandler() {
        when(retentionRegistry.all()).thenReturn(List.of(firstHandler, secondHandler));
        when(retentionHandlerInvoker.invoke(any())).thenReturn(0);

        retentionExecutionService.runForCurrentTenant();

        verify(retentionHandlerInvoker).invoke(firstHandler);
        verify(retentionHandlerInvoker).invoke(secondHandler);
    }
}
