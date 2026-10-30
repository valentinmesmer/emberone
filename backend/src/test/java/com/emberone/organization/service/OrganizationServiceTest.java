/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.organization.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.dto.*;
import com.emberone.organization.event.OrganizationCreatedEvent;
import com.emberone.organization.exception.OrganizationCodeAlreadyExistsException;
import com.emberone.organization.exception.OrganizationDeactivatedException;
import com.emberone.organization.mapper.OrganizationMapper;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.platform.security.TenantContextHolder;
import com.emberone.standard.exception.StandardNotConfiguredException;
import com.emberone.standard.service.StandardReadinessService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    private static final OrganizationTypeDefinition DEPARTMENT =
            OrganizationTypeDefinition.builder()
                    .id(1L)
                    .tenantId(7L)
                    .name("Abteilung")
                    .active(true)
                    .build();

    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationMapper mapper;
    @Mock StandardReadinessService standardReadinessService;
    @Mock OrganizationTypeDefinitionService organizationTypeDefinitionService;
    @Mock ApplicationEventPublisher eventPublisher;

    private OrganizationService service;

    @BeforeEach
    void setUp() {
        service =
                new OrganizationService(
                        organizationRepository,
                        mapper,
                        standardReadinessService,
                        organizationTypeDefinitionService,
                        eventPublisher);
        lenient()
                .when(organizationTypeDefinitionService.requireActiveType(anyLong()))
                .thenReturn(DEPARTMENT);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void createsRootInTenantContextAndPublishesCreatedEvent() {
        SetupOrganizationRequest request = rootRequest("FW1");
        OrganizationDto dto = OrganizationDto.builder().id(10L).build();
        doNothing().when(standardReadinessService).requireReadyForOrganizationCreation(7L, 1L);
        when(organizationRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Organization organization = invocation.getArgument(0);
                            assertThat(TenantContextHolder.getTenantId()).isEqualTo(7L);
                            organization.setId(10L);
                            return organization;
                        });
        when(mapper.toDto(any())).thenReturn(dto);

        assertThat(service.createRootOrganizationForTenant(7L, request)).isSameAs(dto);

        verify(eventPublisher).publishEvent(new OrganizationCreatedEvent(7L, 10L, 1L));
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void rejectsRootCreationWhenStandardNotConfigured() {
        SetupOrganizationRequest request = rootRequest("FW1");
        doThrow(new StandardNotConfiguredException())
                .when(standardReadinessService)
                .requireReadyForOrganizationCreation(7L, 1L);

        assertThatThrownBy(() -> service.createRootOrganizationForTenant(7L, request))
                .isInstanceOf(StandardNotConfiguredException.class);

        verify(organizationRepository, never()).save(any());
    }

    @Test
    void duplicateRootCodeIsRejectedBeforeTenantContextIsSet() {
        SetupOrganizationRequest request = rootRequest("FW1");
        when(organizationRepository.findByOrganizationCode("FW1"))
                .thenReturn(Optional.of(Organization.builder().id(99L).build()));

        assertThatThrownBy(() -> service.createRootOrganizationForTenant(7L, request))
                .isInstanceOf(OrganizationCodeAlreadyExistsException.class);

        assertThat(TenantContextHolder.getTenantId()).isNull();
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void tenantContextIsClearedWhenCreatedEventPublishFails() {
        SetupOrganizationRequest request = rootRequest("FW1");
        when(organizationRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Organization organization = invocation.getArgument(0);
                            organization.setId(10L);
                            return organization;
                        });
        org.mockito.Mockito.doThrow(new IllegalStateException("boom"))
                .when(eventPublisher)
                .publishEvent(any(OrganizationCreatedEvent.class));

        assertThatThrownBy(() -> service.createRootOrganizationForTenant(7L, request))
                .isInstanceOf(IllegalStateException.class);
        assertThat(TenantContextHolder.getTenantId()).isNull();
    }

    @Test
    void createsChildWithParentTenantAndRejectsDuplicateCode() {
        Organization parent =
                Organization.builder()
                        .id(1L)
                        .tenantId(7L)
                        .name("Parent")
                        .organizationCode("P")
                        .build();
        CreateChildOrganizationRequest request = new CreateChildOrganizationRequest();
        request.setOrganizationCode("C1");
        request.setName("Child");
        request.setOrganizationTypeId(1L);
        doNothing().when(standardReadinessService).requireReadyForOrganizationCreation(7L, 1L);
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(organizationRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            Organization child = invocation.getArgument(0);
                            child.setId(2L);
                            return child;
                        });
        when(mapper.toDto(any())).thenReturn(OrganizationDto.builder().id(2L).build());

        service.createChildOrganization(1L, request);

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(7L);
        assertThat(captor.getValue().getParentOrganization()).isSameAs(parent);
        verify(eventPublisher).publishEvent(new OrganizationCreatedEvent(7L, 2L, 1L));

        when(organizationRepository.findByOrganizationCode("C1"))
                .thenReturn(Optional.of(Organization.builder().id(3L).build()));
        assertThatThrownBy(() -> service.createChildOrganization(1L, request))
                .isInstanceOf(OrganizationCodeAlreadyExistsException.class);
    }

    @Test
    void getChildOrganizationsSortsByNameThenId() {
        Organization beta = Organization.builder().id(2L).name("Beta").build();
        Organization alpha = Organization.builder().id(1L).name("alpha").build();
        when(organizationRepository.findByParentOrganizationId(10L))
                .thenReturn(List.of(beta, alpha));
        when(mapper.toDto(alpha)).thenReturn(OrganizationDto.builder().id(1L).build());
        when(mapper.toDto(beta)).thenReturn(OrganizationDto.builder().id(2L).build());

        assertThat(service.getChildOrganizations(10L))
                .extracting(OrganizationDto::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void updateOrganizationRejectsDuplicateCodeFromAnotherOrganization() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setName("Updated");
        request.setOrganizationCode("FW2");
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByOrganizationCode("FW2"))
                .thenReturn(Optional.of(Organization.builder().id(99L).build()));

        assertThatThrownBy(() -> service.updateOrganization(10L, request))
                .isInstanceOf(OrganizationCodeAlreadyExistsException.class);
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void moveOrganizationRejectsCycle() {
        Organization root = Organization.builder().id(1L).tenantId(7L).name("Root").build();
        Organization child =
                Organization.builder()
                        .id(2L)
                        .tenantId(7L)
                        .name("Child")
                        .parentOrganization(root)
                        .build();
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(root));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(root, child));

        MoveOrganizationRequest request = new MoveOrganizationRequest();
        request.setNewParentOrganizationId(2L);

        assertThatThrownBy(() -> service.moveOrganization(7L, 1L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("descendants");
    }

    @Test
    void createChildOrganizationRejectsDeactivatedParent() {
        Organization parent =
                Organization.builder()
                        .id(1L)
                        .tenantId(7L)
                        .deactivatedAt(java.time.Instant.now())
                        .build();
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(parent));

        CreateChildOrganizationRequest request = new CreateChildOrganizationRequest();
        request.setOrganizationCode("C1");
        request.setName("Child");
        request.setOrganizationTypeId(1L);

        assertThatThrownBy(() -> service.createChildOrganization(1L, request))
                .isInstanceOf(OrganizationDeactivatedException.class);
    }

    @Test
    void updateOrganizationPersistsWhenCodeBelongsToSameOrganization() {
        Organization organization =
                Organization.builder()
                        .id(10L)
                        .tenantId(7L)
                        .name("Old")
                        .organizationCode("FW1")
                        .build();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setName("Updated");
        request.setOrganizationCode("FW1");
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByOrganizationCode("FW1"))
                .thenReturn(Optional.of(organization));
        when(organizationRepository.save(organization)).thenReturn(organization);
        when(mapper.toDto(organization)).thenReturn(OrganizationDto.builder().id(10L).build());

        assertThat(service.updateOrganization(10L, request).getId()).isEqualTo(10L);
        verify(mapper).updateFromRequest(request, organization);
    }

    private SetupOrganizationRequest rootRequest(String code) {
        SetupOrganizationRequest request = new SetupOrganizationRequest();
        request.setOrganizationCode(code);
        request.setName("Fire Department");
        request.setOrganizationTypeId(1L);
        return request;
    }
}
