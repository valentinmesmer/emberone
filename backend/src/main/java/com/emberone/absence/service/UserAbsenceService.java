/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.absence.service;

import com.emberone.absence.domain.AbsenceReason;
import com.emberone.absence.domain.UserAbsence;
import com.emberone.absence.domain.UserAbsenceOrganization;
import com.emberone.absence.dto.CreateUserAbsenceRequest;
import com.emberone.absence.dto.UpdateUserAbsenceRequest;
import com.emberone.absence.dto.UserAbsenceDto;
import com.emberone.absence.dto.UserAbsencePageDto;
import com.emberone.absence.exception.AbsenceValidationException;
import com.emberone.absence.exception.UserAbsenceNotFoundException;
import com.emberone.absence.list.AbsenceListDefinition;
import com.emberone.absence.list.AbsenceListFilter;
import com.emberone.absence.list.AbsenceListSpecs;
import com.emberone.absence.mapper.UserAbsenceDtoAssembler;
import com.emberone.absence.mapper.UserAbsenceMapper;
import com.emberone.absence.repository.UserAbsenceRepository;
import com.emberone.organization.domain.Organization;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.permission.domain.PermissionType;
import com.emberone.platform.security.OrganizationAccessService;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.PageReorder;
import com.emberone.user.domain.User;
import com.emberone.user.exception.UserNotFoundException;
import com.emberone.user.repository.UserRepository;
import com.emberone.user.service.UserService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
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
public class UserAbsenceService {

    private final UserAbsenceRepository userAbsenceRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationAccessService organizationAccessService;
    private final UserService userService;
    private final UserAbsenceMapper mapper;
    private final UserAbsenceDtoAssembler userAbsenceDtoAssembler;

    private static Set<Long> normalizeOrganizationIds(List<Long> organizationIds) {
        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        for (Long id : organizationIds) {
            if (id != null) {
                ordered.add(id);
            }
        }
        if (ordered.isEmpty()) {
            throw new AbsenceValidationException("At least one organization is required.");
        }
        return ordered;
    }

