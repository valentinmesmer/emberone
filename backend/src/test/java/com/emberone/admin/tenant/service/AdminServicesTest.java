/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.admin.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.admin.organization.dto.AdminOrganizationDetailDto;
import com.emberone.admin.organization.dto.AdminOrganizationTreeResponseDto;
import com.emberone.admin.organization.service.AdminOrganizationService;
import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.admin.tenant.domain.Tenant;
import com.emberone.admin.tenant.dto.CreateTenantRequest;
import com.emberone.admin.tenant.dto.TenantAdminDetailDto;
import com.emberone.admin.tenant.dto.TenantAdminPatchRequest;
import com.emberone.admin.tenant.dto.TenantDto;
import com.emberone.admin.tenant.list.TenantListFilter;
import com.emberone.admin.tenant.mapper.TenantMapper;
import com.emberone.admin.tenant.repository.TenantRepository;
import com.emberone.federalstate.FederalStateSettingsRegistry;
import com.emberone.federalstate.domain.FederalState;
import com.emberone.management.service.ManagementCapabilityService;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.domain.OrganizationTypeDefinition;
import com.emberone.organization.dto.CreateChildOrganizationRequest;
import com.emberone.organization.dto.OrganizationDto;
import com.emberone.organization.dto.SetupOrganizationRequest;
import com.emberone.organization.dto.UpdateOrganizationRequest;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organization.service.OrganizationDeactivationService;
import com.emberone.organization.service.OrganizationService;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.platform.security.EmberOneAppRole;
import com.emberone.platform.security.TokenService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.standard.service.StandardReadinessService;
import com.emberone.user.dto.CreateUserRequest;
import com.emberone.user.dto.SetupUserRequest;
import com.emberone.user.dto.UserDto;
import com.emberone.user.dto.UserPageDto;
import com.emberone.user.mapper.UserMapper;
import com.emberone.user.service.UserService;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.dto.UserOrganizationGroupMembershipDto;
import com.emberone.userorganizationgroupmembership.mapper.UserOrganizationGroupMembershipMapper;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminServicesTest {

    private static final ListQuery DEFAULT_LIST_QUERY = new ListQuery(0, null, null, null);

    private static final OrganizationTypeDefinition DEPARTMENT =
            OrganizationTypeDefinition.builder()
                    .id(1L)
                    .tenantId(7L)
                    .name("Abteilung")
                    .active(true)
                    .build();

    @Mock TenantRepository tenantRepository;
    @Mock TenantMapper tenantMapper;
    @Mock OrganizationService organizationService;
    @Mock OrganizationDeactivationService organizationDeactivationService;
    @Mock OrganizationRepository organizationRepository;
    @Mock UserService userService;
    @Mock UserMapper userMapper;
    @Mock UserOrganizationGroupMembershipRepository groupMembershipRepository;
    @Mock OrganizationGroupRepository organizationGroupRepository;
    @Mock UserOrganizationGroupMembershipService groupMembershipService;
    @Mock UserOrganizationGroupMembershipMapper groupMembershipMapper;
    @Mock ManagementCapabilityService managementCapabilityService;
    @Mock TokenService tokenService;
    @Mock TenantCreationService tenantCreationService;
    @Mock TenantStandardsBootstrapService tenantStandardsBootstrapService;
    @Mock StandardReadinessService standardReadinessService;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock FederalStateSettingsRegistry federalStateSettingsRegistry;
    private AdminTenantService tenantAdminService;
    private AdminOrganizationService organizationAdminService;

    @BeforeEach
    void setUp() {
        organizationAdminService =
                new AdminOrganizationService(
                        tenantRepository,
                        organizationService,
                        organizationDeactivationService,
                        organizationRepository,
                        userService,
                        userMapper,
                        groupMembershipRepository,
                        organizationGroupRepository,
                        groupMembershipService,
                        groupMembershipMapper,
                        managementCapabilityService);
        tenantAdminService =
                new AdminTenantService(
                        tenantRepository,
                        tenantMapper,
                        organizationService,
                        tokenService,
                        tenantCreationService,
                        tenantStandardsBootstrapService,
                        organizationAdminService,
                        standardReadinessService,
                        eventPublisher,
                        federalStateSettingsRegistry);
    }

    @Test
    void globalAdminListsAllTenantsSortedByNameThenId() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(true);
        Tenant zulu = Tenant.builder().id(2L).name("Zulu").build();
        Tenant alpha = Tenant.builder().id(1L).name("alpha").build();
        when(tenantRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alpha, zulu)));
        when(tenantMapper.toDto(zulu)).thenReturn(TenantDto.builder().id(2L).name("Zulu").build());
        when(tenantMapper.toDto(alpha))
                .thenReturn(TenantDto.builder().id(1L).name("alpha").build());

        assertThat(
                        tenantAdminService
                                .listTenants(DEFAULT_LIST_QUERY, TenantListFilter.NONE)
                                .getContent())
                .extracting(TenantDto::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void tenantAdminOnlyReceivesOwnTenantAndRequiresTenantClaim() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(null);
        assertThatThrownBy(
                        () ->
                                tenantAdminService.listTenants(
                                        DEFAULT_LIST_QUERY, TenantListFilter.NONE))
                .isInstanceOf(ResponseStatusException.class);

        Tenant tenant = Tenant.builder().id(7L).name("EmberOne").build();
        when(tokenService.getTenantId()).thenReturn(7L);
        when(tenantRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(tenant)));
        when(tenantMapper.toDto(tenant)).thenReturn(TenantDto.builder().id(7L).build());
        assertThat(
                        tenantAdminService
                                .listTenants(DEFAULT_LIST_QUERY, TenantListFilter.NONE)
                                .getContent())
                .extracting(TenantDto::getId)
                .containsExactly(7L);
    }

    @Test
    void createTenantNormalizesNameDefaultsLocaleAndSeedsStandards() {
        CreateTenantRequest request = new CreateTenantRequest();
        request.setName(" EmberOne ");
        request.setAbbreviation("FW");
        when(tenantRepository.existsByAbbreviation("fw")).thenReturn(false);
        when(tenantCreationService.persist(any()))
                .thenAnswer(
                        invocation -> {
                            Tenant tenant = invocation.getArgument(0);
                            tenant.setId(7L);
                            return tenant;
                        });
        when(tenantMapper.toDto(any())).thenReturn(TenantDto.builder().id(7L).build());
        when(federalStateSettingsRegistry.require(FederalState.BAYERN))
                .thenReturn(mock(com.emberone.federalstate.FederalStateSettings.class));

        tenantAdminService.createTenant(request);

        var captor = org.mockito.ArgumentCaptor.forClass(Tenant.class);
        verify(tenantCreationService).persist(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("EmberOne");
        assertThat(captor.getValue().getAbbreviation()).isEqualTo("fw");
        assertThat(captor.getValue().getLocale()).isEqualTo(SupportedLocale.DE);
        assertThat(captor.getValue().getFederalState()).isEqualTo(FederalState.BAYERN);
        verify(federalStateSettingsRegistry).require(FederalState.BAYERN);
        verify(tenantStandardsBootstrapService).syncBaseline(7L);
    }

    @Test
    void patchTenantTrimsValuesAndRejectsBlankName() {
        Tenant tenant = Tenant.builder().id(7L).name("Old").build();
        TenantAdminPatchRequest request = new TenantAdminPatchRequest();
        request.setName(" EmberOne ");
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(tenant)).thenReturn(tenant);
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of());
        when(organizationRepository.findByParentOrganizationIsNull()).thenReturn(List.of());

        tenantAdminService.patchTenant(7L, request);

        assertThat(tenant.getName()).isEqualTo("EmberOne");

        request.setName("  ");
        assertThatThrownBy(() -> tenantAdminService.patchTenant(7L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Name cannot be empty");
    }

    @Test
    void createRootOrganizationRequiresTenantAndReturnsRefreshedDetail() {
        SetupOrganizationRequest request = new SetupOrganizationRequest();
        Tenant tenant = Tenant.builder().id(7L).name("EmberOne").build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of());
        when(organizationRepository.findByParentOrganizationIsNull()).thenReturn(List.of());

        assertThat(tenantAdminService.createRootOrganization(7L, request).getId()).isEqualTo(7L);
        verify(organizationService).createRootOrganizationForTenant(7L, request);

        when(tenantRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tenantAdminService.createRootOrganization(8L, request))
                .isInstanceOf(ResponseStatusException.class);
        verify(organizationService, never()).createRootOrganizationForTenant(8L, request);
    }

    @Test
    void createLeaderUserBuildsTrimmedSetupAndAssignsLeadership() {
        Organization organization =
                Organization.builder().id(10L).tenantId(7L).type(DEPARTMENT).build();
        CreateUserRequest request = new CreateUserRequest();
        request.setFirstName(" Ada ");
        request.setLastName(" Lovelace ");
        request.setPrivateEmail(" ada@example.com ");
        request.setStreet(" Main 1 ");
        request.setCity(" Berlin ");
        request.setPostalCode(" 10115 ");
        request.setCountry(" DE ");
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(groupMembershipRepository.findActiveLeadershipLeaderMemberships(any(), any(), any()))
                .thenReturn(List.of());
        when(userService.createInitialTenantMember(any(), any()))
                .thenReturn(UserDto.builder().id(1L).build());
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(organization));
        when(groupMembershipRepository.findOrganizationIdsWithActiveLeadershipMember(any()))
                .thenReturn(List.of(10L));
        when(organizationRepository.existsByParentOrganization_Id(10L)).thenReturn(false);

        organizationAdminService.createOrganizationLeaderUser(7L, 10L, request);

        var captor = org.mockito.ArgumentCaptor.forClass(SetupUserRequest.class);
        verify(userService)
                .createInitialTenantMember(org.mockito.ArgumentMatchers.eq(7L), captor.capture());
        assertThat(captor.getValue().getFirstName()).isEqualTo("Ada");
        assertThat(captor.getValue().getPrivateEmail()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getMainOrganizationId()).isEqualTo(10L);
        verify(userService).addUserToLeadershipGroupAsLeader(1L, 10L);
    }

    @Test
    void getTenantDetailReturnsMappedDetail() {
        Tenant tenant = Tenant.builder().id(7L).name("EmberOne").build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of());
        when(organizationRepository.findByParentOrganizationIsNull()).thenReturn(List.of());
        when(standardReadinessService.isReadyForOrganizationCreation(7L)).thenReturn(false);

        TenantAdminDetailDto detail = tenantAdminService.getTenantDetail(7L);

        assertThat(detail.getId()).isEqualTo(7L);
        assertThat(detail.getName()).isEqualTo("EmberOne");
        assertThat(detail.isStandardsConfiguredForOrgCreation()).isFalse();
    }

    @Test
    void getTenantDetailThrowsWhenTenantMissing() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantAdminService.getTenantDetail(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    void listTenantsRejectsCallerWithoutAdminRole() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                tenantAdminService.listTenants(
                                        DEFAULT_LIST_QUERY, TenantListFilter.NONE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Unauthorized");
        verify(tenantRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getAdminOrganizationTreeBuildsHierarchyAndLeaderFlags() {
        Tenant tenant = Tenant.builder().id(7L).name("EmberOne").build();
        Organization parent =
                Organization.builder()
                        .id(10L)
                        .tenantId(7L)
                        .name("Parent")
                        .organizationCode("P")
                        .type(DEPARTMENT)
                        .build();
        Organization child =
                Organization.builder()
                        .id(11L)
                        .tenantId(7L)
                        .name("Child")
                        .organizationCode("C")
                        .type(DEPARTMENT)
                        .parentOrganization(parent)
                        .build();
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(parent, child));
        when(groupMembershipRepository.findOrganizationIdsWithActiveLeadershipMember(any()))
                .thenReturn(List.of(11L));

        AdminOrganizationTreeResponseDto tree = organizationAdminService.getOrganizationTree(7L);

        assertThat(tree.getTenantId()).isEqualTo(7L);
        assertThat(tree.getRoots())
                .singleElement()
                .satisfies(
                        root -> {
                            assertThat(root.getId()).isEqualTo(10L);
                            assertThat(root.isLeaderMissing()).isTrue();
                            assertThat(root.isLeaderMissingInSubtree()).isTrue();
                            assertThat(root.getChildren())
                                    .singleElement()
                                    .satisfies(
                                            childNode -> {
                                                assertThat(childNode.getId()).isEqualTo(11L);
                                                assertThat(childNode.isLeaderMissing()).isFalse();
                                            });
                        });
    }

    @Test
    void getOrganizationUsersDelegatesToUserService() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        UserDto user = UserDto.builder().id(1L).firstName("A").lastName("B").build();
        when(userService.listUsersForOrganization(10L, DEFAULT_LIST_QUERY))
                .thenReturn(UserPageDto.builder().content(List.of(user)).build());

        assertThat(
                        organizationAdminService
                                .getOrganizationUsers(7L, 10L, DEFAULT_LIST_QUERY)
                                .getContent())
                .singleElement()
                .extracting(UserDto::getId)
                .isEqualTo(1L);
    }

    @Test
    void getAdminOrganizationRejectsCrossTenantAccess() {
        Organization foreign = Organization.builder().id(10L).tenantId(8L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> organizationAdminService.getOrganization(7L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void updateAdminOrganizationDelegatesToOrganizationService() {
        Organization organization =
                Organization.builder()
                        .id(10L)
                        .tenantId(7L)
                        .name("Parent")
                        .organizationCode("P")
                        .type(DEPARTMENT)
                        .build();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        request.setName("Updated");
        request.setOrganizationCode("P");
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(organization));
        when(groupMembershipRepository.findOrganizationIdsWithActiveLeadershipMember(any()))
                .thenReturn(List.of());
        when(groupMembershipRepository.findActiveLeadershipMemberships(any(), any()))
                .thenReturn(List.of());
        when(organizationRepository.existsByParentOrganization_Id(10L)).thenReturn(false);

        AdminOrganizationDetailDto detail =
                organizationAdminService.updateOrganization(7L, 10L, request);

        verify(organizationService).updateOrganization(10L, request);
        assertThat(detail.getId()).isEqualTo(10L);
    }

    @Test
    void createAdminChildOrganizationValidatesTenantAndReturnsDetail() {
        Organization parent =
                Organization.builder()
                        .id(10L)
                        .tenantId(7L)
                        .name("Parent")
                        .organizationCode("P")
                        .type(DEPARTMENT)
                        .build();
        CreateChildOrganizationRequest request = new CreateChildOrganizationRequest();
        request.setOrganizationCode("C1");
        request.setName("Child");
        request.setOrganizationTypeId(1L);
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(organizationService.createChildOrganization(10L, request))
                .thenReturn(OrganizationDto.builder().id(11L).build());
        when(organizationRepository.findById(11L))
                .thenReturn(
                        Optional.of(
                                Organization.builder()
                                        .id(11L)
                                        .tenantId(7L)
                                        .parentOrganization(parent)
                                        .name("Child")
                                        .organizationCode("C1")
                                        .type(DEPARTMENT)
                                        .build()));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(parent));
        when(groupMembershipRepository.findOrganizationIdsWithActiveLeadershipMember(any()))
                .thenReturn(List.of());
        when(groupMembershipRepository.findActiveLeadershipMemberships(any(), any()))
                .thenReturn(List.of());
        when(organizationRepository.existsByParentOrganization_Id(11L)).thenReturn(false);

        AdminOrganizationDetailDto detail =
                organizationAdminService.createChildOrganization(7L, 10L, request);

        assertThat(detail.getId()).isEqualTo(11L);
        verify(organizationService).createChildOrganization(10L, request);
    }

    @Test
    void getAdminChildOrganizationsReturnsSortedChildrenForTenant() {
        Organization parent = Organization.builder().id(10L).tenantId(7L).build();
        Organization beta =
                Organization.builder()
                        .id(12L)
                        .tenantId(7L)
                        .parentOrganization(parent)
                        .name("Beta")
                        .organizationCode("B")
                        .type(DEPARTMENT)
                        .build();
        Organization alpha =
                Organization.builder()
                        .id(11L)
                        .tenantId(7L)
                        .parentOrganization(parent)
                        .name("alpha")
                        .organizationCode("A")
                        .type(DEPARTMENT)
                        .build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(organizationRepository.findAllWithParentFetch())
                .thenReturn(List.of(parent, alpha, beta));
        when(organizationRepository.findByParentOrganizationId(10L))
                .thenReturn(List.of(beta, alpha));
        when(groupMembershipRepository.findOrganizationIdsWithActiveLeadershipMember(any()))
                .thenReturn(List.of());
        when(groupMembershipRepository.findActiveLeadershipMemberships(any(), any()))
                .thenReturn(List.of());
        when(organizationRepository.existsByParentOrganization_Id(any(Long.class)))
                .thenReturn(false);

        assertThat(organizationAdminService.getChildOrganizations(7L, 10L))
                .extracting(AdminOrganizationDetailDto::getId)
                .containsExactly(11L, 12L);
    }

    @Test
    void tenantAdminReceivesEmptyListWhenOwnTenantMissing() {
        when(tokenService.hasRole(EmberOneAppRole.GLOBAL_ADMIN.getRoleName())).thenReturn(false);
        when(tokenService.hasRole(EmberOneAppRole.TENANT_ADMIN.getRoleName())).thenReturn(true);
        when(tokenService.getTenantId()).thenReturn(7L);
        when(tenantRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(
                        tenantAdminService
                                .listTenants(DEFAULT_LIST_QUERY, TenantListFilter.NONE)
                                .getContent())
                .isEmpty();
    }

    @Test
    void patchTenantThrowsWhenTenantMissing() {
        when(tenantRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> tenantAdminService.patchTenant(99L, new TenantAdminPatchRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    void getAdminOrganizationReturnsDetailForTenantOrganization() {
        Organization organization =
                Organization.builder()
                        .id(10L)
                        .tenantId(7L)
                        .name("Parent")
                        .organizationCode("P")
                        .type(DEPARTMENT)
                        .build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findAllWithParentFetch()).thenReturn(List.of(organization));
        when(groupMembershipRepository.findOrganizationIdsWithActiveLeadershipMember(any()))
                .thenReturn(List.of(10L));
        when(groupMembershipRepository.findActiveLeadershipMemberships(any(), any()))
                .thenReturn(List.of());
        when(organizationRepository.existsByParentOrganization_Id(10L)).thenReturn(false);

        AdminOrganizationDetailDto detail = organizationAdminService.getOrganization(7L, 10L);

        assertThat(detail.getId()).isEqualTo(10L);
        assertThat(detail.getName()).isEqualTo("Parent");
        assertThat(detail.isLeaderMissingInSubtree()).isFalse();
    }

    @Test
    void getAdminOrganizationThrowsWhenOrganizationMissing() {
        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationAdminService.getOrganization(7L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Organization not found");
    }

    @Test
    void createAdminChildOrganizationRejectsCrossTenantAndMissingParent() {
        CreateChildOrganizationRequest request = new CreateChildOrganizationRequest();
        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> organizationAdminService.createChildOrganization(7L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Organization not found");

        Organization foreign = Organization.builder().id(10L).tenantId(8L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> organizationAdminService.createChildOrganization(7L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void updateAdminOrganizationRejectsMissingAndCrossTenantOrganizations() {
        UpdateOrganizationRequest request = new UpdateOrganizationRequest();
        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> organizationAdminService.updateOrganization(7L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Organization not found");

        Organization foreign = Organization.builder().id(10L).tenantId(8L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> organizationAdminService.updateOrganization(7L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
        verify(organizationService, never()).updateOrganization(any(Long.class), any());
    }

    @Test
    void createAdminOrgLeaderUserRejectsMissingOrgCrossTenantAndExistingLeader() {
        CreateUserRequest request = new CreateUserRequest();
        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                organizationAdminService.createOrganizationLeaderUser(
                                        7L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Organization not found");

        Organization foreign = Organization.builder().id(10L).tenantId(8L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(
                        () ->
                                organizationAdminService.createOrganizationLeaderUser(
                                        7L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");

        Organization own = Organization.builder().id(10L).tenantId(7L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(own));
        when(groupMembershipRepository.findActiveLeadershipLeaderMemberships(any(), any(), any()))
                .thenReturn(List.of(mock(UserOrganizationGroupMembership.class)));
        assertThatThrownBy(
                        () ->
                                organizationAdminService.createOrganizationLeaderUser(
                                        7L, 10L, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already has");
        verify(userService, never()).createInitialTenantMember(any(), any());
    }

    @Test
    void getAdminChildOrganizationsRejectsMissingAndCrossTenantParent() {
        when(organizationRepository.findById(10L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> organizationAdminService.getChildOrganizations(7L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Organization not found");

        Organization foreign = Organization.builder().id(10L).tenantId(8L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(foreign));
        assertThatThrownBy(() -> organizationAdminService.getChildOrganizations(7L, 10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void getAdminOrganizationTreeThrowsWhenTenantMissing() {
        when(tenantRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationAdminService.getOrganizationTree(7L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Tenant not found");
    }

    @Test
    void getLeadershipGroupMembershipsReturnsAllMembershipsIncludingEnded() {
        Organization organization = Organization.builder().id(10L).tenantId(7L).build();
        OrganizationGroup leadershipGroup =
                OrganizationGroup.builder()
                        .id(30L)
                        .organization(organization)
                        .type(OrganizationGroupType.LEADERSHIP)
                        .build();
        UserOrganizationGroupMembership ended =
                UserOrganizationGroupMembership.builder().id(5L).build();
        UserOrganizationGroupMembershipDto dto =
                UserOrganizationGroupMembershipDto.builder().id(5L).build();
        when(organizationRepository.findById(10L)).thenReturn(Optional.of(organization));
        when(organizationGroupRepository.findByOrganizationIdAndType(
                        10L, OrganizationGroupType.LEADERSHIP))
                .thenReturn(Optional.of(leadershipGroup));
        when(groupMembershipRepository.findByOrganizationGroup_IdWithUser(30L))
                .thenReturn(List.of(ended));
        when(groupMembershipMapper.toDto(ended)).thenReturn(dto);

        assertThat(organizationAdminService.getLeadershipGroupMemberships(7L, 10L).getMemberships())
                .extracting(UserOrganizationGroupMembershipDto::getId)
                .containsExactly(5L);
        verify(groupMembershipRepository, never())
                .findActiveByOrganizationGroup_IdWithUser(any(), any());
    }
}
