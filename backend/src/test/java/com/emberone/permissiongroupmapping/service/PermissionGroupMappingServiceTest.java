/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.permissiongroupmapping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.permission.event.OrganizationGroupPermissionsChangedEvent;
import com.emberone.permission.event.PermissionGroupPermissionsChangedEvent;
import com.emberone.permissiongroup.domain.PermissionGroup;
import com.emberone.permissiongroup.exception.PermissionGroupNotFoundException;
import com.emberone.permissiongroup.repository.PermissionGroupRepository;
import com.emberone.permissiongroupmapping.domain.PermissionGroupMapping;
import com.emberone.permissiongroupmapping.repository.PermissionGroupMappingRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PermissionGroupMappingServiceTest {

    @Mock PermissionGroupMappingRepository mappingRepository;
    @Mock PermissionGroupRepository permissionGroupRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    private PermissionGroupMappingService service;

    @BeforeEach
    void setUp() {
        service =
                new PermissionGroupMappingService(
                        mappingRepository, permissionGroupRepository, eventPublisher);
    }

    @Test
    void createStandardMappingPersistsMappingWithTenantFromPermissionGroup() {
        PermissionGroup permissionGroup = PermissionGroup.builder().id(5L).tenantId(7L).build();
        when(permissionGroupRepository.findById(5L)).thenReturn(Optional.of(permissionGroup));
        when(mappingRepository.existsByOrganizationGroupIdAndPermissionGroup_Id(100L, 5L))
                .thenReturn(false);
        when(mappingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createStandardMapping(100L, 5L);

        ArgumentCaptor<PermissionGroupMapping> captor =
                ArgumentCaptor.forClass(PermissionGroupMapping.class);
        verify(mappingRepository).save(captor.capture());
        PermissionGroupMapping saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(7L);
        assertThat(saved.getOrganizationGroupId()).isEqualTo(100L);
        assertThat(saved.getPermissionGroup()).isSameAs(permissionGroup);
        assertThat(saved.isStandardManaged()).isTrue();
        verify(eventPublisher).publishEvent(new OrganizationGroupPermissionsChangedEvent(7L, 100L));
    }

    @Test
    void createStandardMappingSkipsSaveWhenMappingAlreadyExists() {
        PermissionGroup permissionGroup = PermissionGroup.builder().id(5L).tenantId(7L).build();
        when(permissionGroupRepository.findById(5L)).thenReturn(Optional.of(permissionGroup));
        when(mappingRepository.existsByOrganizationGroupIdAndPermissionGroup_Id(100L, 5L))
                .thenReturn(true);

        service.createStandardMapping(100L, 5L);

        verify(mappingRepository, never()).save(any());
    }

    @Test
    void createStandardMappingThrowsWhenPermissionGroupMissing() {
        when(permissionGroupRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createStandardMapping(100L, 5L))
                .isInstanceOf(PermissionGroupNotFoundException.class);
    }

    @Test
    void deleteMappingsByOrganizationGroupIdPublishesEventWhenMappingsExist() {
        when(mappingRepository.findByOrganizationGroupId(100L))
                .thenReturn(
                        java.util.List.of(
                                PermissionGroupMapping.builder()
                                        .tenantId(7L)
                                        .organizationGroupId(100L)
                                        .build()));

        service.deleteMappingsByOrganizationGroupId(100L);

        verify(mappingRepository).deleteByOrganizationGroupId(100L);
        verify(eventPublisher).publishEvent(new OrganizationGroupPermissionsChangedEvent(7L, 100L));
    }

    @Test
    void deleteMappingsByOrganizationGroupIdDelegatesToRepository() {
        when(mappingRepository.findByOrganizationGroupId(100L)).thenReturn(java.util.List.of());

        service.deleteMappingsByOrganizationGroupId(100L);

        verify(mappingRepository).deleteByOrganizationGroupId(100L);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void deleteMappingsByPermissionGroupIdPublishesEventWhenGroupExists() {
        when(permissionGroupRepository.findById(5L))
                .thenReturn(Optional.of(PermissionGroup.builder().id(5L).tenantId(7L).build()));

        service.deleteMappingsByPermissionGroupId(5L);

        verify(mappingRepository).deleteByPermissionGroupId(5L);
        verify(eventPublisher).publishEvent(new PermissionGroupPermissionsChangedEvent(7L, 5L));
    }

    @Test
    void deleteMappingsByPermissionGroupIdDelegatesToRepository() {
        when(permissionGroupRepository.findById(5L)).thenReturn(Optional.empty());

        service.deleteMappingsByPermissionGroupId(5L);

        verify(mappingRepository).deleteByPermissionGroupId(5L);
        verify(eventPublisher, never()).publishEvent(any());
    }
}
