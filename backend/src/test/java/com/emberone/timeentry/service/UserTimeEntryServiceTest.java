/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.domain.OrganizationGroupType;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.timeentry.domain.UserTimeEntry;
import com.emberone.timeentry.dto.CreateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UpdateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.exception.TimeEntryValidationException;
import com.emberone.timeentry.exception.UserTimeEntryNotFoundException;
import com.emberone.timeentry.mapper.UserTimeEntryDtoAssembler;
import com.emberone.timeentry.mapper.UserTimeEntryMapper;
import com.emberone.timeentry.repository.UserTimeEntryRepository;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserTimeEntryServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORG_ID = 10L;
    private static final Long OTHER_ORG_ID = 11L;

    @Mock private UserTimeEntryRepository timeEntryRepository;

    @Mock private UserRepository userRepository;

    @Mock private OrganizationRepository organizationRepository;

    @Mock private OrganizationGroupRepository organizationGroupRepository;

    @Mock private OrganizationAccessService organizationAccessService;

    @Mock private UserService userService;

    @Mock private UserTimeEntryMapper mapper;

    @Mock private UserTimeEntryDtoAssembler dtoAssembler;

    @InjectMocks private UserTimeEntryService userTimeEntryService;

    @Test
    void listTimeEntriesForUser_throwsWhenUserMissing() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> userTimeEntryService.listTimeEntriesForUser(USER_ID, null, null))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void listTimeEntriesForOrganization_throwsWhenOrganizationMissing() {
        when(organizationRepository.existsById(ORG_ID)).thenReturn(false);

        assertThatThrownBy(
                        () ->
                                userTimeEntryService.listTimeEntriesForOrganization(
                                        ORG_ID, null, null))
                .isInstanceOf(OrganizationNotFoundException.class);
    }

    @Test
    void getTimeEntryForOrganization_returnsDtoWhenFound() {
        UserTimeEntry entry = UserTimeEntry.builder().id(2L).build();
        UserTimeEntryDto dto = UserTimeEntryDto.builder().id(2L).build();
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(timeEntryRepository.findByIdAndOrganization_Id(2L, ORG_ID))
                .thenReturn(Optional.of(entry));
        when(dtoAssembler.toDto(entry)).thenReturn(dto);

        assertThat(userTimeEntryService.getTimeEntryForOrganization(ORG_ID, 2L)).isEqualTo(dto);
    }

    @Test
    void getTimeEntryForOrganization_throwsWhenMissing() {
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(timeEntryRepository.findByIdAndOrganization_Id(2L, ORG_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userTimeEntryService.getTimeEntryForOrganization(ORG_ID, 2L))
                .isInstanceOf(UserTimeEntryNotFoundException.class);
    }

    @Test
    void createTimeEntry_rejectsEndBeforeStart() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(5L).build();
        Instant start = LocalDateTime.of(2026, 6, 10, 10, 0).toInstant(ZoneOffset.UTC);
        Instant end = start.minusSeconds(3600);

        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.TIME_ENTRY_WRITE))
                .thenReturn(true);

        CreateUserTimeEntryRequest request =
                CreateUserTimeEntryRequest.builder()
                        .organizationId(ORG_ID)
                        .startTime(start)
                        .endTime(end)
                        .build();

        assertThatThrownBy(() -> userTimeEntryService.createTimeEntry(USER_ID, request))
                .isInstanceOf(TimeEntryValidationException.class)
                .hasMessageContaining("End time must not be before start time");
    }

    @Test
    void createTimeEntry_rejectsOrganizationGroupFromDifferentOrganization() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(5L).build();
        Organization otherOrganization =
                Organization.builder().id(OTHER_ORG_ID).tenantId(5L).build();
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(3L)
                        .organization(otherOrganization)
                        .tenantId(5L)
                        .build();
        Instant start = LocalDateTime.of(2026, 6, 10, 10, 0).toInstant(ZoneOffset.UTC);

        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.TIME_ENTRY_WRITE))
                .thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(organizationGroupRepository.findById(3L)).thenReturn(Optional.of(group));

        CreateUserTimeEntryRequest request =
                CreateUserTimeEntryRequest.builder()
                        .organizationId(ORG_ID)
                        .organizationGroupId(3L)
                        .startTime(start)
                        .endTime(start.plusSeconds(2 * 3600))
                        .build();

        assertThatThrownBy(() -> userTimeEntryService.createTimeEntry(USER_ID, request))
                .isInstanceOf(TimeEntryValidationException.class)
                .hasMessageContaining(
                        "Organization group does not belong to the selected organization");
    }

    @Test
    void createTimeEntry_persistsOpenEntryWithoutGroup() {
        User user = User.builder().id(USER_ID).tenantId(5L).build();
        Organization organization = Organization.builder().id(ORG_ID).tenantId(5L).build();
        Instant start = LocalDateTime.of(2026, 6, 10, 10, 0).toInstant(ZoneOffset.UTC);
        CreateUserTimeEntryRequest request =
                CreateUserTimeEntryRequest.builder()
                        .organizationId(ORG_ID)
                        .startTime(start)
                        .description("Training")
                        .build();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.TIME_ENTRY_WRITE))
                .thenReturn(true);
        when(timeEntryRepository.save(any(UserTimeEntry.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        UserTimeEntryDto dto = UserTimeEntryDto.builder().build();
        when(mapper.toDto(any(UserTimeEntry.class))).thenReturn(dto);

        assertThat(userTimeEntryService.createTimeEntry(USER_ID, request)).isSameAs(dto);
        var captor = org.mockito.ArgumentCaptor.forClass(UserTimeEntry.class);
        verify(timeEntryRepository).save(captor.capture());
        assertThat(captor.getValue())
                .satisfies(
                        entry -> {
                            assertThat(entry.getUser()).isSameAs(user);
                            assertThat(entry.getOrganization()).isSameAs(organization);
                            assertThat(entry.getOrganizationGroup()).isNull();
                            assertThat(entry.getTenantId()).isEqualTo(5L);
                            assertThat(entry.getStartTime()).isEqualTo(start);
                            assertThat(entry.getEndTime()).isNull();
                            assertThat(entry.getDescription()).isEqualTo("Training");
                        });
    }

    @Test
    void updateTimeEntry_updatesFieldsAndReturnsLocalizedDto() {
        UserTimeEntry entry = UserTimeEntry.builder().id(9L).tenantId(5L).build();
        Organization organization = Organization.builder().id(ORG_ID).tenantId(5L).build();
        OrganizationGroup group =
                OrganizationGroup.builder()
                        .id(3L)
                        .name("crew")
                        .type(OrganizationGroupType.CUSTOM)
                        .organization(organization)
                        .build();
        Instant start = LocalDateTime.of(2026, 6, 11, 8, 0).toInstant(ZoneOffset.UTC);
        UpdateUserTimeEntryRequest request =
                UpdateUserTimeEntryRequest.builder()
                        .organizationId(ORG_ID)
                        .organizationGroupId(3L)
                        .startTime(start)
                        .endTime(start.plusSeconds(2 * 3600))
                        .description("Updated")
                        .build();
        when(timeEntryRepository.findByIdAndUser_Id(9L, USER_ID)).thenReturn(Optional.of(entry));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.TIME_ENTRY_WRITE))
                .thenReturn(true);
        when(organizationGroupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(timeEntryRepository.save(entry)).thenReturn(entry);
        UserTimeEntryDto dto = UserTimeEntryDto.builder().build();
        when(mapper.toDto(entry)).thenReturn(dto);

        assertThat(userTimeEntryService.updateTimeEntry(USER_ID, 9L, request)).isSameAs(dto);
        assertThat(entry.getOrganization()).isSameAs(organization);
        assertThat(entry.getOrganizationGroup()).isSameAs(group);
        assertThat(entry.getStartTime()).isEqualTo(start);
        assertThat(entry.getEndTime()).isEqualTo(start.plusSeconds(2 * 3600));
        assertThat(entry.getDescription()).isEqualTo("Updated");
    }

    @Test
    void updateTimeEntryInOrganization_rejectsEntryFromDifferentOrganization() {
        Organization organization = Organization.builder().id(ORG_ID).tenantId(5L).build();
        UpdateUserTimeEntryRequest request =
                UpdateUserTimeEntryRequest.builder()
                        .organizationId(ORG_ID)
                        .startTime(LocalDateTime.of(2026, 6, 11, 8, 0).toInstant(ZoneOffset.UTC))
                        .build();
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(organization));
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.TIME_ENTRY_WRITE))
                .thenReturn(true);
        when(timeEntryRepository.findByIdAndUser_IdAndOrganization_Id(9L, USER_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                userTimeEntryService.updateTimeEntryInOrganization(
                                        USER_ID, ORG_ID, 9L, request))
                .isInstanceOf(UserTimeEntryNotFoundException.class);
    }

    @Test
    void deleteTimeEntryInOrganization_requiresEntryInOrganization() {
        when(timeEntryRepository.findByIdAndUser_IdAndOrganization_Id(9L, USER_ID, ORG_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                userTimeEntryService.deleteTimeEntryInOrganization(
                                        USER_ID, ORG_ID, 9L))
                .isInstanceOf(UserTimeEntryNotFoundException.class);
    }

    @Test
    void deleteTimeEntry_requiresOwnedEntryAndDeletesIt() {
        when(timeEntryRepository.findByIdAndUser_Id(9L, USER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userTimeEntryService.deleteTimeEntry(USER_ID, 9L))
                .isInstanceOf(UserTimeEntryNotFoundException.class);

        UserTimeEntry entry =
                UserTimeEntry.builder()
                        .id(9L)
                        .organization(Organization.builder().id(ORG_ID).build())
                        .build();
        when(timeEntryRepository.findByIdAndUser_Id(9L, USER_ID)).thenReturn(Optional.of(entry));
        when(organizationRepository.existsById(ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        ORG_ID, PermissionType.TIME_ENTRY_WRITE))
                .thenReturn(true);
        userTimeEntryService.deleteTimeEntry(USER_ID, 9L);
        verify(timeEntryRepository).delete(entry);

        UserTimeEntry orgScopedEntry =
                UserTimeEntry.builder()
                        .id(10L)
                        .organization(Organization.builder().id(ORG_ID).build())
                        .build();
        when(timeEntryRepository.findByIdAndUser_IdAndOrganization_Id(10L, USER_ID, ORG_ID))
                .thenReturn(Optional.of(orgScopedEntry));
        userTimeEntryService.deleteTimeEntryInOrganization(USER_ID, ORG_ID, 10L);
        verify(timeEntryRepository).delete(orgScopedEntry);
    }

    @Test
    void createTimeEntryInOrganization_rejectsOrganizationMismatchWithPath() {
        Instant start = LocalDateTime.of(2026, 6, 10, 10, 0).toInstant(ZoneOffset.UTC);
        CreateUserTimeEntryRequest request =
                CreateUserTimeEntryRequest.builder()
                        .organizationId(OTHER_ORG_ID)
                        .startTime(start)
                        .build();

        assertThatThrownBy(
                        () ->
                                userTimeEntryService.createTimeEntryInOrganization(
                                        USER_ID, ORG_ID, request))
                .isInstanceOf(TimeEntryValidationException.class)
                .hasMessageContaining("does not match the path parameter");
    }

    @Test
    void createTimeEntry_rejectsUnauthorizedOrganization() {
        Instant start = LocalDateTime.of(2026, 6, 10, 10, 0).toInstant(ZoneOffset.UTC);
        when(organizationRepository.existsById(OTHER_ORG_ID)).thenReturn(true);
        when(organizationAccessService.hasOrganizationPermission(
                        OTHER_ORG_ID, PermissionType.TIME_ENTRY_WRITE))
                .thenReturn(false);
        org.mockito.Mockito.doThrow(
                        new IllegalArgumentException(
                                "User does not belong to the specified organization"))
                .when(userService)
                .assertUserBelongsToOrganization(USER_ID, OTHER_ORG_ID);

        CreateUserTimeEntryRequest request =
                CreateUserTimeEntryRequest.builder()
                        .organizationId(OTHER_ORG_ID)
                        .startTime(start)
                        .build();

        assertThatThrownBy(() -> userTimeEntryService.createTimeEntry(USER_ID, request))
                .isInstanceOf(TimeEntryValidationException.class)
                .hasMessageContaining("time entry write access");
    }
}
