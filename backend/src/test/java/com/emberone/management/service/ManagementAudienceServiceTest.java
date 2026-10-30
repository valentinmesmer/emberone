/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.management.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.management.domain.ManagementAudience;
import com.emberone.management.domain.ManagementAudienceKind;
import com.emberone.management.dto.ManagementAudienceEntryRequest;
import com.emberone.management.dto.ManagementAudienceSettingsDto;
import com.emberone.management.exception.ManagementAudienceValidationException;
import com.emberone.management.repository.ManagementAudienceRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.repository.OrganizationTypeDefinitionRepository;
import com.emberone.organization.service.OrganizationTypeDefinitionService;
import com.emberone.permission.domain.PermissionType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManagementAudienceServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long MANAGING_ORG_ID = 10L;

    @Mock ManagementAudienceRepository audienceRepository;
    @Mock ManagementCapabilityService capabilityService;
    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationTypeDefinitionRepository organizationTypeRepository;
    @Mock OrganizationTypeDefinitionService organizationTypeService;

    @InjectMocks ManagementAudienceService service;

    private Organization managing;

    @BeforeEach
    void setUp() {
        managing =
                Organization.builder()
                        .id(MANAGING_ORG_ID)
                        .tenantId(TENANT_ID)
                        .name("Geschaeftsstelle")
                        .build();
        lenient()
                .when(organizationRepository.findById(MANAGING_ORG_ID))
                .thenReturn(Optional.of(managing));
        lenient().when(capabilityService.isCapable(managing)).thenReturn(true);
    }

    @Test
    void rejectsOrganizationWithoutCapability() {
        when(capabilityService.isCapable(managing)).thenReturn(false);

        assertThatThrownBy(
                        () -> service.replace(TENANT_ID, MANAGING_ORG_ID, List.of(tenantEntry(60))))
                .isInstanceOf(ManagementAudienceValidationException.class);

        verify(audienceRepository, never()).saveAll(any());
    }

    @Test
    void rejectsReferenceForTenantKind() {
        ManagementAudienceEntryRequest entry = tenantEntry(60);
        entry.setRefId(20L);

        assertThatThrownBy(() -> service.replace(TENANT_ID, MANAGING_ORG_ID, List.of(entry)))
                .isInstanceOf(ManagementAudienceValidationException.class)
                .hasMessageContaining("takes no reference");
    }

    @Test
    void rejectsMissingReferenceForOrganizationKind() {
        ManagementAudienceEntryRequest entry = entry(ManagementAudienceKind.ORG, null, 60);

        assertThatThrownBy(() -> service.replace(TENANT_ID, MANAGING_ORG_ID, List.of(entry)))
                .isInstanceOf(ManagementAudienceValidationException.class)
                .hasMessageContaining("needs a reference");
    }

    @Test
    void rejectsUnknownOrganizationReference() {
        activeOrganizations(managing);

        assertThatThrownBy(
                        () ->
                                service.replace(
                                        TENANT_ID,
                                        MANAGING_ORG_ID,
                                        List.of(entry(ManagementAudienceKind.ORG, 99L, 60))))
                .isInstanceOf(ManagementAudienceValidationException.class)
                .hasMessageContaining("unknown or inactive");
    }

    @Test
    void rejectsManagementAccessAsAudiencePermission() {
        ManagementAudienceEntryRequest request = entry(ManagementAudienceKind.ORG, 20L, 60);
        request.setPermissionTypes(List.of(PermissionType.MANAGEMENT_ACCESS));

        assertThatThrownBy(() -> service.replace(TENANT_ID, MANAGING_ORG_ID, List.of(request)))
                .isInstanceOf(ManagementAudienceValidationException.class)
                .hasMessageContaining("Management access");
    }

    @Test
    void rejectsDuplicateEntry() {
        activeOrganizations(managing, org(20L));
        ManagementAudienceEntryRequest first = entry(ManagementAudienceKind.ORG, 20L, 60);
        ManagementAudienceEntryRequest second = entry(ManagementAudienceKind.ORG, 20L, 90);

        assertThatThrownBy(
                        () -> service.replace(TENANT_ID, MANAGING_ORG_ID, List.of(first, second)))
                .isInstanceOf(ManagementAudienceValidationException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void keepsIdOfKeptEntryAndDeletesRemovedEntries() {
        ManagementAudience kept =
                ManagementAudience.builder()
                        .id(1L)
                        .tenantId(TENANT_ID)
                        .managingOrganizationId(MANAGING_ORG_ID)
                        .kind(ManagementAudienceKind.ORG)
                        .refId(20L)
                        .maxDurationMinutes(60)
                        .build();
        ManagementAudience removed =
                ManagementAudience.builder()
                        .id(2L)
                        .tenantId(TENANT_ID)
                        .managingOrganizationId(MANAGING_ORG_ID)
                        .kind(ManagementAudienceKind.TENANT)
                        .maxDurationMinutes(30)
                        .build();
        when(audienceRepository.findByManagingOrganizationIdOrderByIdAsc(MANAGING_ORG_ID))
                .thenReturn(List.of(kept, removed), List.of(kept));
        activeOrganizations(managing, org(20L));

        service.replace(
                TENANT_ID, MANAGING_ORG_ID, List.of(entry(ManagementAudienceKind.ORG, 20L, 120)));

        ArgumentCaptor<Iterable> deleted = ArgumentCaptor.forClass(Iterable.class);
        verify(audienceRepository).deleteAll(deleted.capture());
        assertThat(deleted.getValue()).containsExactly(removed);
        ArgumentCaptor<Iterable> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(audienceRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(kept);
        assertThat(kept.getMaxDurationMinutes()).isEqualTo(120);
    }

    @Test
    void settingsOfOrganizationOfAnotherTenantAreNotFound() {
        Organization foreign =
                Organization.builder().id(MANAGING_ORG_ID).tenantId(8L).name("Fremd").build();
        when(organizationRepository.findById(MANAGING_ORG_ID)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.getSettings(TENANT_ID, MANAGING_ORG_ID))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void settingsListEntriesOnly() {
        ManagementAudience entry =
                ManagementAudience.builder()
                        .id(1L)
                        .tenantId(TENANT_ID)
                        .managingOrganizationId(MANAGING_ORG_ID)
                        .kind(ManagementAudienceKind.TENANT)
                        .permissionTypes("USER_READ")
                        .maxDurationMinutes(45)
                        .build();
        when(audienceRepository.findByManagingOrganizationIdOrderByIdAsc(MANAGING_ORG_ID))
                .thenReturn(List.of(entry));
        ManagementAudienceSettingsDto settings = service.getSettings(TENANT_ID, MANAGING_ORG_ID);

        assertThat(settings.getEntries())
                .extracting(e -> e.getMaxDurationMinutes())
                .containsExactly(45);
    }

    private static ManagementAudienceEntryRequest tenantEntry(int maxDurationMinutes) {
        return entry(ManagementAudienceKind.TENANT, null, maxDurationMinutes);
    }

    private static ManagementAudienceEntryRequest entry(
            ManagementAudienceKind kind, Long refId, int maxDuration) {
        ManagementAudienceEntryRequest request = new ManagementAudienceEntryRequest();
        request.setKind(kind);
        request.setRefId(refId);
        request.setPermissionTypes(List.of(PermissionType.USER_READ));
        request.setMaxDurationMinutes(maxDuration);
        return request;
    }

    @Test
    void rejectsOrganizationReferenceToTheManagingOrganizationItself() {
        activeOrganizations(managing);

        assertThatThrownBy(
                        () ->
                                service.replace(
                                        TENANT_ID,
                                        MANAGING_ORG_ID,
                                        List.of(
                                                entry(
                                                        ManagementAudienceKind.ORG,
                                                        MANAGING_ORG_ID,
                                                        60))))
                .isInstanceOf(ManagementAudienceValidationException.class)
                .hasMessageContaining("cannot manage itself");
    }

    @Test
    void rejectsInactiveTargetOrganization() {
        activeOrganizations(managing);

        assertThatThrownBy(
                        () ->
                                service.replace(
                                        TENANT_ID,
                                        MANAGING_ORG_ID,
                                        List.of(entry(ManagementAudienceKind.ORG, 20L, 60))))
                .isInstanceOf(ManagementAudienceValidationException.class)
                .hasMessageContaining("unknown or inactive");
    }

    private void activeOrganizations(Organization... organizations) {
        when(organizationRepository.findActiveByTenantIdOrderByName(TENANT_ID))
                .thenReturn(List.of(organizations));
    }

    private static Organization org(long id) {
        return Organization.builder().id(id).tenantId(TENANT_ID).name("Ziel").build();
    }
}
