/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.platform.web.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.emberone.absence.list.AbsenceListDefinition;
import com.emberone.admin.tenant.list.TenantListDefinition;
import com.emberone.admin.user.list.AdminUserListDefinition;
import com.emberone.appointment.list.AppointmentListDefinition;
import com.emberone.event.list.EventAttendanceListDefinition;
import com.emberone.event.list.OperationEventListDefinition;
import com.emberone.event.list.ServiceEventListDefinition;
import com.emberone.onboarding.list.OnboardingInvitationListDefinition;
import com.emberone.rank.list.OrganizationRankMemberListDefinition;
import com.emberone.timeentry.list.TimeEntryListDefinition;
import com.emberone.trainingcourse.list.TrainingCourseRecordListDefinition;
import com.emberone.user.list.OrganizationMemberListDefinition;
import com.emberone.userdocument.list.UserDocumentListDefinition;
import com.emberone.userorganizationgroupmembership.list.OrganizationGroupMemberListDefinition;
import com.emberone.workflow.list.WorkflowListDefinition;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/**
 * One check per list: every whitelisted key reverses with {@code desc}; removed or unknown keys are
 * rejected.
 */
class ListDefinitionsSortKeysTest {

    private record Case(
            ListDefinition<?> definition,
            String key,
            String firstProperty,
            List<String> rejected) {}

    @Test
    void descendingSortReversesOrderAndRemovedKeysAreRejected() {
        List<Case> cases =
                List.of(
                        new Case(
                                OrganizationMemberListDefinition.INSTANCE,
                                "lastName",
                                "lastName",
                                List.of("personalNumber")),
                        new Case(
                                AdminUserListDefinition.INSTANCE,
                                "lastName",
                                "surname",
                                List.of("username")),
                        new Case(
                                OrganizationGroupMemberListDefinition.INSTANCE,
                                "lastName",
                                "user.lastName",
                                List.of("startDate")),
                        new Case(
                                OrganizationRankMemberListDefinition.INSTANCE,
                                "firstName",
                                "user.firstName",
                                List.of("personalNumber")),
                        new Case(
                                OnboardingInvitationListDefinition.INSTANCE,
                                "lastName",
                                "user.lastName",
                                List.of("invitedAt", "status")),
                        new Case(
                                EventAttendanceListDefinition.INSTANCE,
                                "lastName",
                                "user.lastName",
                                List.of("firstName")),
                        new Case(
                                AppointmentListDefinition.INSTANCE,
                                "name",
                                "name",
                                List.of("location")),
                        new Case(
                                ServiceEventListDefinition.INSTANCE,
                                "startAt",
                                "start",
                                List.of("topic", "location")),
                        new Case(
                                OperationEventListDefinition.INSTANCE,
                                "startAt",
                                "start",
                                List.of("number", "keyword")),
                        new Case(
                                AbsenceListDefinition.INSTANCE,
                                "lastName",
                                "user.lastName",
                                List.of("endDate", "reason")),
                        new Case(
                                TimeEntryListDefinition.INSTANCE,
                                "lastName",
                                "user.lastName",
                                List.of("endAt")),
                        new Case(
                                TrainingCourseRecordListDefinition.INSTANCE,
                                "name",
                                "name",
                                List.of("startDate", "hours")),
                        new Case(
                                UserDocumentListDefinition.INSTANCE,
                                "title",
                                "title",
                                List.of("sizeBytes")),
                        new Case(
                                WorkflowListDefinition.INSTANCE,
                                "openedAt",
                                "openedAt",
                                List.of("decidedAt")),
                        new Case(
                                TenantListDefinition.INSTANCE,
                                "name",
                                "name",
                                List.of("abbreviation")));
        for (Case c : cases) {
            Sort asc =
                    c.definition().pageable(new ListQuery(0, 10, null, c.key() + ",asc")).getSort();
            Sort desc =
                    c.definition()
                            .pageable(new ListQuery(0, 10, null, c.key() + ",desc"))
                            .getSort();
            assertThat(asc.getOrderFor(c.firstProperty()).getDirection())
                    .as(c.key())
                    .isEqualTo(Sort.Direction.ASC);
            assertThat(desc.getOrderFor(c.firstProperty()).getDirection())
                    .as(c.key())
                    .isEqualTo(Sort.Direction.DESC);
            List<String> rejected = new java.util.ArrayList<>(c.rejected());
            rejected.add("nope");
            for (String key : rejected) {
                assertThatThrownBy(
                                () ->
                                        c.definition()
                                                .pageable(
                                                        new ListQuery(0, 10, null, key + ",desc")))
                        .as(key)
                        .isInstanceOf(IllegalArgumentException.class);
            }
        }
    }

    @Test
    void userDocumentDefaultsToNewestFirst() {
        Sort sort =
                UserDocumentListDefinition.INSTANCE
                        .pageable(new ListQuery(0, 10, null, null))
                        .getSort();
        assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
