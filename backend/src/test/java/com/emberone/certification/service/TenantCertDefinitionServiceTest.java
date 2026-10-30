/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.certification.event.CertificationEventGrantRepository;
import com.emberone.certification.event.TenantCertDefinitionDeletedEvent;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.standard.CertDefinitionSelectionRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.qualification.tenant.TenantQualificationCertDefinitionRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class TenantCertDefinitionServiceTest {

    private static final long TENANT_ID = 1L;
    private static final String CONFIG_KEY = "ASB";
    private static final long DEFINITION_ID = 10L;

    @Mock TenantCertDefinitionRepository tenantDefinitionRepository;
    @Mock CertDefinitionSelectionRepository selectionRepository;
    @Mock UserCertificationAssignmentRepository assignmentRepository;
    @Mock UserCertificationRecordRepository recordRepository;
    @Mock CertificationEventGrantRepository eventGrantRepository;
    @Mock TenantQualificationCertDefinitionRepository qualificationCertDefinitionRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TenantCertDefinitionEvidenceTypeService evidenceTypeService;

    @InjectMocks TenantCertDefinitionService service;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void deleteThrowsWhenEventGrantsExist() {
        TenantCertDefinition definition = definition();
        when(tenantDefinitionRepository.findByConfigKey(CONFIG_KEY))
                .thenReturn(Optional.of(definition));
        when(recordRepository.existsByTenantCertDefinitionIdIn(anyList())).thenReturn(false);
        when(assignmentRepository.existsByTenantCertDefinitionIdIn(anyList())).thenReturn(false);
        when(eventGrantRepository.existsByTenantCertDefinitionId(eq(DEFINITION_ID)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.delete(TENANT_ID, CONFIG_KEY))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cannot be deleted");

        verify(tenantDefinitionRepository, never()).delete(definition);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteSucceedsWhenNoRecordsAssignmentsOrGrants() {
        TenantCertDefinition definition = definition();
        when(tenantDefinitionRepository.findByConfigKey(CONFIG_KEY))
                .thenReturn(Optional.of(definition));
        when(recordRepository.existsByTenantCertDefinitionIdIn(anyList())).thenReturn(false);
        when(assignmentRepository.existsByTenantCertDefinitionIdIn(anyList())).thenReturn(false);
        when(eventGrantRepository.existsByTenantCertDefinitionId(eq(DEFINITION_ID)))
                .thenReturn(false);

        service.delete(TENANT_ID, CONFIG_KEY);

        verify(selectionRepository).deleteByDefinitionConfigKey(CONFIG_KEY);
        verify(eventPublisher)
                .publishEvent(new TenantCertDefinitionDeletedEvent(TENANT_ID, CONFIG_KEY));
        verify(tenantDefinitionRepository).delete(definition);
    }

    private static TenantCertDefinition definition() {
        return TenantCertDefinition.builder()
                .id(DEFINITION_ID)
                .tenantId(TENANT_ID)
                .configKey(CONFIG_KEY)
                .name("Atemschutz")
                .build();
    }
}
