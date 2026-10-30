/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.emberone.certification.domain.CertificationStatus;
import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.dto.UserCertificationStatusDto;
import com.emberone.certification.event.TenantCertDefinitionArchivedEvent;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.platform.navigation.domain.AppLinkType;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.domain.TodoSourceEntityType;
import com.emberone.todo.domain.TodoType;
import com.emberone.todo.service.TodoOpenRequest;
import com.emberone.todo.service.TodoService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CertificationReminderServiceTest {

    private static final long TENANT_ID = 1L;
    private static final long USER_ID = 42L;
    private static final long DEFINITION_ID = 100L;

    @Mock UserCertificationAssignmentRepository assignmentRepository;
    @Mock CertificationStatusService statusService;
    @Mock TodoService todoService;
    @Mock EntityManager entityManager;

    private CertificationReminderService service;

    private static CertificationStatusItemDto statusItem(CertificationStatus status) {
        return statusItem(status, true, false);
    }

    private static CertificationStatusItemDto statusItem(
            CertificationStatus status, boolean assigned, boolean archived) {
        return CertificationStatusItemDto.builder()
                .tenantCertDefinitionId(DEFINITION_ID)
                .certDefinitionName("Erste Hilfe")
                .status(status.name())
                .expiryDate(
                        status == CertificationStatus.MISSING ? null : LocalDate.now().minusDays(1))
                .assigned(assigned)
                .archived(archived)
                .build();
    }

    @BeforeEach
    void setUp() {
        service =
                new CertificationReminderService(
                        assignmentRepository, statusService, todoService, entityManager);
    }

    private void stubAssignedUsers(Long... userIds) {
        when(assignmentRepository.findDistinctUserIds(
                        org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(userIds)));
    }

    @Test
    void createsReminderForMissingExpiredAndExpiringSoon() {
        stubAssignedUsers(USER_ID);
        when(statusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .userId(USER_ID)
                                .items(
                                        List.of(
                                                statusItem(CertificationStatus.MISSING),
                                                statusItem(CertificationStatus.EXPIRED),
                                                statusItem(CertificationStatus.EXPIRING_SOON),
                                                statusItem(CertificationStatus.VALID)))
                                .build());

        int created = service.createRemindersForTenant(TENANT_ID);

        assertThat(created).isEqualTo(3);
        verify(todoService, times(3)).open(any());
    }

    @Test
    void opensTodoWithDefinitionLinkAndStatusParams() {
        stubAssignedUsers(USER_ID);
        when(statusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .userId(USER_ID)
                                .items(List.of(statusItem(CertificationStatus.MISSING)))
                                .build());

        service.createRemindersForTenant(TENANT_ID);

        ArgumentCaptor<TodoOpenRequest> captor = ArgumentCaptor.forClass(TodoOpenRequest.class);
        verify(todoService).open(captor.capture());
        TodoOpenRequest request = captor.getValue();
        assertThat(request.tenantId()).isEqualTo(TENANT_ID);
        assertThat(request.type()).isEqualTo(TodoType.CERTIFICATION_EXPIRING);
        assertThat(request.assigneeUserId()).isEqualTo(USER_ID);
        assertThat(request.sourceEntityType()).isEqualTo(TodoSourceEntityType.CERTIFICATION_RECORD);
        assertThat(request.sourceEntityId()).isEqualTo(DEFINITION_ID);
        assertThat(request.link().type()).isEqualTo(AppLinkType.CERTIFICATION_RECORD);
        assertThat(request.link().linkId()).isEqualTo(DEFINITION_ID);
        assertThat(request.params().get(TodoParamKeys.STATUS))
                .isEqualTo(CertificationStatus.MISSING.name());
        assertThat(request.params().get(TodoParamKeys.CERT_DEFINITION_NAME))
                .isEqualTo("Erste Hilfe");
    }

    @Test
    void skipsUsersWithoutActionableRequirements() {
        stubAssignedUsers(USER_ID);
        when(statusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .userId(USER_ID)
                                .items(List.of(statusItem(CertificationStatus.VALID)))
                                .build());

        int created = service.createRemindersForTenant(TENANT_ID);

        assertThat(created).isZero();
        verifyNoInteractions(todoService);
    }

    @Test
    void skipsHistoricalItemsEvenWhenExpired() {
        stubAssignedUsers(USER_ID);
        when(statusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .userId(USER_ID)
                                .items(
                                        List.of(
                                                statusItem(
                                                        CertificationStatus.EXPIRED, false, false)))
                                .build());

        int created = service.createRemindersForTenant(TENANT_ID);

        assertThat(created).isZero();
        verifyNoInteractions(todoService);
    }

    @Test
    void syncRemindersForUser_opensTodoForActionableStatus() {
        when(statusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .userId(USER_ID)
                                .items(List.of(statusItem(CertificationStatus.MISSING)))
                                .build());

        service.syncRemindersForUser(TENANT_ID, USER_ID);

        verify(todoService).open(any());
        verify(todoService, never())
                .cancelOpenBySourceParticipant(any(), any(), any(), any(), any());
        verify(todoService)
                .cancelOpenByAssigneeTypeAndSourceExcept(
                        eq(USER_ID),
                        eq(TodoType.CERTIFICATION_EXPIRING),
                        eq(TodoSourceEntityType.CERTIFICATION_RECORD),
                        eq(Set.of(DEFINITION_ID)),
                        eq(null));
    }

    @Test
    void syncRemindersForUser_cancelsTodoForNonActionableStatus() {
        when(statusService.getStatusForUser(USER_ID))
                .thenReturn(
                        UserCertificationStatusDto.builder()
                                .userId(USER_ID)
                                .items(List.of(statusItem(CertificationStatus.VALID)))
                                .build());

        service.syncRemindersForUser(TENANT_ID, USER_ID);

        verify(todoService, never()).open(any());
        verify(todoService)
                .cancelOpenBySourceParticipant(
                        eq(TodoSourceEntityType.CERTIFICATION_RECORD),
                        eq(DEFINITION_ID),
                        eq(TodoType.CERTIFICATION_EXPIRING),
                        eq(USER_ID),
                        eq(null));
        verify(todoService)
                .cancelOpenByAssigneeTypeAndSourceExcept(
                        eq(USER_ID),
                        eq(TodoType.CERTIFICATION_EXPIRING),
                        eq(TodoSourceEntityType.CERTIFICATION_RECORD),
                        eq(Set.of()),
                        eq(null));
    }

    @Test
    void onTenantCertDefinitionArchivedCancelsTodosBySource() {
        service.onTenantCertDefinitionArchived(
                new TenantCertDefinitionArchivedEvent(TENANT_ID, DEFINITION_ID));

        verify(todoService)
                .cancelBySource(TodoSourceEntityType.CERTIFICATION_RECORD, DEFINITION_ID, null);
    }
}
