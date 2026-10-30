/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.absence.domain.AbsenceReason;
import com.emberone.absence.domain.UserAbsence;
import com.emberone.absence.domain.UserAbsenceOrganization;
import com.emberone.absence.dto.CreateUserAbsenceRequest;
import com.emberone.absence.dto.UpdateUserAbsenceRequest;
import com.emberone.absence.dto.UserAbsenceDto;
import com.emberone.absence.dto.UserAbsencePageDto;
import com.emberone.absence.exception.AbsenceValidationException;
import com.emberone.absence.exception.UserAbsenceNotFoundException;
import com.emberone.absence.list.AbsenceListFilter;
import com.emberone.absence.mapper.UserAbsenceDtoAssembler;
import com.emberone.absence.mapper.UserAbsenceMapper;
import com.emberone.absence.repository.UserAbsenceRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserAbsenceServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORG_ID = 10L;
    private static final Long OTHER_ORG_ID = 20L;
    private static final Long TENANT_ID = 5L;

    @Mock private UserAbsenceRepository userAbsenceRepository;

    @Mock private UserRepository userRepository;

    @Mock private OrganizationRepository organizationRepository;

    @Mock private OrganizationAccessService organizationAccessService;

    @Mock private UserService userService;

    @Mock private UserAbsenceMapper mapper;

    @Mock private UserAbsenceDtoAssembler userAbsenceDtoAssembler;

    @InjectMocks private UserAbsenceService userAbsenceService;

    private static CreateUserAbsenceRequest validCreateRequest() {
        return CreateUserAbsenceRequest.builder()
                .organizationIds(List.of(ORG_ID))
                .startDate(LocalDate.of(2026, 6, 10))
                .endDate(LocalDate.of(2026, 6, 12))
                .reason(AbsenceReason.VACATION)
                .build();
    }

    @Test
    void createAbsence_rejectsEndBeforeStart() {
        CreateUserAbsenceRequest request =
                CreateUserAbsenceRequest.builder()
                        .organizationIds(List.of(10L))
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 9))
                        .reason(AbsenceReason.VACATION)
                        .build();

        assertThatThrownBy(() -> userAbsenceService.createAbsence(USER_ID, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessageContaining("End date must not be before start date");
    }

    @Test
    void createAbsence_requiresAtLeastOneOrganization() {
        CreateUserAbsenceRequest request =
                CreateUserAbsenceRequest.builder()
                        .organizationIds(List.of())
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 12))
                        .reason(AbsenceReason.VACATION)
                        .build();

        assertThatThrownBy(() -> userAbsenceService.createAbsence(USER_ID, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessageContaining("At least one organization is required");
    }

    @Test
    void createAbsence_ignoresNullOrganizationIdsWhenNormalizing() {
        CreateUserAbsenceRequest request =
                CreateUserAbsenceRequest.builder()
                        .organizationIds(Arrays.asList(null, null))
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 12))
                        .reason(AbsenceReason.VACATION)
                        .build();

        assertThatThrownBy(() -> userAbsenceService.createAbsence(USER_ID, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessageContaining("At least one organization is required");
    }

    @Test
    void listAbsencesForUser_requiresExistingUser() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                userAbsenceService.listAbsencesForUser(
                                        USER_ID,
                                        new ListQuery(0, null, null, null),
                                        AbsenceListFilter.empty()))
                .isInstanceOf(UserNotFoundException.class);
        verify(userAbsenceRepository, never())
                .findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listAbsencesForUser_mapsRepositoryPage() {
        UserAbsence absence = UserAbsence.builder().id(3L).build();
        UserAbsencePageDto page =
                UserAbsencePageDto.builder()
                        .content(List.of(UserAbsenceDto.builder().id(3L).build()))
                        .build();
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userAbsenceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(absence)));
        when(userAbsenceRepository.findWithOrganizationsByIdIn(List.of(3L)))
                .thenReturn(List.of(absence));
        when(userAbsenceDtoAssembler.toPageDto(any(Page.class))).thenReturn(page);

        assertThat(
                        userAbsenceService.listAbsencesForUser(
                                USER_ID,
                                new ListQuery(0, null, null, null),
                                AbsenceListFilter.empty()))
                .isEqualTo(page);
    }

    @Test
    void createAbsence_deduplicatesOrganizationsAndBuildsLinks() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        UserAbsenceDto dto = UserAbsenceDto.builder().id(4L).build();
        CreateUserAbsenceRequest request =
                CreateUserAbsenceRequest.builder()
                        .organizationIds(Arrays.asList(ORG_ID, null, ORG_ID))
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 12))
                        .reason(AbsenceReason.VACATION)
                        .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(true);
        org.mockito.Mockito.doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(USER_ID, ORG_ID);
        when(organizationRepository.getReferenceById(ORG_ID)).thenReturn(organization);
        when(userAbsenceRepository.save(any(UserAbsence.class)))
                .thenAnswer(
                        invocation -> {
                            UserAbsence saved = invocation.getArgument(0);
                            saved.setId(4L);
                            return saved;
                        });
        when(mapper.toDto(any(UserAbsence.class))).thenReturn(dto);

        assertThat(userAbsenceService.createAbsence(USER_ID, request)).isSameAs(dto);
        var captor = org.mockito.ArgumentCaptor.forClass(UserAbsence.class);
        verify(userAbsenceRepository).save(captor.capture());
        UserAbsence saved = captor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getOrganizationLinks())
                .singleElement()
                .satisfies(
                        link -> {
                            assertThat(link.getAbsence()).isSameAs(saved);
                            assertThat(link.getOrganization()).isSameAs(organization);
                            assertThat(link.getTenantId()).isEqualTo(TENANT_ID);
                        });
        verify(userAbsenceRepository).flush();
    }

    @Test
    void createAbsence_rejectsMissingOrUnauthorizedOrganization() {
        CreateUserAbsenceRequest request = validCreateRequest();
        when(organizationRepository.existsById(ORG_ID)).thenReturn(false);

        assertThatThrownBy(() -> userAbsenceService.createAbsence(USER_ID, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessage("Organization not found.");

        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(false);
        assertThatThrownBy(() -> userAbsenceService.createAbsence(USER_ID, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessageContaining("absence write access");
    }

    @Test
    void createAbsence_rejectsUserWithoutOrganizationMembership() {
        CreateUserAbsenceRequest request = validCreateRequest();
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(true);
        org.mockito.Mockito.doThrow(
                        new IllegalArgumentException(
                                "User does not belong to the specified organization"))
                .when(userService)
                .assertUserBelongsToOrganization(USER_ID, ORG_ID);

        assertThatThrownBy(() -> userAbsenceService.createAbsence(USER_ID, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessageContaining("belong to the selected organization");
    }

    @Test
    void createAbsence_requiresWritePermissionAndMembership() {
        User user = User.builder().id(USER_ID).tenantId(TENANT_ID).build();
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        UserAbsenceDto dto = UserAbsenceDto.builder().id(4L).build();
        CreateUserAbsenceRequest request = validCreateRequest();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(true);
        org.mockito.Mockito.doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(USER_ID, ORG_ID);
        when(organizationRepository.getReferenceById(ORG_ID)).thenReturn(organization);
        when(userAbsenceRepository.save(any(UserAbsence.class)))
                .thenAnswer(
                        invocation -> {
                            UserAbsence saved = invocation.getArgument(0);
                            saved.setId(4L);
                            return saved;
                        });
        when(mapper.toDto(any(UserAbsence.class))).thenReturn(dto);

        assertThat(userAbsenceService.createAbsence(USER_ID, request)).isSameAs(dto);
    }

    @Test
    void updateAbsence_updatesExistingEntityAndReplacesLinks() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        UserAbsenceOrganization existingLink =
                UserAbsenceOrganization.builder().organization(organization).build();
        UserAbsence absence =
                UserAbsence.builder()
                        .id(8L)
                        .organizationLinks(new java.util.ArrayList<>(List.of(existingLink)))
                        .build();
        UpdateUserAbsenceRequest request =
                UpdateUserAbsenceRequest.builder()
                        .organizationIds(List.of(ORG_ID))
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 11))
                        .reason(AbsenceReason.VACATION)
                        .build();
        when(userAbsenceRepository.findByIdAndUser_Id(8L, USER_ID))
                .thenReturn(Optional.of(absence));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(true);
        org.mockito.Mockito.doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(USER_ID, ORG_ID);
        when(organizationRepository.getReferenceById(ORG_ID)).thenReturn(organization);
        when(userAbsenceRepository.save(absence)).thenReturn(absence);
        UserAbsenceDto dto = UserAbsenceDto.builder().id(8L).build();
        when(mapper.toDto(absence)).thenReturn(dto);

        assertThat(userAbsenceService.updateAbsence(USER_ID, 8L, request)).isSameAs(dto);
        verify(mapper).updateFromRequest(request, absence);
        assertThat(absence.getOrganizationLinks())
                .singleElement()
                .extracting(UserAbsenceOrganization::getOrganization)
                .isEqualTo(organization);
    }

    @Test
    void updateAbsence_preservesOrganizationLinksWithoutWriteAccess() {
        Organization writableOrg = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        Organization protectedOrg =
                Organization.builder().id(OTHER_ORG_ID).tenantId(TENANT_ID).build();
        UserAbsenceOrganization protectedLink =
                UserAbsenceOrganization.builder().organization(protectedOrg).build();
        UserAbsence absence =
                UserAbsence.builder()
                        .id(8L)
                        .organizationLinks(new java.util.ArrayList<>(List.of(protectedLink)))
                        .build();
        UpdateUserAbsenceRequest request =
                UpdateUserAbsenceRequest.builder()
                        .organizationIds(List.of(ORG_ID))
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 11))
                        .reason(AbsenceReason.VACATION)
                        .build();
        when(userAbsenceRepository.findByIdAndUser_Id(8L, USER_ID))
                .thenReturn(Optional.of(absence));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        OTHER_ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(false);
        org.mockito.Mockito.doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(USER_ID, ORG_ID);
        when(organizationRepository.getReferenceById(ORG_ID)).thenReturn(writableOrg);
        when(organizationRepository.getReferenceById(OTHER_ORG_ID)).thenReturn(protectedOrg);
        when(userAbsenceRepository.save(absence)).thenReturn(absence);
        when(mapper.toDto(absence)).thenReturn(UserAbsenceDto.builder().id(8L).build());

        userAbsenceService.updateAbsence(USER_ID, 8L, request);

        assertThat(absence.getOrganizationLinks())
                .extracting(link -> link.getOrganization().getId())
                .containsExactlyInAnyOrder(ORG_ID, OTHER_ORG_ID);
    }

    @Test
    void updateAbsence_rejectsAbsenceFromDifferentOrganization() {
        UpdateUserAbsenceRequest request =
                UpdateUserAbsenceRequest.builder()
                        .organizationIds(List.of(ORG_ID))
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 11))
                        .reason(AbsenceReason.VACATION)
                        .build();
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(true);
        when(userAbsenceRepository.findByIdAndUser_IdAndOrganizationLinks_Organization_Id(
                        8L, USER_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                userAbsenceService.updateAbsenceInOrganization(
                                        USER_ID, ORG_ID, 8L, request))
                .isInstanceOf(UserAbsenceNotFoundException.class);
    }

    @Test
    void updateAbsenceInOrganization_requiresPathOrganizationInRequest() {
        UpdateUserAbsenceRequest request =
                UpdateUserAbsenceRequest.builder()
                        .organizationIds(List.of(OTHER_ORG_ID))
                        .startDate(LocalDate.of(2026, 6, 10))
                        .endDate(LocalDate.of(2026, 6, 11))
                        .reason(AbsenceReason.VACATION)
                        .build();

        assertThatThrownBy(
                        () ->
                                userAbsenceService.updateAbsenceInOrganization(
                                        USER_ID, ORG_ID, 8L, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessageContaining("Path organization must be included");
    }

    @Test
    void deleteAbsenceInOrganization_requiresAbsenceInOrganization() {
        when(userAbsenceRepository.findByIdAndUser_IdAndOrganizationLinks_Organization_Id(
                        8L, USER_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> userAbsenceService.deleteAbsenceInOrganization(USER_ID, ORG_ID, 8L))
                .isInstanceOf(UserAbsenceNotFoundException.class);
    }

    @Test
    void createAbsenceInOrganization_requiresPathOrganizationInRequest() {
        CreateUserAbsenceRequest request = validCreateRequest();
        assertThatThrownBy(
                        () ->
                                userAbsenceService.createAbsenceInOrganization(
                                        USER_ID, OTHER_ORG_ID, request))
                .isInstanceOf(AbsenceValidationException.class)
                .hasMessageContaining("Path organization must be included");
    }

    @Test
    void deleteAbsence_requiresOwnedAbsenceAndDeletesIt() {
        when(userAbsenceRepository.findByIdAndUser_Id(8L, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userAbsenceService.deleteAbsence(USER_ID, 8L))
                .isInstanceOf(UserAbsenceNotFoundException.class);

        UserAbsence absence = UserAbsence.builder().id(8L).build();
        when(userAbsenceRepository.findByIdAndUser_Id(8L, USER_ID))
                .thenReturn(Optional.of(absence));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.ABSENCE_WRITE))
                .thenReturn(true);
        org.mockito.Mockito.doNothing()
                .when(userService)
                .assertUserBelongsToOrganization(USER_ID, ORG_ID);
        userAbsenceService.deleteAbsence(USER_ID, 8L);
        verify(userAbsenceRepository).delete(absence);

        Organization organization = Organization.builder().id(ORG_ID).tenantId(TENANT_ID).build();
        UserAbsenceOrganization link =
                UserAbsenceOrganization.builder().organization(organization).build();
        UserAbsence orgScopedAbsence =
                UserAbsence.builder()
                        .id(10L)
                        .organizationLinks(new java.util.ArrayList<>(List.of(link)))
                        .build();
        when(userAbsenceRepository.findByIdAndUser_IdAndOrganizationLinks_Organization_Id(
                        10L, USER_ID, ORG_ID))
                .thenReturn(Optional.of(orgScopedAbsence));
        userAbsenceService.deleteAbsenceInOrganization(USER_ID, ORG_ID, 10L);
        verify(userAbsenceRepository).delete(orgScopedAbsence);
    }
}
