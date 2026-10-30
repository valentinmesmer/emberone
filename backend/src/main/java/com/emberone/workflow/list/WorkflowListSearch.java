/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.list;

import com.emberone.platform.web.list.ListQuery;
import com.emberone.platform.web.list.ListSpecs;
import com.emberone.user.domain.User;
import com.emberone.workflow.domain.Workflow;
import com.emberone.workflow.domain.WorkflowType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Search over workflow instances: every word must match the workflow type label, the applicant's
 * name, the applicant's personal number, the workflow number (id) or the target user's display name
 * stored in the payload.
 */
@Component
@RequiredArgsConstructor
public class WorkflowListSearch {

    private final MessageSource messageSource;

    public Specification<Workflow> search(ListQuery query) {
        List<String> tokens = ListSpecs.searchTokens(query.search());
        if (tokens.isEmpty()) {
            return ListSpecs.unconstrained();
        }
        return (root, cq, cb) -> {
            List<Predicate> perToken = new ArrayList<>();
            for (String token : tokens) {
                perToken.add(tokenMatches(token, root, cq, cb));
            }
            return cb.and(perToken.toArray(Predicate[]::new));
        };
    }

    private Predicate tokenMatches(
            String token, Root<Workflow> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        List<Predicate> options = new ArrayList<>();
        List<Long> typeIds = matchingTypeIds(token);
        if (!typeIds.isEmpty()) {
            options.add(root.get("workflowTypeId").in(typeIds));
        }
        options.add(applicantMatches(token, root, cq, cb));
        options.add(targetUserMatches(token, root, cb));
        Long number = parseNumber(token);
        if (number != null) {
            options.add(cb.equal(root.get("id"), number));
        }
        return cb.or(options.toArray(Predicate[]::new));
    }

    /**
     * Target user display name snapshot in the payload JSON (organization-scoped workflows). The
     * key is matched together with the token, so only that field's value is searched.
     */
    private static Predicate targetUserMatches(
            String token, Root<Workflow> root, CriteriaBuilder cb) {
        String pattern = "%\"targetuserdisplayname\":\"%" + ListSpecs.escapeLike(token) + "%";
        return cb.like(cb.lower(root.get("payload")), pattern, '\\');
    }

    /** Type ids whose German label contains the token (tokens are already lower-cased). */
    private List<Long> matchingTypeIds(String token) {
        return Arrays.stream(WorkflowType.values())
                .filter(
                        type ->
                                messageSource
                                        .getMessage(
                                                "workflow.type." + type.name(),
                                                null,
                                                type.name(),
                                                Locale.GERMAN)
                                        .toLowerCase(Locale.ROOT)
                                        .contains(token))
                .map(WorkflowType::getId)
                .toList();
    }

    private Predicate applicantMatches(
            String token, Root<Workflow> root, CriteriaQuery<?> cq, CriteriaBuilder cb) {
        Subquery<Long> applicant = cq.subquery(Long.class);
        Root<User> user = applicant.from(User.class);
        String pattern = "%" + ListSpecs.escapeLike(token) + "%";
        List<Predicate> matches = new ArrayList<>();
        matches.add(cb.like(cb.lower(user.get("firstName")), pattern, '\\'));
        matches.add(cb.like(cb.lower(user.get("lastName")), pattern, '\\'));
        Long personalNumber = parseNumber(token);
        if (personalNumber != null) {
            matches.add(cb.equal(user.get("personalNumber"), personalNumber));
        }
        applicant
                .select(user.get("id"))
                .where(
                        cb.equal(user.get("id"), root.get("openedByUserId")),
                        cb.or(matches.toArray(Predicate[]::new)));
        return cb.exists(applicant);
    }

    private static Long parseNumber(String token) {
        if (token.isEmpty() || !token.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.valueOf(token);
        } catch (NumberFormatException overflow) {
            return null;
        }
    }
}
