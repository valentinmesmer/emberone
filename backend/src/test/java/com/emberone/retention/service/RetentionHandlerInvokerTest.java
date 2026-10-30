/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.retention.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.platform.security.TenantContextHolder;
import com.emberone.retention.RetentionPolicyService;
import com.emberone.retention.TestRetentionProfileFactory;
import com.emberone.retention.domain.RetentionCategory;
import com.emberone.retention.handler.NoOpRetentionHandler;
import com.emberone.retention.repository.RetentionRunRepository;
import com.emberone.retention.spi.RetentionHandler;
import jakarta.persistence.EntityManager;
import java.util.Set;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetentionHandlerInvokerTest {

    @Mock RetentionPolicyService retentionPolicyService;
    @Mock RetentionRunRepository retentionRunRepository;
    @Mock EntityManager entityManager;
    @Mock Session session;

    RetentionHandlerInvoker retentionHandlerInvoker;

    @BeforeEach
    void setTenant() {
        // Session extends EntityManager, so @InjectMocks could inject either mock.
        retentionHandlerInvoker =
                new RetentionHandlerInvoker(
                        retentionPolicyService, retentionRunRepository, entityManager);
        TenantContextHolder.setTenantId(9L);
        when(retentionPolicyService.profileForTenant(9L))
                .thenReturn(TestRetentionProfileFactory.minimalValidProfile());
        when(entityManager.unwrap(Session.class)).thenReturn(session);
    }

    @AfterEach
    void clearTenant() {
        TenantContextHolder.clear();
    }

    @Test
    void doesNotPersistRunWhenNothingDeleted() {
        RetentionHandler handler =
                new NoOpRetentionHandler(RetentionCategory.TODO, Set.of("todo_items"));

        int deleted = retentionHandlerInvoker.invoke(handler);

        assertThat(deleted).isZero();
        verify(retentionRunRepository, never()).save(any());
    }
}
