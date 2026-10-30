/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.ListSpecs;
import com.emberone.workflow.domain.ParticipantRole;
import com.emberone.workflow.domain.PrincipalType;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowParticipant;
import com.emberone.workflow.domain.WorkflowStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class WorkflowListSpecs {

    private WorkflowListSpecs() {}

    public static Specification<Workflow> openedByUser(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("openedByUserId"), userId);
    }

    public static Specification<Workflow> workflowStatusesFilter(List<WorkflowStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return ListSpecs.unconstrained();
        }
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Workflow> workflowTypesFilter(List<Long> workflowTypeIds) {
        if (workflowTypeIds == null || workflowTypeIds.isEmpty()) {
            return ListSpecs.unconstrained();
        }
        return (root, query, cb) -> root.get("workflowTypeId").in(workflowTypeIds);
    }

    public static Specification<Workflow> routedThroughOrganization(Long organizationId) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Workflow, WorkflowParticipant> participants =
                    root.join("participants", JoinType.INNER);
            return cb.equal(participants.get("organizationId"), organizationId);
        };
    }

    public static Specification<Workflow> openedByUserInOrganization(
            Long userId, Long organizationId) {
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Workflow, WorkflowParticipant> participants =
                    root.join("participants", JoinType.INNER);
            return cb.and(
                    cb.equal(root.get("openedByUserId"), userId),
                    cb.equal(participants.get("organizationId"), organizationId));
        };
    }

    public static Specification<Workflow> participantCandidateForUser(
            Long userId, List<Long> groupIds) {
        List<Long> effectiveGroupIds =
                groupIds == null || groupIds.isEmpty() ? List.of(-1L) : groupIds;
        return (root, query, cb) -> {
            query.distinct(true);
            Join<Workflow, WorkflowParticipant> participant =
                    root.join("participants", JoinType.INNER);
            List<Predicate> slotMatches = new ArrayList<>();
            slotMatches.add(
                    cb.and(
                            cb.equal(participant.get("role"), ParticipantRole.VIEWER),
                            cb.equal(participant.get("principalType"), PrincipalType.USER),
                            cb.equal(participant.get("principalId"), userId)));
            slotMatches.add(
                    cb.and(
                            cb.equal(participant.get("role"), ParticipantRole.APPROVER),
                            cb.equal(participant.get("principalType"), PrincipalType.USER),
                            cb.equal(participant.get("principalId"), userId)));
            slotMatches.add(
                    cb.and(
                            cb.equal(participant.get("role"), ParticipantRole.APPROVER),
                            cb.equal(
                                    participant.get("principalType"),
                                    PrincipalType.ORGANIZATION_GROUP),
                            participant.get("principalId").in(effectiveGroupIds)));
            return cb.or(slotMatches.toArray(Predicate[]::new));
        };
    }
}