    private static void validateDateRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new AbsenceValidationException("End date must not be before start date.");
        }
    }

    @Transactional(readOnly = true)
    public UserAbsenceDto getAbsenceForUser(Long userId, Long absenceId) {
        return mapper.toDto(requireOwnedAbsence(userId, absenceId));
    }

    @Transactional(readOnly = true)
    public UserAbsenceDto getAbsenceForOrganization(Long organizationId, Long absenceId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new AbsenceValidationException("Organization not found.");
        }
        UserAbsence absence =
                userAbsenceRepository
                        .findByIdAndOrganizationId(absenceId, organizationId)
                        .orElseThrow(() -> new UserAbsenceNotFoundException(absenceId));
        return mapper.toDto(absence);
    }

    @Transactional(readOnly = true)
    public UserAbsencePageDto listAbsencesForUser(
            Long userId, ListQuery query, AbsenceListFilter filter) {
        ensureUserExists(userId);
        return listAbsences(
                Specification.allOf(
                        AbsenceListSpecs.forUser(userId),
                        AbsenceListSpecs.periodsFilter(filter.periods()),
                        AbsenceListSpecs.reasonsFilter(filter.reasons()),
                        AbsenceListSpecs.linkedToAnyOrganization(filter.organizationIds()),
                        AbsenceListSpecs.overlappingYears(filter.years()),
                        AbsenceListDefinition.INSTANCE.searchSpecification(query)),
                query,
                filter);
    }

    @Transactional(readOnly = true)
    public UserAbsencePageDto listAbsencesForUserInOrganization(
            Long userId, Long organizationId, ListQuery query, AbsenceListFilter filter) {
        ensureUserExists(userId);
        if (!organizationRepository.existsById(organizationId)) {
            throw new AbsenceValidationException("Organization not found.");
        }
        return listAbsences(
                Specification.allOf(
                        AbsenceListSpecs.forUser(userId),
                        AbsenceListSpecs.linkedToOrganization(organizationId),
                        AbsenceListSpecs.periodsFilter(filter.periods()),
                        AbsenceListSpecs.reasonsFilter(filter.reasons()),
                        AbsenceListSpecs.linkedToAnyOrganization(filter.organizationIds()),
                        AbsenceListSpecs.overlappingYears(filter.years()),
                        AbsenceListDefinition.INSTANCE.searchSpecification(query)),
                query,
                filter);
    }

    @Transactional(readOnly = true)
    public UserAbsencePageDto listActiveAbsencesForOrganization(
            Long organizationId, ListQuery query, AbsenceListFilter filter) {
        return listAbsences(
                Specification.allOf(
                        AbsenceListSpecs.linkedToOrganization(organizationId),
                        AbsenceListSpecs.periodsFilter(filter.periods()),
                        AbsenceListSpecs.reasonsFilter(filter.reasons()),
                        AbsenceListSpecs.overlappingYears(filter.years()),
                        AbsenceListDefinition.INSTANCE.searchSpecification(query)),
                query,
                filter);
    }

    private UserAbsencePageDto listAbsences(
            Specification<UserAbsence> spec, ListQuery query, AbsenceListFilter filter) {
        Pageable pageable = AbsenceListDefinition.INSTANCE.pageable(query, filter.resolveSort());
        Page<UserAbsence> entityPage = userAbsenceRepository.findAll(spec, pageable);
        List<Long> ids = entityPage.getContent().stream().map(UserAbsence::getId).toList();
        List<UserAbsence> hydrated =
                ids.isEmpty() ? List.of() : userAbsenceRepository.findWithOrganizationsByIdIn(ids);
        List<UserAbsence> ordered = PageReorder.reorderByIds(ids, hydrated, UserAbsence::getId);
        Page<UserAbsence> result = PageReorder.pageOf(ordered, entityPage);
        return userAbsenceDtoAssembler.toPageDto(result);
    }

    @Transactional
    public UserAbsenceDto createAbsence(Long userId, CreateUserAbsenceRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Set<Long> organizationIds = normalizeOrganizationIds(request.getOrganizationIds());
        validateOrganizationWriteAccess(userId, organizationIds);

        User user = requireUser(userId);
        UserAbsence absence = newAbsence(user, request, organizationIds);
        UserAbsence saved = userAbsenceRepository.save(absence);
        log.info("Created user absence {}", saved.getId());
        return mapper.toDto(saved);
    }

    @Transactional
    public UserAbsenceDto createAbsenceInOrganization(
            Long userId, Long organizationId, CreateUserAbsenceRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Set<Long> organizationIds = normalizeOrganizationIds(request.getOrganizationIds());
        requirePathOrganizationIncluded(organizationId, organizationIds);
        validateOrganizationWriteAccess(userId, organizationIds);

        User user = requireUser(userId);
        UserAbsence absence = newAbsence(user, request, organizationIds);
        UserAbsence saved = userAbsenceRepository.save(absence);
        log.info(
                "Created user absence {} for user {} in organization {}",
                saved.getId(),
                userId,
                organizationId);
        return mapper.toDto(saved);
    }

    @Transactional
    public UserAbsenceDto updateAbsence(
            Long userId, Long absenceId, UpdateUserAbsenceRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Set<Long> organizationIds = normalizeOrganizationIds(request.getOrganizationIds());
        validateOrganizationWriteAccess(userId, organizationIds);

        UserAbsence absence = requireOwnedAbsence(userId, absenceId);
        mapper.updateFromRequest(request, absence);
        mergeOrganizationLinks(absence, organizationIds);

        UserAbsence saved = userAbsenceRepository.save(absence);
        log.info("Updated user absence {}", absenceId);
        return mapper.toDto(saved);
    }

    @Transactional
    public UserAbsenceDto updateAbsenceInOrganization(
            Long userId, Long organizationId, Long absenceId, UpdateUserAbsenceRequest request) {
        validateDateRange(request.getStartDate(), request.getEndDate());
        Set<Long> organizationIds = normalizeOrganizationIds(request.getOrganizationIds());
        requirePathOrganizationIncluded(organizationId, organizationIds);
        validateOrganizationWriteAccess(userId, organizationIds);

        UserAbsence absence = requireAbsenceInOrganization(userId, organizationId, absenceId);
        mapper.updateFromRequest(request, absence);
        mergeOrganizationLinks(absence, organizationIds);

        UserAbsence saved = userAbsenceRepository.save(absence);
        log.info(
                "Updated user absence {} for user {} in organization {}",
                absenceId,
                userId,
                organizationId);
        return mapper.toDto(saved);
    }

    @Transactional
    public void deleteAbsence(Long userId, Long absenceId) {
        UserAbsence absence = requireOwnedAbsence(userId, absenceId);
        validateOrganizationWriteAccess(userId, organizationIdsFromAbsence(absence));
        userAbsenceRepository.delete(absence);
        log.info("Deleted user absence {}", absenceId);
    }

    @Transactional
    public void deleteAbsenceInOrganization(Long userId, Long organizationId, Long absenceId) {
        UserAbsence absence = requireAbsenceInOrganization(userId, organizationId, absenceId);
        validateOrganizationWriteAccess(userId, organizationIdsFromAbsence(absence));
        userAbsenceRepository.delete(absence);
        log.info(
                "Deleted user absence {} for user {} in organization {}",
                absenceId,
                userId,
                organizationId);
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void ensureUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
    }

    private UserAbsence newAbsence(
            User user, CreateUserAbsenceRequest request, Set<Long> organizationIds) {
        UserAbsence absence = UserAbsence.builder().build();
        mapper.applyCreate(request, absence);
        absence.setUser(user);
        absence.setTenantId(user.getTenantId());
        setOrganizationLinks(absence, organizationIds);
        return absence;
    }

    private UserAbsence requireOwnedAbsence(Long userId, Long absenceId) {
        return userAbsenceRepository
                .findByIdAndUser_Id(absenceId, userId)
                .orElseThrow(() -> new UserAbsenceNotFoundException(absenceId));
    }

    private UserAbsence requireAbsenceInOrganization(
            Long userId, Long organizationId, Long absenceId) {
        return userAbsenceRepository
                .findByIdAndUser_IdAndOrganizationLinks_Organization_Id(
                        absenceId, userId, organizationId)
                .orElseThrow(() -> new UserAbsenceNotFoundException(absenceId));
    }

    private void requirePathOrganizationIncluded(
            Long pathOrganizationId, Set<Long> organizationIds) {
        if (!organizationIds.contains(pathOrganizationId)) {
            throw new AbsenceValidationException(
                    "Path organization must be included in the request organization IDs.");
        }
    }

    private void validateOrganizationWriteAccess(Long userId, Set<Long> organizationIds) {
        for (Long organizationId : organizationIds) {
            if (!organizationRepository.existsById(organizationId)) {
                throw new AbsenceValidationException("Organization not found.");
            }
            if (!organizationAccessService.hasOrganizationPermission(
                    organizationId, PermissionType.ABSENCE_WRITE)) {
                throw new AbsenceValidationException(
                        "You do not have absence write access for one of the selected organizations.");
            }
            try {
                userService.assertUserBelongsToOrganization(userId, organizationId);
            } catch (IllegalArgumentException e) {
                throw new AbsenceValidationException(
                        "You must belong to the selected organization to manage absences.");
            }
        }
    }

    private static Set<Long> organizationIdsFromAbsence(UserAbsence absence) {
        LinkedHashSet<Long> organizationIds = new LinkedHashSet<>();
        for (UserAbsenceOrganization link : absence.getOrganizationLinks()) {
            if (link.getOrganization() != null && link.getOrganization().getId() != null) {
                organizationIds.add(link.getOrganization().getId());
            }
        }
        return organizationIds;
    }

    private void setOrganizationLinks(UserAbsence absence, Set<Long> organizationIds) {
        absence.getOrganizationLinks().clear();
        userAbsenceRepository.flush();
        addOrganizationLinks(absence, organizationIds);
    }

    private void mergeOrganizationLinks(UserAbsence absence, Set<Long> requestedOrganizationIds) {
        Set<Long> organizationIds = new LinkedHashSet<>(requestedOrganizationIds);
        for (UserAbsenceOrganization link : absence.getOrganizationLinks()) {
            Long existingOrganizationId = link.getOrganization().getId();
            if (!organizationAccessService.hasOrganizationPermission(
                    existingOrganizationId, PermissionType.ABSENCE_WRITE)) {
                organizationIds.add(existingOrganizationId);
            }
        }
        absence.getOrganizationLinks().clear();
        userAbsenceRepository.flush();
        addOrganizationLinks(absence, organizationIds);
    }

    private void addOrganizationLinks(UserAbsence absence, Set<Long> organizationIds) {
        for (Long organizationId : organizationIds) {
            Organization organization = organizationRepository.getReferenceById(organizationId);
            UserAbsenceOrganization link =
                    UserAbsenceOrganization.builder()
                            .tenantId(organization.getTenantId())
                            .absence(absence)
                            .organization(organization)
                            .build();
            absence.getOrganizationLinks().add(link);
        }
    }

    @Transactional(readOnly = true)
    public List<AbsenceReason> listDistinctReasonsForUser(Long userId) {
        ensureUserExists(userId);
        return userAbsenceRepository.findDistinctReasonsByUserId(userId).stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Organization> listDistinctOrganizationsForUser(Long userId) {
        ensureUserExists(userId);
        return userAbsenceRepository.findDistinctOrganizationRowsByUserId(userId).stream()
                .map(row -> Organization.builder().id((Long) row[0]).name((String) row[1]).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> listDistinctYearsForUser(Long userId) {
        ensureUserExists(userId);
        return listDistinctYearsFromDateRanges(
                userAbsenceRepository.findDateRangesByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<AbsenceReason> listDistinctReasonsForUserInOrganization(
            Long userId, Long organizationId) {
        ensureUserExists(userId);
        ensureOrganizationExists(organizationId);
        return userAbsenceRepository
                .findDistinctReasonsByUserIdAndOrganizationId(userId, organizationId)
                .stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Organization> listDistinctOrganizationsForUserInOrganization(
            Long userId, Long organizationId) {
        ensureUserExists(userId);
        ensureOrganizationExists(organizationId);
        return userAbsenceRepository
                .findDistinctOrganizationRowsByUserIdAndOrganizationId(userId, organizationId)
                .stream()
                .map(row -> Organization.builder().id((Long) row[0]).name((String) row[1]).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> listDistinctYearsForUserInOrganization(Long userId, Long organizationId) {
        ensureUserExists(userId);
        ensureOrganizationExists(organizationId);
        return listDistinctYearsFromDateRanges(
                userAbsenceRepository.findDateRangesByUserIdAndOrganizationId(
                        userId, organizationId));
    }

    @Transactional(readOnly = true)
    public List<AbsenceReason> listDistinctReasonsForOrganization(Long organizationId) {
        ensureOrganizationExists(organizationId);
        return userAbsenceRepository.findDistinctReasonsByOrganizationId(organizationId).stream()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Integer> listDistinctYearsForOrganization(Long organizationId) {
        ensureOrganizationExists(organizationId);
        return listDistinctYearsFromDateRanges(
                userAbsenceRepository.findDateRangesByOrganizationId(organizationId));
    }

    private List<Integer> listDistinctYearsFromDateRanges(List<Object[]> dateRanges) {
        Set<Integer> years = new TreeSet<>(Comparator.reverseOrder());
        for (Object[] row : dateRanges) {
            LocalDate start = (LocalDate) row[0];
            LocalDate end = (LocalDate) row[1];
            for (int year = start.getYear(); year <= end.getYear(); year++) {
                years.add(year);
            }
        }
        years.add(OrganizationWallClockTime.today().getYear());
        return List.copyOf(years);
    }

    private void ensureOrganizationExists(Long organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new AbsenceValidationException("Organization not found.");
        }
    }
}
