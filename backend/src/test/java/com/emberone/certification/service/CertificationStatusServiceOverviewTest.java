/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.emberone.certification.domain.UserCertificationAssignment;
import com.emberone.certification.domain.UserCertificationRecord;
import com.emberone.certification.dto.CertificationOverviewCountsDto;
import com.emberone.certification.dto.CertificationOverviewRowDto;
import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.list.CertificationOverviewListFilter;
import com.emberone.certification.repository.UserCertificationAssignmentRepository;
import com.emberone.certification.repository.UserCertificationRecordRepository;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.platform.web.PageDto;
import com.emberone.platform.web.list.ListQuery;
import com.emberone.user.domain.User;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowStatus;
import com.emberone.workflow.domain.WorkflowType;
import com.emberone.workflow.repository.WorkflowRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class CertificationStatusServiceOverviewTest {

    private static final CertificationOverviewListFilter UNFILTERED =
            CertificationOverviewListFilter.parse(null, null);

    private static final Long ORG_ID = 10L;
    private static final Long DEF_A = 1L;
    private static final Long DEF_B = 2L;

    private final UserCertificationAssignmentRepository assignmentRepository =
            mock(UserCertificationAssignmentRepository.class);
    private final TenantCertDefinitionRepository definitionRepository =
            mock(TenantCertDefinitionRepository.class);
    private final UserCertificationRecordRepository recordRepository =
            mock(UserCertificationRecordRepository.class);
    private final WorkflowRepository workflowRepository = mock(WorkflowRepository.class);
    private final UserOrganizationMembershipRepository membershipRepository =
            mock(UserOrganizationMembershipRepository.class);
    private final TenantCertDefinitionEvidenceTypeService evidenceTypeService =
            mock(TenantCertDefinitionEvidenceTypeService.class);

    private CertificationStatusService service;
    private final LocalDate today = OrganizationWallClockTime.today();

    private final List<UserCertificationAssignment> assignments = new ArrayList<>();
    private final List<UserCertificationRecord> records = new ArrayList<>();
    private final List<Workflow> workflows = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service =
                new CertificationStatusService(
                        assignmentRepository,
                        definitionRepository,
                        recordRepository,
                        workflowRepository,
                        JsonMapper.builder().build(),
                        membershipRepository,
                        evidenceTypeService);

        TenantCertDefinition a = definition(DEF_A, "Atemschutz");
        TenantCertDefinition b = definition(DEF_B, "Maschinist");
        User ada = user(1L, "Ada", "Zeta");
        User bob = user(2L, "Bob", "Alpha");
        User cy = user(3L, "Cy", "Alpha");
        lenient()
                .when(membershipRepository.findActiveByOrganization_Id(any(), any()))
                .thenReturn(List.of(membership(ada), membership(bob), membership(cy)));

        // Ada: A valid, B expired. Bob: A missing, B missing but in review (excluded).
        // Cy: A expiring soon.
        assign(1L, DEF_A);
        assign(1L, DEF_B);
        assign(2L, DEF_A);
        assign(2L, DEF_B);
        assign(3L, DEF_A);
        record(1L, DEF_A, today.minusDays(300), today.plusDays(100));
        record(1L, DEF_B, today.minusDays(900), today.minusDays(10));
        record(3L, DEF_A, today.minusDays(300), today.plusDays(5));
        workflows.add(
                Workflow.builder()
                        .id(1L)
                        .workflowTypeId(WorkflowType.CERTIFICATION_RECORD.getId())
                        .status(WorkflowStatus.OPEN)
                        .openedByUserId(2L)
                        .payload("{\"tenantCertDefinitionId\":" + DEF_B + "}")
                        .build());

        lenient().when(definitionRepository.findAllById(any())).thenReturn(List.of(a, b));
        lenient().when(definitionRepository.findById(DEF_A)).thenReturn(Optional.of(a));
        lenient().when(definitionRepository.findById(DEF_B)).thenReturn(Optional.of(b));
        lenient()
                .when(assignmentRepository.findByUserIdIn(any()))
                .thenAnswer(
                        inv ->
                                assignments.stream()
                                        .filter(
                                                x ->
                                                        ids(inv.getArgument(0))
                                                                .contains(x.getUserId()))
                                        .toList());
        lenient()
                .when(recordRepository.findByUserIdIn(any()))
                .thenAnswer(
                        inv ->
                                records.stream()
                                        .filter(
                                                x ->
                                                        ids(inv.getArgument(0))
                                                                .contains(x.getUserId()))
                                        .toList());
        lenient()
                .when(workflowRepository.findOpenByOpenedByUserIdInAndWorkflowTypeId(any(), any()))
                .thenAnswer(
                        inv ->
                                workflows.stream()
                                        .filter(
                                                x ->
                                                        ids(inv.getArgument(0))
                                                                .contains(x.getOpenedByUserId()))
                                        .toList());
        // per-user path (getStatusForUser)
        lenient()
                .when(assignmentRepository.findByUserId(anyLong()))
                .thenAnswer(
                        inv ->
                                assignments.stream()
                                        .filter(x -> x.getUserId().equals(inv.getArgument(0)))
                                        .toList());
        lenient()
                .when(recordRepository.findByUserId(anyLong()))
                .thenAnswer(
                        inv ->
                                records.stream()
                                        .filter(x -> x.getUserId().equals(inv.getArgument(0)))
                                        .toList());
        lenient()
                .when(recordRepository.findByUserIdAndTenantCertDefinitionId(anyLong(), anyLong()))
                .thenAnswer(
                        inv ->
                                records.stream()
                                        .filter(
                                                x ->
                                                        x.getUserId().equals(inv.getArgument(0))
                                                                && x.getTenantCertDefinitionId()
                                                                        .equals(inv.getArgument(1)))
                                        .toList());
        lenient()
                .when(workflowRepository.findOpenedByUser(anyLong()))
                .thenAnswer(
                        inv ->
                                workflows.stream()
                                        .filter(
                                                x ->
                                                        x.getOpenedByUserId()
                                                                .equals(inv.getArgument(0)))
                                        .toList());
        lenient().when(evidenceTypeService.getEvidenceTypes(any())).thenReturn(Set.of());
    }

    @SuppressWarnings("unchecked")
    private static Collection<Long> ids(Object arg) {
        return (Collection<Long>) arg;
    }

    @Test
    void sortsByLastNameFirstNameThenCertificationName() {
        PageDto<CertificationOverviewRowDto> page = overview(UNFILTERED, null, 0, null);

        assertThat(page.getContent())
                .extracting(
                        r ->
                                r.getLastName()
                                        + "/"
                                        + r.getFirstName()
                                        + "/"
                                        + r.getCertDefinitionName())
                .containsExactly(
                        "Alpha/Bob/Atemschutz",
                        "Alpha/Cy/Atemschutz",
                        "Zeta/Ada/Atemschutz",
                        "Zeta/Ada/Maschinist");
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent().getFirst().getUserDisplayName()).isEqualTo("Bob Alpha");
    }

    @Test
    void filtersByStatusAndTreatsAbsentAsUnfiltered() {
        assertThat(
                        overview(
                                        CertificationOverviewListFilter.parse(
                                                List.of("MISSING"), null),
                                        null,
                                        0,
                                        null)
                                .getContent())
                .extracting(CertificationOverviewRowDto::getUserId)
                .containsExactly(2L);
        assertThat(
                        overview(
                                        CertificationOverviewListFilter.parse(
                                                List.of("EXPIRED"), null),
                                        null,
                                        0,
                                        null)
                                .getContent())
                .extracting(CertificationOverviewRowDto::getCertDefinitionName)
                .containsExactly("Maschinist");
        assertThat(
                        overview(
                                        CertificationOverviewListFilter.parse(
                                                List.of("EXPIRING_SOON"), null),
                                        null,
                                        0,
                                        null)
                                .getContent())
                .extracting(CertificationOverviewRowDto::getUserId)
                .containsExactly(3L);
        assertThat(
                        overview(
                                        CertificationOverviewListFilter.parse(
                                                List.of("VALID"), null),
                                        null,
                                        0,
                                        null)
                                .getContent())
                .extracting(CertificationOverviewRowDto::getUserId)
                .containsExactly(1L);
        assertThat(
                        overview(CertificationOverviewListFilter.parse(null, null), null, 0, null)
                                .getContent())
                .hasSize(4);
        assertThat(
                        overview(
                                        CertificationOverviewListFilter.parse(
                                                List.of("MISSING", "EXPIRED"), null),
                                        null,
                                        0,
                                        null)
                                .getContent())
                .hasSize(2);
        assertThat(
                        overview(
                                        CertificationOverviewListFilter.parse(null, List.of("2")),
                                        null,
                                        0,
                                        null)
                                .getContent())
                .extracting(CertificationOverviewRowDto::getCertDefinitionName)
                .containsExactly("Maschinist");
    }

    @Test
    void searchMatchesFirstAndLastNameTokens() {
        assertThat(overview(UNFILTERED, "alpha", 0, null).getContent()).hasSize(2);
        assertThat(overview(UNFILTERED, "ada zeta", 0, null).getContent()).hasSize(2);
        assertThat(overview(UNFILTERED, "cy alp", 0, null).getContent())
                .extracting(CertificationOverviewRowDto::getUserId)
                .containsExactly(3L);
        assertThat(overview(UNFILTERED, "nobody", 0, null).getContent()).isEmpty();
    }

    @Test
    void pagesInMemoryWithPageMetadata() {
        PageDto<CertificationOverviewRowDto> first = overview(UNFILTERED, null, 0, 3);
        assertThat(first.getContent()).hasSize(3);
        assertThat(first.isHasNext()).isTrue();
        assertThat(first.getTotalPages()).isEqualTo(2);

        PageDto<CertificationOverviewRowDto> second = overview(UNFILTERED, null, 1, 3);
        assertThat(second.getContent()).hasSize(1);
        assertThat(second.isHasNext()).isFalse();
        assertThat(second.getPage()).isEqualTo(1);

        assertThat(overview(UNFILTERED, null, 5, 3).getContent()).isEmpty();
    }

    @Test
    void countsIgnoreFilterAndExcludeInReview() {
        CertificationOverviewCountsDto counts = service.getStatusOverviewCounts(ORG_ID);

        assertThat(counts.getAll()).isEqualTo(4);
        assertThat(counts.getMissing()).isEqualTo(1);
        assertThat(counts.getExpired()).isEqualTo(1);
        assertThat(counts.getExpiringSoon()).isEqualTo(1);
        assertThat(counts.getValid()).isEqualTo(1);
    }

    @Test
    void overviewRowsMatchPerUserStatusComputation() {
        List<CertificationOverviewRowDto> rows = overview(UNFILTERED, null, 0, null).getContent();

        for (Long userId : List.of(1L, 2L, 3L)) {
            List<CertificationStatusItemDto> expected =
                    service.getStatusForUser(userId).getItems().stream()
                            .filter(i -> i.isAssigned() && !i.isArchived())
                            .filter(
                                    i ->
                                            Set.of("MISSING", "EXPIRED", "EXPIRING_SOON", "VALID")
                                                    .contains(i.getStatus()))
                            .toList();
            assertThat(rows.stream().filter(r -> r.getUserId().equals(userId)).toList())
                    .extracting(
                            CertificationOverviewRowDto::getTenantCertDefinitionId,
                            CertificationOverviewRowDto::getStatus,
                            CertificationOverviewRowDto::getExpiryDate)
                    .containsExactlyInAnyOrderElementsOf(
                            expected.stream()
                                    .map(
                                            i ->
                                                    org.assertj.core.api.Assertions.tuple(
                                                            i.getTenantCertDefinitionId(),
                                                            i.getStatus(),
                                                            i.getExpiryDate()))
                                    .toList());
        }
    }

    private PageDto<CertificationOverviewRowDto> overview(
            CertificationOverviewListFilter filter, String search, int page, Integer size) {
        return service.getStatusOverview(ORG_ID, new ListQuery(page, size, search, null), filter);
    }

    private void assign(Long userId, Long definitionId) {
        assignments.add(
                UserCertificationAssignment.builder()
                        .userId(userId)
                        .tenantCertDefinitionId(definitionId)
                        .responsibleOrganizationId(ORG_ID)
                        .build());
    }

    private void record(Long userId, Long definitionId, LocalDate completed, LocalDate expiry) {
        records.add(
                UserCertificationRecord.builder()
                        .id(userId * 100 + definitionId)
                        .userId(userId)
                        .tenantCertDefinitionId(definitionId)
                        .completedDate(completed)
                        .expiryDate(expiry)
                        .build());
    }

    private static TenantCertDefinition definition(Long id, String name) {
        return TenantCertDefinition.builder()
                .id(id)
                .tenantId(1L)
                .name(name)
                .active(true)
                .archived(false)
                .reminderDaysBeforeExpiry(30)
                .build();
    }

    private static User user(Long id, String first, String last) {
        return User.builder().id(id).firstName(first).lastName(last).build();
    }

    private static UserOrganizationMembership membership(User user) {
        return UserOrganizationMembership.builder().user(user).build();
    }

    @Test
    void clientSortAscDescReversesOrder() {
        for (String key : List.of("name", "expiryDate")) {
            List<String> asc = sorted(key + ",asc").stream().map(r -> sortValue(key, r)).toList();
            List<String> desc = sorted(key + ",desc").stream().map(r -> sortValue(key, r)).toList();
            assertThat(asc).hasSize(4);
            assertThat(desc.reversed()).as(key).containsExactlyElementsOf(asc);
        }
        assertThat(sorted("expiryDate,asc"))
                .extracting(CertificationOverviewRowDto::getExpiryDate)
                .containsExactly(today.minusDays(10), today.plusDays(5), today.plusDays(100), null);
        for (String removed : List.of("certDefinition", "status", "bogus")) {
            assertThatThrownBy(() -> sorted(removed + ",asc"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static String sortValue(String key, CertificationOverviewRowDto row) {
        return switch (key) {
            case "name" -> row.getLastName() + "/" + row.getFirstName();
            default -> String.valueOf(row.getExpiryDate());
        };
    }

    private List<CertificationOverviewRowDto> sorted(String sort) {
        return service.getStatusOverview(ORG_ID, new ListQuery(0, null, null, sort), UNFILTERED)
                .getContent();
    }
}
