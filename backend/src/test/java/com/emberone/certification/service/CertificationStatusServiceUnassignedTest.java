/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.emberone.certification.domain.CertificationStatus;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.dto.UserCertificationStatusDto;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.repository.WorkflowRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class CertificationStatusServiceUnassignedTest {

    private static final Long USER_ID = 42L;
    private static final Long DEFINITION_ID = 7L;

    @Mock UserCertificationAssignmentRepository assignmentRepository;
    @Mock TenantCertDefinitionRepository tenantDefinitionRepository;
    @Mock UserCertificationRecordRepository recordRepository;
    @Mock WorkflowRepository workflowRepository;
    @Mock JsonMapper jsonMapper;
    @Mock UserOrganizationMembershipRepository membershipRepository;
    @Mock TenantCertDefinitionEvidenceTypeService evidenceTypeService;

    @InjectMocks CertificationStatusService statusService;

    @Test
    void keepsHistoricalRecordsVisibleAfterAssignmentRemoved() {
        TenantCertDefinition definition =
                TenantCertDefinition.builder()
                        .id(DEFINITION_ID)
                        .tenantId(1L)
                        .name("G.26.3")
                        .archived(false)
                        .active(true)
                        .reminderDaysBeforeExpiry(30)
                        .build();
        UserCertificationRecord record =
                UserCertificationRecord.builder()
                        .id(99L)
                        .userId(USER_ID)
                        .tenantCertDefinitionId(DEFINITION_ID)
                        .completedDate(LocalDate.of(2024, 6, 1))
                        .expiryDate(LocalDate.of(2027, 6, 1))
                        .build();

        when(assignmentRepository.findByUserId(USER_ID)).thenReturn(List.of());
        when(recordRepository.findByUserId(USER_ID)).thenReturn(List.of(record));
        when(tenantDefinitionRepository.findById(DEFINITION_ID))
                .thenReturn(Optional.of(definition));
        when(recordRepository.findByUserIdAndTenantCertDefinitionId(USER_ID, DEFINITION_ID))
                .thenReturn(List.of(record));
        when(workflowRepository.findOpenedByUser(USER_ID)).thenReturn(List.of());
        when(evidenceTypeService.getEvidenceTypes(DEFINITION_ID)).thenReturn(Set.of());

        UserCertificationStatusDto status = statusService.getStatusForUser(USER_ID);

        assertThat(status.getItems()).hasSize(1);
        assertThat(status.getItems().getFirst().getStatus())
                .isEqualTo(CertificationStatus.VALID.name());
        assertThat(status.getItems().getFirst().isAssigned()).isFalse();
        assertThat(status.getItems().getFirst().isArchived()).isFalse();
        assertThat(status.getItems().getFirst().getRecordId()).isEqualTo(99L);
        assertThat(status.getMissingCount()).isZero();
    }
}
