/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.courseoffering.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.courseoffering.dto.CourseOfferingTypeSettingsDto;
import com.emberone.courseoffering.organization.OrganizationCourseOfferingType;
import com.emberone.courseoffering.organization.OrganizationCourseOfferingTypeRepository;
import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.exception.ConflictException;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EffectiveStandardResolver;
import com.emberone.standard.service.EnabledModuleResolver;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizationCourseOfferingTypeServiceTest {

    private static final long TENANT_ID = 7L;
    private static final long ORG_ID = 10L;

    @Mock OrganizationRepository organizationRepository;
    @Mock EffectiveStandardResolver effectiveStandardResolver;
    @Mock EnabledModuleResolver enabledModuleResolver;
    @Mock OrganizationCourseOfferingTypeRepository typeRepository;
    @Mock TenantTrainingCourseTypeRepository tenantTypeRepository;

    @InjectMocks OrganizationCourseOfferingTypeService service;

    private Organization organization;
    private Standard standard;

    @BeforeEach
    void setUp() {
        organization = new Organization();
        organization.setId(ORG_ID);
        organization.setTenantId(TENANT_ID);
        standard = new Standard();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
    }

    private void moduleEnabled(boolean enabled) {
        when(effectiveStandardResolver.resolveForOrganization(organization))
                .thenReturn(Optional.of(standard));
        when(enabledModuleResolver.forStandard(standard))
                .thenReturn(enabled ? Set.of(Module.COURSE_OFFERINGS) : Set.of());
    }

    private static TenantTrainingCourseType type(String key, boolean archived) {
        TenantTrainingCourseType type = new TenantTrainingCourseType();
        type.setConfigKey(key);
        type.setArchived(archived);
        return type;
    }

    private static OrganizationCourseOfferingType row(String key) {
        return OrganizationCourseOfferingType.builder()
                .tenantId(TENANT_ID)
                .organizationId(ORG_ID)
                .definitionConfigKey(key)
                .build();
    }

    @Test
    void settingsAreEmptyAndDisabledWithoutModule() {
        moduleEnabled(false);

        CourseOfferingTypeSettingsDto settings = service.getSettings(TENANT_ID, ORG_ID);

        assertThat(settings.isEnabled()).isFalse();
        assertThat(settings.getDefinitionConfigKeys()).isEmpty();
    }

    @Test
    void settingsListSelectedKeysWhenModuleEnabled() {
        moduleEnabled(true);
        when(typeRepository.findByOrganizationId(ORG_ID)).thenReturn(List.of(row("B"), row("A")));

        CourseOfferingTypeSettingsDto settings = service.getSettings(TENANT_ID, ORG_ID);

        assertThat(settings.isEnabled()).isTrue();
        assertThat(settings.getDefinitionConfigKeys()).containsExactly("A", "B");
    }

    @Test
    void replaceAddsNewAndRemovesDroppedKeys() {
        moduleEnabled(true);
        when(tenantTypeRepository.findByConfigKey("A")).thenReturn(Optional.of(type("A", false)));
        when(tenantTypeRepository.findByConfigKey("B")).thenReturn(Optional.of(type("B", false)));
        OrganizationCourseOfferingType keptA = row("A");
        OrganizationCourseOfferingType droppedC = row("C");
        when(typeRepository.findByOrganizationId(ORG_ID)).thenReturn(List.of(keptA, droppedC));

        service.replace(TENANT_ID, ORG_ID, List.of("A", " B ", ""));

        verify(typeRepository).deleteAll(List.of(droppedC));
        verify(typeRepository)
                .saveAll(
                        org.mockito.ArgumentMatchers
                                .<Iterable<OrganizationCourseOfferingType>>any());
    }

    @Test
    void replaceRejectsDisabledModule() {
        moduleEnabled(false);

        assertThatThrownBy(() -> service.replace(TENANT_ID, ORG_ID, List.of("A")))
                .isInstanceOf(ConflictException.class);
        verify(typeRepository, never()).saveAll(any());
    }

    @Test
    void replaceRejectsUnknownType() {
        moduleEnabled(true);
        when(tenantTypeRepository.findByConfigKey("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.replace(TENANT_ID, ORG_ID, List.of("X")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void replaceRejectsArchivedType() {
        moduleEnabled(true);
        when(tenantTypeRepository.findByConfigKey("X")).thenReturn(Optional.of(type("X", true)));

        assertThatThrownBy(() -> service.replace(TENANT_ID, ORG_ID, List.of("X")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void organizationOfAnotherTenantIsNotFound() {
        assertThatThrownBy(() -> service.getSettings(99L, ORG_ID))
                .isInstanceOf(OrganizationNotFoundException.class);
    }
}
