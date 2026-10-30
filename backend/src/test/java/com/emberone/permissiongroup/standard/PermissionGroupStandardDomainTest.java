/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroup.standard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.module.domain.Module;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.permission.domain.PermissionGroupType;
import com.emberone.permission.domain.PermissionType;
import com.emberone.permission.event.PermissionGroupPermissionsChangedEvent;
import com.emberone.permissiongroup.domain.PermissionGroup;
import com.emberone.permissiongroup.repository.PermissionGroupRepository;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import com.emberone.permissiongroupmapping.service.PermissionGroupMappingService;
import com.emberone.platform.exception.ConflictException;
import com.emberone.platform.i18n.PermissionGroupNamingService;
import com.emberone.standard.domain.Standard;
import com.emberone.standard.service.EnabledModuleResolver;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PermissionGroupStandardDomainTest {

    @Mock PermissionGroupTemplateRepository templateRepository;
    @Mock OrgGroupPermissionGroupTemplateRepository mappingTemplateRepository;
    @Mock PermissionGroupRepository permissionGroupRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock PermissionGroupMappingService permissionGroupMappingService;
    @Mock PermissionGroupMappingRepository permissionGroupMappingRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock PermissionGroupNamingService permissionGroupNamingService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock EnabledModuleResolver enabledModuleResolver;

    private PermissionGroupStandardDomain domain;

    private static Standard standard(long id) {
        return Standard.builder().id(id).tenantId(7L).name("Test").build();
    }

    @BeforeEach
    void setUp() {
        lenient()
                .when(enabledModuleResolver.forStandard(any()))
                .thenReturn(EnumSet.allOf(Module.class));
        domain =
                new PermissionGroupStandardDomain(
                        templateRepository,
                        mappingTemplateRepository,
                        permissionGroupRepository,
                        organizationGroupRepository,
                        permissionGroupMappingService,
                        permissionGroupMappingRepository,
                        organizationRepository,
                        permissionGroupNamingService,
                        eventPublisher,
                        enabledModuleResolver);
    }

    @Test
    void addTemplateRejectsNonCustomType() {
        Standard standard = standard(1L);

        assertThatThrownBy(
                        () ->
                                domain.addTemplate(
                                        standard,
                                        "Büro",
                                        null,
                                        PermissionGroupType.ADMIN,
                                        List.of(PermissionType.ORGANIZATION_READ),
                                        List.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("custom");
        verify(templateRepository, never()).save(any());
    }

    @Test
    void deleteTemplateRejectsSystemGroup() {
        Standard standard = standard(1L);
        PermissionGroupTemplate admin =
                PermissionGroupTemplate.builder()
                        .id(10L)
                        .tenantId(7L)
                        .standardId(1L)
                        .configKey("ADMIN")
                        .groupType(PermissionGroupType.ADMIN)
                        .build();
        when(templateRepository.findByStandardIdAndConfigKey(1L, "ADMIN"))
                .thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> domain.deleteTemplate(standard, "ADMIN", List.of()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ADMIN");
        verify(templateRepository, never()).delete(any());
    }

    @Test
    void updateTemplateForAdminAlwaysAssignsAllPermissions() {
        Standard standard = standard(1L);
        PermissionGroupTemplate admin =
                PermissionGroupTemplate.builder()
                        .id(10L)
                        .tenantId(7L)
                        .standardId(1L)
                        .configKey("ADMIN")
                        .name(null)
                        .groupType(PermissionGroupType.ADMIN)
                        .grants(new java.util.ArrayList<>())
                        .build();
        admin.setPermissionTypes(List.of(PermissionType.ORGANIZATION_READ));
        when(templateRepository.findByStandardIdAndConfigKey(1L, "ADMIN"))
                .thenReturn(Optional.of(admin));

        domain.updateTemplate(
                standard,
                "ADMIN",
                null,
                null,
                List.of(PermissionType.ORGANIZATION_READ),
                List.of());

        ArgumentCaptor<PermissionGroupTemplate> captor =
                ArgumentCaptor.forClass(PermissionGroupTemplate.class);
        verify(templateRepository).save(captor.capture());
        assertThat(captor.getValue().getPermissionTypes())
                .containsExactlyInAnyOrder(
                        EnumSet.complementOf(EnumSet.of(PermissionType.MANAGEMENT_ACCESS))
                                .toArray(PermissionType[]::new));
    }

    @Test
    void updateTemplatePublishesPermissionsChangedEventForMaterializedOrgs() {
        Standard standard = standard(1L);
        Organization org = Organization.builder().id(100L).tenantId(7L).build();
        PermissionGroupTemplate template =
                PermissionGroupTemplate.builder()
                        .id(10L)
                        .tenantId(7L)
                        .standardId(1L)
                        .configKey("DISPATCH")
                        .name("Dispatch")
                        .groupType(PermissionGroupType.CUSTOM)
                        .grants(new java.util.ArrayList<>())
                        .build();
        template.setPermissionTypes(List.of(PermissionType.ORGANIZATION_READ));
        when(templateRepository.findByStandardIdAndConfigKey(1L, "DISPATCH"))
                .thenReturn(Optional.of(template));
        PermissionGroup instance =
                PermissionGroup.builder().id(200L).tenantId(7L).organizationId(100L).build();
        when(permissionGroupRepository.findByOrganizationIdAndConfigKey(100L, "DISPATCH"))
                .thenReturn(Optional.of(instance));

        domain.updateTemplate(
                standard,
                "DISPATCH",
                "Dispatch",
                null,
                List.of(PermissionType.ORGANIZATION_READ),
                List.of(org));

        verify(permissionGroupRepository).save(instance);
        verify(eventPublisher).publishEvent(new PermissionGroupPermissionsChangedEvent(7L, 200L));
    }
}
