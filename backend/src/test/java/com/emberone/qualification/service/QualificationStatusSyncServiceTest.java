/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.qualification.domain.QualificationExerciseStatus;
import com.emberone.qualification.domain.QualificationOrgDecisionStatus;
import com.emberone.qualification.domain.QualificationRevokeReason;
import com.emberone.qualification.domain.UserQualificationOrgDecision;
import com.emberone.qualification.dto.QualificationStatusItemDto;
import com.emberone.qualification.dto.UserQualificationStatusDto;
import com.emberone.qualification.repository.UserQualificationOrgDecisionRepository;
import com.emberone.qualification.tenant.TenantQualificationDefinitionRepository;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QualificationStatusSyncServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long USER_ID = 42L;
    private static final long ORG_ID = 10L;
    private static final long DEFINITION_ID = 100L;

    @Mock QualificationStatusService statusService;
    @Mock QualificationCatalogService catalogService;
    @Mock QualificationOrgDecisionService orgDecisionService;
    @Mock TenantQualificationDefinitionRepository definitionRepository;
    @Mock UserQualificationOrgDecisionRepository orgDecisionRepository;
    @Mock UserOrganizationMembershipRepository membershipRepository;

    private QualificationStatusSyncService syncService;

    private static UserQualificationStatusDto status(QualificationStatusItemDto... items) {
        return UserQualificationStatusDto.builder()
                .userId(USER_ID)
                .organizationId(ORG_ID)
                .items(List.of(items))
                .build();
    }

    private static QualificationStatusItemDto item(
            long definitionId,
            QualificationExerciseStatus status,
            boolean prerequisitesMet,
            boolean orgDecisionGranted) {
        return QualificationStatusItemDto.builder()
                .tenantQualificationDefinitionId(definitionId)
                .configKey("Q-" + definitionId)
                .abbreviation("Q-" + definitionId)
                .name("Qualification " + definitionId)
                .status(status)
                .prerequisitesMet(prerequisitesMet)
                .trainingCoursesMet(prerequisitesMet)
                .certDefinitionsMet(prerequisitesMet)
                .requiresOrgDecision(true)
                .orgDecisionGranted(orgDecisionGranted)
                .orgDecisions(List.of())
                .trainingCourseDetails(List.of())
                .certDefinitionDetails(List.of())
                .build();
    }

    @BeforeEach
    void setUp() {
        syncService =
                new QualificationStatusSyncService(
                        statusService,
                        catalogService,
                        orgDecisionService,
                        definitionRepository,
                        orgDecisionRepository,
                        membershipRepository);
    }

    @Test
    void syncForUserInOrganization_autoRevokesWhenPrerequisitesLost() {
        QualificationStatusItemDto statusItem =
                item(DEFINITION_ID, QualificationExerciseStatus.PREREQUISITES_MISSING, false, true);
        when(statusService.getStatusForUser(ORG_ID, USER_ID)).thenReturn(status(statusItem));

        syncService.syncForUserInOrganization(TENANT_ID, USER_ID, ORG_ID);

        verify(orgDecisionService)
                .autoRevoke(
                        TENANT_ID,
                        USER_ID,
                        DEFINITION_ID,
                        ORG_ID,
                        QualificationRevokeReason.PREREQUISITES_LOST);
    }

    @Test
    void syncForCatalogChange_revokesGrantedDecisionsRemovedFromCatalog() {
        when(catalogService.resolveCatalogForOrganization(ORG_ID)).thenReturn(List.of());
        when(orgDecisionRepository.findByOrganizationId(ORG_ID))
                .thenReturn(
                        List.of(
                                UserQualificationOrgDecision.builder()
                                        .tenantId(TENANT_ID)
                                        .userId(USER_ID)
                                        .tenantQualificationDefinitionId(DEFINITION_ID)
                                        .organizationId(ORG_ID)
                                        .status(QualificationOrgDecisionStatus.GRANTED)
                                        .startDate(LocalDate.now())
                                        .build()));

        syncService.syncForCatalogChange(TENANT_ID, List.of(ORG_ID));

        verify(orgDecisionService)
                .autoRevoke(
                        TENANT_ID,
                        USER_ID,
                        DEFINITION_ID,
                        ORG_ID,
                        QualificationRevokeReason.REMOVED_FROM_CATALOG);
    }
}
