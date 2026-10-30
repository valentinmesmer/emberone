/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.list.ListPages;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.ListSpecs;
import com.emberone.qualification.domain.QualificationExerciseStatus;
import com.emberone.qualification.domain.UserQualificationOrgDecision;
import com.emberone.qualification.dto.*;
import com.emberone.qualification.list.QualificationListFilter;
import com.emberone.qualification.list.QualificationListSupport;
import com.emberone.qualification.repository.UserQualificationOrgDecisionRepository;
import com.emberone.qualification.tenant.TenantQualificationDefinition;
import com.emberone.qualification.tenant.TenantQualificationDefinitionRepository;
import com.emberone.qualification.util.QualificationOrgDecisionIntervals;
import com.emberone.user.domain.User;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QualificationStatusService {

    private final QualificationCatalogService catalogService;
    private final TenantQualificationDefinitionRepository definitionRepository;
    private final UserQualificationOrgDecisionRepository orgDecisionRepository;
    private final PrerequisiteEvaluationService prerequisiteEvaluationService;
    private final UserOrganizationMembershipRepository membershipRepository;

    private static String formatDisplayName(User user) {
        String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
        String last = user.getLastName() != null ? user.getLastName().trim() : "";
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? "#" + user.getId() : combined;
    }

    static QualificationExerciseStatus resolveStatus(
            boolean requiresOrgDecision,
            boolean prerequisitesMet,
            boolean orgDecisionGranted,
            boolean hasRevokedDecision) {
        if (!prerequisitesMet) {
            return QualificationExerciseStatus.PREREQUISITES_MISSING;
        }
        if (!requiresOrgDecision) {
            return QualificationExerciseStatus.CAN_EXERCISE;
        }
        if (orgDecisionGranted) {
            return QualificationExerciseStatus.CAN_EXERCISE;
        }
        if (hasRevokedDecision) {
            return QualificationExerciseStatus.ORG_DECISION_REVOKED;
        }
        return QualificationExerciseStatus.ORG_DECISION_DENIED;
    }

    @Transactional(readOnly = true)
    public QualificationStatusItemPageDto listStatusItemsForUser(
            Long organizationId, Long userId, ListQuery query, QualificationListFilter filter) {
        List<QualificationStatusItemDto> items =
                QualificationListSupport.filterAndSort(
                        getStatusForUser(organizationId, userId).getItems(), query, filter);
        return toItemPage(items, query);
    }

    @Transactional(readOnly = true)
    public SelfQualificationStatusItemPageDto listSelfStatusItems(
            Long userId, ListQuery query, QualificationListFilter filter) {
        List<SelfQualificationStatusItemDto> flat = new ArrayList<>();
        for (UserOrganizationMembership membership :
                membershipRepository.findActiveByUser_Id(userId, Instant.now())) {
            Long organizationId = membership.getOrganization().getId();
            for (QualificationStatusItemDto item :
                    getStatusForUser(organizationId, userId).getItems()) {
                flat.add(
                        SelfQualificationStatusItemDto.builder()
                                .organizationId(organizationId)
                                .item(item)
                                .build());
            }
        }
        List<SelfQualificationStatusItemDto> filtered =
                QualificationListSupport.filterAndSortSelf(flat, query, filter);
        Page<SelfQualificationStatusItemDto> page = ListPages.slice(filtered, query);
        return SelfQualificationStatusItemPageDto.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    private static QualificationStatusItemPageDto toItemPage(
            List<QualificationStatusItemDto> items, ListQuery query) {
        Page<QualificationStatusItemDto> page = ListPages.slice(items, query);
        return QualificationStatusItemPageDto.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional(readOnly = true)
    public UserQualificationStatusDto getStatusForUser(Long organizationId, Long userId) {
        List<QualificationDefinitionDto> catalog =
                catalogService.resolveCatalogForOrganization(organizationId);
        PrerequisiteEvaluationService.Holdings holdings =
                prerequisiteEvaluationService.loadHoldings(userId);

        List<QualificationStatusItemDto> items = new ArrayList<>();
        for (QualificationDefinitionDto catalogItem : catalog) {
            TenantQualificationDefinition definition =
                    definitionRepository.findById(catalogItem.getId()).orElse(null);
            if (definition == null) {
                continue;
            }
            items.add(evaluateItem(definition, organizationId, userId, holdings));
        }
        return UserQualificationStatusDto.builder()
                .userId(userId)
                .organizationId(organizationId)
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public OrganizationQualificationStatusOverviewDto getStatusOverview(
            Long organizationId, String search) {
        List<QualificationDefinitionDto> catalog =
                catalogService.resolveCatalogForOrganization(organizationId);
        Map<Long, QualificationDefinitionDto> definitionsById =
                catalog.stream()
                        .collect(
                                Collectors.toMap(
                                        QualificationDefinitionDto::getId,
                                        definition -> definition,
                                        (left, right) -> left,
                                        LinkedHashMap::new));

        Map<Long, List<QualificationOverviewMemberDto>> membersByDefinition = new LinkedHashMap<>();
        Map<Long, List<QualificationOverviewMemberDto>> appointableByDefinition =
                new LinkedHashMap<>();
        for (Long definitionId : definitionsById.keySet()) {
            membersByDefinition.put(definitionId, new ArrayList<>());
            appointableByDefinition.put(definitionId, new ArrayList<>());
        }

        List<UserOrganizationMembership> memberships =
                membershipRepository.findActiveByOrganization_Id(organizationId, Instant.now());

        for (UserOrganizationMembership membership : memberships) {
            User user = membership.getUser();
            Long userId = user.getId();
            String displayName = formatDisplayName(user);
            UserQualificationStatusDto status = getStatusForUser(organizationId, userId);
            for (QualificationStatusItemDto item : status.getItems()) {
                Long definitionId = item.getTenantQualificationDefinitionId();
                if (!definitionsById.containsKey(definitionId)) {
                    continue;
                }
                QualificationOverviewMemberDto member =
                        QualificationOverviewMemberDto.builder()
                                .userId(userId)
                                .userDisplayName(displayName)
                                .build();
                if (item.getStatus() == QualificationExerciseStatus.CAN_EXERCISE) {
                    membersByDefinition.get(definitionId).add(member);
                } else if (item.isRequiresOrgDecision()
                        && item.isPrerequisitesMet()
                        && !item.isOrgDecisionGranted()) {
                    appointableByDefinition.get(definitionId).add(member);
                }
            }
        }

        List<QualificationOverviewGroupDto> groups =
                definitionsById.values().stream()
                        .map(
                                definition -> {
                                    Long definitionId = definition.getId();
                                    List<QualificationOverviewMemberDto> members =
                                            membersByDefinition.get(definitionId).stream()
                                                    .sorted(
                                                            Comparator.comparing(
                                                                    QualificationOverviewMemberDto
                                                                            ::getUserDisplayName,
                                                                    String.CASE_INSENSITIVE_ORDER))
                                                    .toList();
                                    List<QualificationOverviewMemberDto> appointableMembers =
                                            appointableByDefinition.get(definitionId).stream()
                                                    .sorted(
                                                            Comparator.comparing(
                                                                    QualificationOverviewMemberDto
                                                                            ::getUserDisplayName,
                                                                    String.CASE_INSENSITIVE_ORDER))
                                                    .toList();
                                    return QualificationOverviewGroupDto.builder()
                                            .tenantQualificationDefinitionId(definitionId)
                                            .configKey(definition.getConfigKey())
                                            .name(definition.getName())
                                            .requiresOrgDecision(definition.isRequiresOrgDecision())
                                            .members(members)
                                            .appointableMembers(appointableMembers)
                                            .build();
                                })
                        .sorted(
                                Comparator.comparing(
                                        QualificationOverviewGroupDto::getName,
                                        String.CASE_INSENSITIVE_ORDER))
                        .toList();

        return OrganizationQualificationStatusOverviewDto.builder()
                .groups(filterGroupsBySearch(groups, search))
                .build();
    }

    /**
     * Token search (case-insensitive, all tokens must match). A group matching by name keeps all
     * its members; otherwise only matching members are kept and groups left without members are
     * dropped.
     */
    static List<QualificationOverviewGroupDto> filterGroupsBySearch(
            List<QualificationOverviewGroupDto> groups, String search) {
        List<String> tokens = ListSpecs.searchTokens(search);
        if (tokens.isEmpty()) {
            return groups;
        }
        return groups.stream()
                .map(
                        group -> {
                            if (matchesTokens(group.getName(), tokens)) {
                                return group;
                            }
                            List<QualificationOverviewMemberDto> members =
                                    group.getMembers().stream()
                                            .filter(
                                                    member ->
                                                            matchesTokens(
                                                                    member.getUserDisplayName(),
                                                                    tokens))
                                            .toList();
                            return members.isEmpty()
                                    ? null
                                    : group.toBuilder().members(members).build();
                        })
                .filter(Objects::nonNull)
                .toList();
    }

    private static boolean matchesTokens(String text, List<String> tokens) {
        String haystack = text == null ? "" : text.toLowerCase(Locale.ROOT);
        return tokens.stream().allMatch(haystack::contains);
    }

    @Transactional(readOnly = true)
    public QualificationExerciseStatus evaluateStatus(
            TenantQualificationDefinition definition, Long organizationId, Long userId) {
        return evaluateItem(
                        definition,
                        organizationId,
                        userId,
                        prerequisiteEvaluationService.loadHoldings(userId))
                .getStatus();
    }

    private QualificationStatusItemDto evaluateItem(
            TenantQualificationDefinition definition,
            Long organizationId,
            Long userId,
            PrerequisiteEvaluationService.Holdings holdings) {
        List<String> trainingCourseKeys =
                definition.getTrainingCourses().stream()
                        .map(link -> link.getTrainingCourseConfigKey())
                        .toList();
        List<String> certKeys =
                definition.getCertDefinitions().stream()
                        .map(link -> link.getCertDefinitionConfigKey())
                        .toList();
        List<String> drivingLicenseKeys =
                definition.getDrivingLicenses().stream()
                        .map(link -> link.getDrivingLicenseConfigKey())
                        .toList();
        boolean trainingCoursesMet =
                prerequisiteEvaluationService.trainingCoursesMet(
                        trainingCourseKeys,
                        definition.getTrainingCourseRequirementMode(),
                        holdings);
        boolean certDefinitionsMet =
                prerequisiteEvaluationService.certDefinitionsMet(
                        certKeys, definition.getCertRequirementMode(), holdings);
        boolean drivingLicensesMet =
                prerequisiteEvaluationService.drivingLicensesMet(
                        drivingLicenseKeys,
                        definition.getDrivingLicenseRequirementMode(),
                        holdings);
        boolean prerequisitesMet = trainingCoursesMet && certDefinitionsMet && drivingLicensesMet;

        List<UserQualificationOrgDecision> decisions =
                orgDecisionRepository
                        .findByUserIdAndTenantQualificationDefinitionIdAndOrganizationIdOrderByStartDateDesc(
                                userId, definition.getId(), organizationId);
        LocalDate today = OrganizationWallClockTime.today();
        boolean orgDecisionGranted = decisions.stream().anyMatch(d -> d.isActiveGrantOn(today));
        boolean hasRevokedDecision =
                !orgDecisionGranted
                        && decisions.stream()
                                .anyMatch(
                                        d ->
                                                QualificationOrgDecisionIntervals
                                                        .isRevokedCoveringDate(d, today));

        QualificationExerciseStatus status =
                resolveStatus(
                        definition.isRequiresOrgDecision(),
                        prerequisitesMet,
                        orgDecisionGranted,
                        hasRevokedDecision);

        return QualificationStatusItemDto.builder()
                .tenantQualificationDefinitionId(definition.getId())
                .configKey(definition.getConfigKey())
                .abbreviation(definition.getConfigKey())
                .name(definition.getName())
                .status(status)
                .prerequisitesMet(prerequisitesMet)
                .trainingCoursesMet(trainingCoursesMet)
                .certDefinitionsMet(certDefinitionsMet)
                .drivingLicensesMet(drivingLicensesMet)
                .trainingCourseRequirementMode(definition.getTrainingCourseRequirementMode())
                .certRequirementMode(definition.getCertRequirementMode())
                .drivingLicenseRequirementMode(definition.getDrivingLicenseRequirementMode())
                .requiresOrgDecision(definition.isRequiresOrgDecision())
                .orgDecisionGranted(orgDecisionGranted)
                .orgDecisions(
                        decisions.stream().map(QualificationOrgDecisionService::toDto).toList())
                .trainingCourseDetails(
                        prerequisiteEvaluationService.trainingCourseDetails(
                                trainingCourseKeys, holdings))
                .certDefinitionDetails(
                        prerequisiteEvaluationService.certDefinitionDetails(certKeys, holdings))
                .drivingLicenseDetails(
                        prerequisiteEvaluationService.drivingLicenseDetails(
                                drivingLicenseKeys, holdings))
                .build();
    }
}
