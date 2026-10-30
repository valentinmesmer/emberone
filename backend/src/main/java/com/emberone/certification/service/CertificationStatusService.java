/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.domain.CertificationStatus;
import com.emberone.certification.domain.UserCertificationAssignment;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.dto.CertificationOverviewCountsDto;
import com.emberone.certification.dto.CertificationOverviewRowDto;
import com.emberone.certification.dto.CertificationOverviewStatusFilter;
import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.dto.UserCertificationStatusDto;
import com.emberone.certification.list.CertificationOverviewListFilter;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.platform.domain.LifecycleFields;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.ListEndpointLimits;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.ListSort;
import com.emberone.platform.web.list.ListSpecs;
import com.emberone.platform.web.list.options.ListFilterOptionDto;
import com.emberone.user.domain.User;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.dto.CertificationRecordWorkflowPayload;
import com.emberone.workflow.repository.WorkflowRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class CertificationStatusService {

    private static final Comparator<String> TEXT_ORDER =
            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER);

    private static final Map<String, Comparator<CertificationOverviewRowDto>> OVERVIEW_SORTS =
            Map.of(
                    "name",
                    Comparator.comparing(CertificationOverviewRowDto::getLastName, TEXT_ORDER)
                            .thenComparing(CertificationOverviewRowDto::getFirstName, TEXT_ORDER),
                    "expiryDate",
                    Comparator.comparing(
                            CertificationOverviewRowDto::getExpiryDate,
                            Comparator.nullsLast(Comparator.naturalOrder())));

    private static final Set<String> OVERVIEW_STATUSES =
            Set.of(
                    CertificationStatus.MISSING.name(),
                    CertificationStatus.EXPIRED.name(),
                    CertificationStatus.EXPIRING_SOON.name(),
                    CertificationStatus.VALID.name());

    private final UserCertificationAssignmentRepository assignmentRepository;
    private final TenantCertDefinitionRepository tenantDefinitionRepository;
    private final UserCertificationRecordRepository recordRepository;
    private final WorkflowRepository workflowRepository;
    private final JsonMapper jsonMapper;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final TenantCertDefinitionEvidenceTypeService evidenceTypeService;

    private static boolean isAssignableDefinition(TenantCertDefinition definition) {
        return definition != null && LifecycleFields.isCatalogSelectable(definition);
    }

    static CertificationStatus computeStatus(
            TenantCertDefinition definition, UserCertificationRecord record, LocalDate today) {
        return computeStatus(definition, record, today, false);
    }

    static CertificationStatus computeStatus(
            TenantCertDefinition definition,
            UserCertificationRecord record,
            LocalDate today,
            boolean inReview) {
        if (record == null) {
            if (inReview) {
                return CertificationStatus.IN_REVIEW;
            }
            return CertificationStatus.MISSING;
        }
        LocalDate expiry = record.getExpiryDate();
        if (expiry == null) {
            return CertificationStatus.VALID;
        }
        if (expiry.isBefore(today)) {
            return CertificationStatus.EXPIRED;
        }
        int reminderDays =
                definition.getReminderDaysBeforeExpiry() == null
                        ? 30
                        : definition.getReminderDaysBeforeExpiry();
        if (!expiry.isAfter(today.plusDays(reminderDays))) {
            return CertificationStatus.EXPIRING_SOON;
        }
        return CertificationStatus.VALID;
    }

    private static String formatDisplayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "#" + user.getId() : combined;
    }

    @Transactional(readOnly = true)
    public UserCertificationStatusDto getStatusForUser(Long userId) {
        return computeStatusForUser(userId, OrganizationWallClockTime.today(), perUserData());
    }

    /**
     * Paged status overview of an organization. Status is computed ({@link #computeStatus}), so
     * filtering, search and paging happen in memory over a batch-loaded snapshot of the
     * organization (documented exception to DB-level paging).
     */
    @Transactional(readOnly = true)
    public PageDto<CertificationOverviewRowDto> getStatusOverview(
            Long organizationId, ListQuery query, CertificationOverviewListFilter filter) {
        List<String> tokens = ListSpecs.searchTokens(query.search());
        List<CertificationOverviewRowDto> rows =
                loadOverviewRows(organizationId).stream()
                        .filter(row -> matchesStatus(row, filter.statuses()))
                        .filter(row -> matchesCertDefinition(row, filter.tenantCertDefinitionIds()))
                        .filter(row -> matchesSearch(row, tokens))
                        .sorted(overviewComparator(query.sort()))
                        .toList();

        int pageIndex = query.page() != null && query.page() >= 0 ? query.page() : 0;
        int pageSize =
                query.size() != null && query.size() > 0
                        ? Math.min(query.size(), ListEndpointLimits.MAX_PAGE_SIZE)
                        : ListEndpointLimits.DEFAULT_PAGE_SIZE;
        long from = (long) pageIndex * pageSize;
        List<CertificationOverviewRowDto> content =
                from >= rows.size()
                        ? List.of()
                        : rows.subList((int) from, (int) Math.min(rows.size(), from + pageSize));
        int totalPages = (int) ((rows.size() + pageSize - 1L) / pageSize);
        return PageDto.<CertificationOverviewRowDto>builder()
                .content(content)
                .page(pageIndex)
                .size(pageSize)
                .totalElements(rows.size())
                .totalPages(totalPages)
                .hasNext(pageIndex + 1 < totalPages)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ListFilterOptionDto> listCertDefinitionFilterOptionsForOrganization(
            Long organizationId) {
        Map<Long, String> namesById = new LinkedHashMap<>();
        for (CertificationOverviewRowDto row : loadOverviewRows(organizationId)) {
            namesById.putIfAbsent(row.getTenantCertDefinitionId(), row.getCertDefinitionName());
        }
        return namesById.entrySet().stream()
                .sorted(
                        Comparator.comparing(
                                Map.Entry::getValue,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .map(
                        entry ->
                                new ListFilterOptionDto(
                                        String.valueOf(entry.getKey()), entry.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CertificationOverviewCountsDto getStatusOverviewCounts(Long organizationId) {
        List<CertificationOverviewRowDto> rows = loadOverviewRows(organizationId);
        return CertificationOverviewCountsDto.builder()
                .all(rows.size())
                .missing(countStatus(rows, CertificationStatus.MISSING))
                .expired(countStatus(rows, CertificationStatus.EXPIRED))
                .expiringSoon(countStatus(rows, CertificationStatus.EXPIRING_SOON))
                .valid(countStatus(rows, CertificationStatus.VALID))
                .build();
    }

    private static long countStatus(List<CertificationOverviewRowDto> rows, CertificationStatus s) {
        return rows.stream().filter(row -> s.name().equals(row.getStatus())).count();
    }

    private static boolean matchesStatus(
            CertificationOverviewRowDto row, List<CertificationOverviewStatusFilter> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        String status = row.getStatus();
        return filters.stream().anyMatch(filter -> filter.name().equals(status));
    }

    private static boolean matchesCertDefinition(
            CertificationOverviewRowDto row, List<Long> tenantCertDefinitionIds) {
        if (tenantCertDefinitionIds == null || tenantCertDefinitionIds.isEmpty()) {
            return true;
        }
        return tenantCertDefinitionIds.contains(row.getTenantCertDefinitionId());
    }

    /** Without {@code sort} the rows keep the order of {@link #loadOverviewRows}. */
    private static Comparator<CertificationOverviewRowDto> overviewComparator(String clientSort) {
        if (clientSort == null || clientSort.isBlank()) {
            return (a, b) -> 0;
        }
        ListSort sort = ListSort.parse(clientSort);
        Comparator<CertificationOverviewRowDto> comparator = OVERVIEW_SORTS.get(sort.key());
        if (comparator == null) {
            throw new IllegalArgumentException("invalid sort field");
        }
        if (sort.direction().isDescending()) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static boolean matchesSearch(CertificationOverviewRowDto row, List<String> tokens) {
        if (tokens.isEmpty()) {
            return true;
        }
        String haystack = (row.getFirstName() + " " + row.getLastName()).toLowerCase(Locale.ROOT);
        return tokens.stream().allMatch(haystack::contains);
    }

    private List<CertificationOverviewRowDto> loadOverviewRows(Long organizationId) {
        LocalDate today = OrganizationWallClockTime.today();
        Map<Long, User> users = new LinkedHashMap<>();
        for (UserOrganizationMembership membership :
                membershipRepository.findActiveByOrganization_Id(organizationId, Instant.now())) {
            users.putIfAbsent(membership.getUser().getId(), membership.getUser());
        }
        if (users.isEmpty()) {
            return List.of();
        }
        StatusData data = batchData(users.keySet());

        List<CertificationOverviewRowDto> rows = new ArrayList<>();
        for (User user : users.values()) {
            UserCertificationStatusDto status = computeStatusForUser(user.getId(), today, data);
            String displayName = formatDisplayName(user);
            for (CertificationStatusItemDto item : status.getItems()) {
                if (!item.isAssigned()
                        || item.isArchived()
                        || !OVERVIEW_STATUSES.contains(item.getStatus())) {
                    continue;
                }
                rows.add(
                        CertificationOverviewRowDto.builder()
                                .userId(user.getId())
                                .userDisplayName(displayName)
                                .firstName(trimToEmpty(user.getFirstName()))
                                .lastName(trimToEmpty(user.getLastName()))
                                .tenantCertDefinitionId(item.getTenantCertDefinitionId())
                                .certDefinitionName(item.getCertDefinitionName())
                                .status(item.getStatus())
                                .expiryDate(item.getExpiryDate())
                                .build());
            }
        }
        rows.sort(
                Comparator.comparing(
                                CertificationOverviewRowDto::getLastName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                CertificationOverviewRowDto::getFirstName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                CertificationOverviewRowDto::getCertDefinitionName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(CertificationOverviewRowDto::getUserId));
        return rows;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private UserCertificationStatusDto computeStatusForUser(
            Long userId, LocalDate today, StatusData data) {
        Map<Long, CertificationStatusItemDto> byDefinition = new LinkedHashMap<>();
        Set<Long> definitionsInReview = data.definitionsInOpenReview(userId);

        for (UserCertificationAssignment assignment : data.assignments(userId)) {
            if (!assignment.isActiveOn(today)) {
                continue;
            }
            addDefinitionRequirement(
                    byDefinition,
                    userId,
                    assignment.getTenantCertDefinitionId(),
                    assignment.getResponsibleOrganizationId(),
                    today,
                    definitionsInReview,
                    data);
        }

        addHistoricalItems(byDefinition, userId, today, data);

        List<CertificationStatusItemDto> items = new ArrayList<>(byDefinition.values());
        items.sort(
                Comparator.comparing(
                        CertificationStatusItemDto::getCertDefinitionName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        int missing = 0, expired = 0, expiringSoon = 0, valid = 0;
        for (CertificationStatusItemDto item : items) {
            switch (CertificationStatus.valueOf(item.getStatus())) {
                case MISSING -> missing++;
                case IN_REVIEW -> {
                    /* pending review */
                }
                case EXPIRED -> expired++;
                case EXPIRING_SOON -> expiringSoon++;
                case VALID -> valid++;
                case ARCHIVED -> {
                    /* historical only */
                }
            }
        }

        return UserCertificationStatusDto.builder()
                .userId(userId)
                .items(items)
                .missingCount(missing)
                .expiredCount(expired)
                .expiringSoonCount(expiringSoon)
                .validCount(valid)
                .build();
    }

    private void addDefinitionRequirement(
            Map<Long, CertificationStatusItemDto> acc,
            Long userId,
            Long definitionId,
            Long responsibleOrgId,
            LocalDate today,
            Set<Long> definitionsInReview,
            StatusData data) {
        if (acc.containsKey(definitionId)) {
            return;
        }
        TenantCertDefinition definition = data.definition(definitionId);
        if (!isAssignableDefinition(definition)) {
            return;
        }
        UserCertificationRecord latest = effectiveRecord(userId, definitionId, today, data);
        CertificationStatus status =
                computeStatus(
                        definition, latest, today, definitionsInReview.contains(definitionId));
        acc.put(
                definitionId,
                CertificationStatusItemDto.builder()
                        .tenantCertDefinitionId(definitionId)
                        .certDefinitionName(definition.getName())
                        .status(status.name())
                        .mandatory(true)
                        .responsibleOrganizationId(responsibleOrgId)
                        .completedDate(latest == null ? null : latest.getCompletedDate())
                        .expiryDate(latest == null ? null : latest.getExpiryDate())
                        .recordId(latest == null ? null : latest.getId())
                        .archived(false)
                        .assigned(true)
                        .evidenceTypes(data.evidenceTypes(definition))
                        .build());
    }

    private void addHistoricalItems(
            Map<Long, CertificationStatusItemDto> acc,
            Long userId,
            LocalDate today,
            StatusData data) {
        for (UserCertificationRecord record : data.records(userId)) {
            Long definitionId = record.getTenantCertDefinitionId();
            if (acc.containsKey(definitionId)) {
                continue;
            }
            TenantCertDefinition definition = data.definition(definitionId);
            if (definition == null) {
                continue;
            }
            if (definition.isArchived()) {
                putArchivedItem(acc, userId, definition, today, data);
            } else {
                putUnassignedHistoricalItem(acc, userId, definition, today, data);
            }
        }

        for (UserCertificationAssignment assignment : data.assignments(userId)) {
            TenantCertDefinition definition =
                    data.definition(assignment.getTenantCertDefinitionId());
            if (definition != null
                    && definition.isArchived()
                    && !acc.containsKey(definition.getId())) {
                putArchivedItem(acc, userId, definition, today, data);
            }
        }
    }

    private void putArchivedItem(
            Map<Long, CertificationStatusItemDto> acc,
            Long userId,
            TenantCertDefinition definition,
            LocalDate today,
            StatusData data) {
        UserCertificationRecord latest = effectiveRecord(userId, definition.getId(), today, data);
        acc.put(
                definition.getId(),
                CertificationStatusItemDto.builder()
                        .tenantCertDefinitionId(definition.getId())
                        .certDefinitionName(definition.getName())
                        .status(CertificationStatus.ARCHIVED.name())
                        .mandatory(false)
                        .responsibleOrganizationId(null)
                        .completedDate(latest == null ? null : latest.getCompletedDate())
                        .expiryDate(latest == null ? null : latest.getExpiryDate())
                        .recordId(latest == null ? null : latest.getId())
                        .archived(true)
                        .assigned(false)
                        .evidenceTypes(data.evidenceTypes(definition))
                        .build());
    }

    private void putUnassignedHistoricalItem(
            Map<Long, CertificationStatusItemDto> acc,
            Long userId,
            TenantCertDefinition definition,
            LocalDate today,
            StatusData data) {
        UserCertificationRecord latest = effectiveRecord(userId, definition.getId(), today, data);
        if (latest == null) {
            return;
        }
        CertificationStatus status = computeStatus(definition, latest, today);
        acc.put(
                definition.getId(),
                CertificationStatusItemDto.builder()
                        .tenantCertDefinitionId(definition.getId())
                        .certDefinitionName(definition.getName())
                        .status(status.name())
                        .mandatory(false)
                        .responsibleOrganizationId(null)
                        .completedDate(latest.getCompletedDate())
                        .expiryDate(latest.getExpiryDate())
                        .recordId(latest.getId())
                        .archived(false)
                        .assigned(false)
                        .evidenceTypes(data.evidenceTypes(definition))
                        .build());
    }

    private UserCertificationRecord effectiveRecord(
            Long userId, Long definitionId, LocalDate today, StatusData data) {
        List<UserCertificationRecord> records = data.records(userId, definitionId);
        if (records.isEmpty()) {
            return null;
        }
        return records.stream()
                .filter(
                        record ->
                                record.getExpiryDate() == null
                                        || !record.getExpiryDate().isBefore(today))
                .max(
                        Comparator.comparing(
                                        (UserCertificationRecord record) ->
                                                record.getExpiryDate() == null
                                                        ? LocalDate.MAX
                                                        : record.getExpiryDate())
                                .thenComparing(UserCertificationRecord::getCompletedDate))
                .orElseGet(
                        () ->
                                records.stream()
                                        .max(
                                                Comparator.comparing(
                                                        UserCertificationRecord::getCompletedDate))
                                        .orElse(null));
    }

    private Set<Long> definitionIdsOf(List<Workflow> workflows) {
        Set<Long> definitionIds = new HashSet<>();
        for (Workflow workflow : workflows) {
            if (workflow.getStatus() != WorkflowStatus.OPEN
                    || !WorkflowType.CERTIFICATION_RECORD
                            .getId()
                            .equals(workflow.getWorkflowTypeId())) {
                continue;
            }
            CertificationRecordWorkflowPayload payload =
                    readCertificationPayload(workflow.getPayload());
            if (payload == null || payload.getTenantCertDefinitionId() == null) {
                continue;
            }
            definitionIds.add(payload.getTenantCertDefinitionId());
        }
        return definitionIds;
    }

    private CertificationRecordWorkflowPayload readCertificationPayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return jsonMapper.readValue(payload, CertificationRecordWorkflowPayload.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // --- Data access for the status computation (per user or batch-loaded) -----------------

    /** Source of the inputs of the status computation; the logic itself exists only once. */
    private interface StatusData {
        List<UserCertificationAssignment> assignments(Long userId);

        List<UserCertificationRecord> records(Long userId);

        List<UserCertificationRecord> records(Long userId, Long definitionId);

        TenantCertDefinition definition(Long definitionId);

        Set<Long> definitionsInOpenReview(Long userId);

        Set<String> evidenceTypes(TenantCertDefinition definition);
    }

    private StatusData perUserData() {
        return new StatusData() {
            @Override
            public List<UserCertificationAssignment> assignments(Long userId) {
                return assignmentRepository.findByUserId(userId);
            }

            @Override
            public List<UserCertificationRecord> records(Long userId) {
                return recordRepository.findByUserId(userId);
            }

            @Override
            public List<UserCertificationRecord> records(Long userId, Long definitionId) {
                return recordRepository.findByUserIdAndTenantCertDefinitionId(userId, definitionId);
            }

            @Override
            public TenantCertDefinition definition(Long definitionId) {
                return tenantDefinitionRepository.findById(definitionId).orElse(null);
            }

            @Override
            public Set<Long> definitionsInOpenReview(Long userId) {
                return definitionIdsOf(workflowRepository.findOpenedByUser(userId));
            }

            @Override
            public Set<String> evidenceTypes(TenantCertDefinition definition) {
                return TenantCertDefinitionEvidenceTypeService.toStringSet(
                        evidenceTypeService.getEvidenceTypes(definition.getId()));
            }
        };
    }

    /**
     * Loads all inputs for the given users with a constant number of queries. Evidence types are
     * not part of the overview rows and are therefore not loaded.
     */
    private StatusData batchData(Collection<Long> userIds) {
        Map<Long, List<UserCertificationAssignment>> assignments =
                assignmentRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.groupingBy(UserCertificationAssignment::getUserId));
        Map<Long, List<UserCertificationRecord>> records =
                recordRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.groupingBy(UserCertificationRecord::getUserId));
        Set<Long> definitionIds = new HashSet<>();
        assignments
                .values()
                .forEach(l -> l.forEach(a -> definitionIds.add(a.getTenantCertDefinitionId())));
        records.values()
                .forEach(l -> l.forEach(r -> definitionIds.add(r.getTenantCertDefinitionId())));
        Map<Long, TenantCertDefinition> definitions =
                tenantDefinitionRepository.findAllById(definitionIds).stream()
                        .collect(Collectors.toMap(TenantCertDefinition::getId, d -> d));
        Map<Long, List<Workflow>> workflows =
                workflowRepository
                        .findOpenByOpenedByUserIdInAndWorkflowTypeId(
                                userIds, WorkflowType.CERTIFICATION_RECORD.getId())
                        .stream()
                        .collect(Collectors.groupingBy(Workflow::getOpenedByUserId));

        return new StatusData() {
            @Override
            public List<UserCertificationAssignment> assignments(Long userId) {
                return assignments.getOrDefault(userId, List.of());
            }

            @Override
            public List<UserCertificationRecord> records(Long userId) {
                return records.getOrDefault(userId, List.of());
            }

            @Override
            public List<UserCertificationRecord> records(Long userId, Long definitionId) {
                return records(userId).stream()
                        .filter(r -> definitionId.equals(r.getTenantCertDefinitionId()))
                        .toList();
            }

            @Override
            public TenantCertDefinition definition(Long definitionId) {
                return definitions.get(definitionId);
            }

            @Override
            public Set<Long> definitionsInOpenReview(Long userId) {
                return definitionIdsOf(workflows.getOrDefault(userId, List.of()));
            }

            @Override
            public Set<String> evidenceTypes(TenantCertDefinition definition) {
                return Set.of();
            }
        };
    }
}
