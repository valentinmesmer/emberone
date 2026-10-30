/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.timeentry.service;

import com.emberone.organization.domain.Organization;
import com.emberone.organization.exception.OrganizationNotFoundException;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.organizationgroup.domain.OrganizationGroup;
import com.emberone.organizationgroup.exception.OrganizationGroupNotFoundException;
import com.emberone.organizationgroup.repository.OrganizationGroupRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.timeentry.domain.UserTimeEntry;
import com.emberone.timeentry.dto.CreateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UpdateUserTimeEntryRequest;
import com.emberone.timeentry.dto.UserTimeEntryDto;
import com.emberone.timeentry.dto.UserTimeEntryPageDto;
import com.emberone.timeentry.exception.TimeEntryValidationException;
import com.emberone.timeentry.exception.UserTimeEntryNotFoundException;
import com.emberone.timeentry.list.TimeEntryListDefinition;
import com.emberone.timeentry.list.TimeEntryListFilter;
import com.emberone.timeentry.list.TimeEntryListSpecs;
import com.emberone.timeentry.mapper.UserTimeEntryDtoAssembler;
import com.emberone.timeentry.mapper.UserTimeEntryMapper;
import com.emberone.timeentry.repository.UserTimeEntryRepository;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTimeEntryService {

    private final UserTimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationGroupRepository organizationGroupRepository;
    private final OrganizationAccessService organizationAccessService;
    private final UserService userService;
    private final UserTimeEntryMapper mapper;
    private final UserTimeEntryDtoAssembler dtoAssembler;

    @Transactional(readOnly = true)
    public UserTimeEntryDto getTimeEntryForUser(Long userId, Long entryId) {
        return dtoAssembler.toDto(requireOwnedEntry(userId, entryId));
    }

    @Transactional(readOnly = true)
    public UserTimeEntryDto getTimeEntryForOrganization(Long organizationId, Long entryId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        UserTimeEntry entry =
                timeEntryRepository
                        .findByIdAndOrganization_Id(entryId, organizationId)
                        .orElseThrow(() -> new UserTimeEntryNotFoundException(entryId));
        return dtoAssembler.toDto(entry);
    }

    @Transactional(readOnly = true)
    public UserTimeEntryPageDto listTimeEntriesForUser(
            Long userId, ListQuery query, TimeEntryListFilter filter) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return listTimeEntries(
                Specification.allOf(
                        TimeEntryListSpecs.forUser(userId),
                        TimeEntryListSpecs.organizationsFilter(filter.organizationIds()),
                        filter.selfList()
                                ? TimeEntryListSpecs.startInCalendarYears(filter.years())
                                : TimeEntryListSpecs.startAtRange(
                                        filter.resolvedFrom(), filter.resolvedTo()),
                        TimeEntryListDefinition.INSTANCE.searchSpecification(query)),
                query,
                false);
    }

    @Transactional(readOnly = true)
    public UserTimeEntryPageDto listTimeEntriesForOrganization(
            Long organizationId, ListQuery query, TimeEntryListFilter filter) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return listTimeEntries(
                Specification.allOf(
                        TimeEntryListSpecs.forOrganization(organizationId),
                        TimeEntryListSpecs.startAtRange(filter.resolvedFrom(), filter.resolvedTo()),
                        TimeEntryListDefinition.INSTANCE.searchSpecification(query)),
                query,
                true);
    }

    @Transactional(readOnly = true)
    public UserTimeEntryPageDto listTimeEntriesForUserInOrganization(
            Long userId, Long organizationId, ListQuery query, TimeEntryListFilter filter) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return listTimeEntries(
                Specification.allOf(
                        TimeEntryListSpecs.forUser(userId),
                        TimeEntryListSpecs.forOrganization(organizationId),
                        TimeEntryListSpecs.organizationsFilter(filter.organizationIds()),
                        filter.selfList()
                                ? TimeEntryListSpecs.startInCalendarYears(filter.years())
                                : TimeEntryListSpecs.startAtRange(
                                        filter.resolvedFrom(), filter.resolvedTo()),
                        TimeEntryListDefinition.INSTANCE.searchSpecification(query)),
                query,
                false);
    }

    @Transactional(readOnly = true)
    public List<Organization> listDistinctOrganizationsForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return timeEntryRepository.findDistinctOrganizationRowsByUserId(userId).stream()
                .map(row -> Organization.builder().id((Long) row[0]).name((String) row[1]).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> listDistinctYearsForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        return listDistinctYearsFromStartTimes(timeEntryRepository.findStartTimesByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<Organization> listDistinctOrganizationsForUserInOrganization(
            Long userId, Long organizationId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return timeEntryRepository
                .findDistinctOrganizationRowsByUserIdAndOrganizationId(userId, organizationId)
                .stream()
                .map(row -> Organization.builder().id((Long) row[0]).name((String) row[1]).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> listDistinctYearsForUserInOrganization(Long userId, Long organizationId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        return listDistinctYearsFromStartTimes(
                timeEntryRepository.findStartTimesByUserIdAndOrganizationId(
                        userId, organizationId));
    }

    private List<Integer> listDistinctYearsFromStartTimes(List<Instant> startTimes) {
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        Set<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        for (Instant start : startTimes) {
            years.add(start.atZone(zone).getYear());
        }
        years.add(OrganizationWallClockTime.today().getYear());
        return List.copyOf(years);
    }

    private UserTimeEntryPageDto listTimeEntries(
            Specification<UserTimeEntry> spec, ListQuery query, boolean includeUser) {
        Pageable pageable = TimeEntryListDefinition.INSTANCE.pageable(query);
        Page<UserTimeEntry> entityPage = timeEntryRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(UserTimeEntry::getId).toList();
        List<UserTimeEntry> hydrated =
                ids.isEmpty()
                        ? List.of()
                        : includeUser
                                ? timeEntryRepository.findWithUserDetailsByIdIn(ids)
                                : timeEntryRepository.findWithDetailsByIdIn(ids);
        List<UserTimeEntry> ordered = PageReorder.reorderByIds(ids, hydrated, UserTimeEntry::getId);
        Page<UserTimeEntry> result = PageReorder.pageOf(ordered, entityPage);
        return dtoAssembler.toPageDto(result);
    }

    @Transactional(readOnly = true)
    public double sumHoursForUserInYear(Long userId, int year) {
        ZoneId zone = OrganizationWallClockTime.organizationZone();
        Instant from = LocalDate.of(year, 1, 1).atStartOfDay(zone).toInstant();
        Instant toExclusive = LocalDate.of(year + 1, 1, 1).atStartOfDay(zone).toInstant();
        return timeEntryRepository
                .findByUser_IdAndStartTimeBetween(userId, from, toExclusive)
                .stream()
                .filter(e -> e.getEndTime() != null)
                .mapToDouble(
                        e -> Duration.between(e.getStartTime(), e.getEndTime()).toMinutes() / 60.0)
                .filter(h -> h > 0)
                .sum();
    }

    @Transactional
    public UserTimeEntryDto createTimeEntry(Long userId, CreateUserTimeEntryRequest request) {
        validateOrganizationWriteAccess(userId, request.getOrganizationId());
        Organization organization = requireOrganization(request.getOrganizationId());
        OrganizationGroup group =
                resolveOrganizationGroup(request.getOrganizationGroupId(), organization);
        validateTimes(request.getStartTime(), request.getEndTime());

        User user = requireUser(userId);
        UserTimeEntry entry = newTimeEntry(user, organization, group, request);
        UserTimeEntry saved = timeEntryRepository.save(entry);
        log.info("Created time entry {}", saved.getId());
        return mapper.toDto(saved);
    }

    @Transactional
    public UserTimeEntryDto createTimeEntryInOrganization(
            Long userId, Long organizationId, CreateUserTimeEntryRequest request) {
        requirePathOrganizationMatches(organizationId, request.getOrganizationId());
        validateOrganizationWriteAccess(userId, request.getOrganizationId());
        Organization organization = requireOrganization(request.getOrganizationId());
        OrganizationGroup group =
                resolveOrganizationGroup(request.getOrganizationGroupId(), organization);
        validateTimes(request.getStartTime(), request.getEndTime());

        User user = requireUser(userId);
        UserTimeEntry entry = newTimeEntry(user, organization, group, request);
        UserTimeEntry saved = timeEntryRepository.save(entry);
        log.info(
                "Created time entry {} for user {} in organization {}",
                saved.getId(),
                userId,
                organizationId);
        return mapper.toDto(saved);
    }

    @Transactional
    public UserTimeEntryDto updateTimeEntry(
            Long userId, Long entryId, UpdateUserTimeEntryRequest request) {
        validateOrganizationWriteAccess(userId, request.getOrganizationId());
        Organization organization = requireOrganization(request.getOrganizationId());
        OrganizationGroup group =
                resolveOrganizationGroup(request.getOrganizationGroupId(), organization);
        validateTimes(request.getStartTime(), request.getEndTime());

        UserTimeEntry entry = requireOwnedEntry(userId, entryId);
        applyTimeEntryUpdate(entry, organization, group, request);

        UserTimeEntry saved = timeEntryRepository.save(entry);
        log.info("Updated time entry {}", entryId);
        return mapper.toDto(saved);
    }

    @Transactional
    public UserTimeEntryDto updateTimeEntryInOrganization(
            Long userId, Long organizationId, Long entryId, UpdateUserTimeEntryRequest request) {
        requirePathOrganizationMatches(organizationId, request.getOrganizationId());
        validateOrganizationWriteAccess(userId, request.getOrganizationId());
        Organization organization = requireOrganization(request.getOrganizationId());
        OrganizationGroup group =
                resolveOrganizationGroup(request.getOrganizationGroupId(), organization);
        validateTimes(request.getStartTime(), request.getEndTime());

        UserTimeEntry entry = requireEntryInOrganization(userId, organizationId, entryId);
        applyTimeEntryUpdate(entry, organization, group, request);

        UserTimeEntry saved = timeEntryRepository.save(entry);
        log.info(
                "Updated time entry {} for user {} in organization {}",
                entryId,
                userId,
                organizationId);
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteTimeEntry(Long userId, Long entryId) {
        UserTimeEntry entry = requireOwnedEntry(userId, entryId);
        validateOrganizationWriteAccess(userId, entry.getOrganization().getId());
        timeEntryRepository.delete(entry);
        log.info("Deleted time entry {}", entryId);
    }

    @Transactional
    public void deleteTimeEntryInOrganization(Long userId, Long organizationId, Long entryId) {
        UserTimeEntry entry = requireEntryInOrganization(userId, organizationId, entryId);
        validateOrganizationWriteAccess(userId, entry.getOrganization().getId());
        timeEntryRepository.delete(entry);
        log.info(
                "Deleted time entry {} for user {} in organization {}",
                entryId,
                userId,
                organizationId);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private Organization requireOrganization(Long organizationId) {
        return organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    private UserTimeEntry requireOwnedEntry(Long userId, Long entryId) {
        return timeEntryRepository
                .findByIdAndUser_Id(entryId, userId)
                .orElseThrow(() -> new UserTimeEntryNotFoundException(entryId));
    }

    private UserTimeEntry requireEntryInOrganization(
            Long userId, Long organizationId, Long entryId) {
        return timeEntryRepository
                .findByIdAndUser_IdAndOrganization_Id(entryId, userId, organizationId)
                .orElseThrow(() -> new UserTimeEntryNotFoundException(entryId));
    }

    private void requirePathOrganizationMatches(
            Long pathOrganizationId, Long requestOrganizationId) {
        if (!pathOrganizationId.equals(requestOrganizationId)) {
            log.warn(
                    "Organization ID mismatch: expected {}, but got {}",
                    pathOrganizationId,
                    requestOrganizationId);
            throw new TimeEntryValidationException(
                    "Organization ID in request does not match the path parameter.");
        }
    }

    private OrganizationGroup resolveOrganizationGroup(
            Long organizationGroupId, Organization organization) {
        if (organizationGroupId == null) {
            return null;
        }
        OrganizationGroup group =
                organizationGroupRepository
                        .findById(organizationGroupId)
                        .orElseThrow(
                                () -> new OrganizationGroupNotFoundException(organizationGroupId));
        if (!group.getOrganization().getId().equals(organization.getId())) {
            throw new TimeEntryValidationException(
                    "Organization group does not belong to the selected organization.");
        }
        return group;
    }

    private UserTimeEntry newTimeEntry(
            User user,
            Organization organization,
            OrganizationGroup group,
            CreateUserTimeEntryRequest request) {
        return UserTimeEntry.builder()
                .user(user)
                .tenantId(user.getTenantId())
                .organization(organization)
                .organizationGroup(group)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .description(request.getDescription())
                .build();
    }

    private void applyTimeEntryUpdate(
            UserTimeEntry entry,
            Organization organization,
            OrganizationGroup group,
            UpdateUserTimeEntryRequest request) {
        entry.setOrganization(organization);
        entry.setOrganizationGroup(group);
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setDescription(request.getDescription());
    }

    private void validateTimes(Instant startTime, Instant endTime) {
        if (endTime != null && endTime.isBefore(startTime)) {
            throw new TimeEntryValidationException("End time must not be before start time.");
        }
    }

    private void validateOrganizationWriteAccess(Long userId, Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new OrganizationNotFoundException(organizationId);
        }
        if (organizationAccessService.hasOrganizationPermission(
                organizationId, PermissionType.TIME_ENTRY_WRITE)) {
            try {
                userService.assertUserBelongsToOrganization(userId, organizationId);
            } catch (IllegalArgumentException e) {
                throw new TimeEntryValidationException(
                        "You must belong to the selected organization to manage time entries.");
            }
            return;
        }
        try {
            userService.assertUserBelongsToOrganization(userId, organizationId);
        } catch (IllegalArgumentException e) {
            throw new TimeEntryValidationException(
                    "You do not have time entry write access for the selected organization.");
        }
    }
}
