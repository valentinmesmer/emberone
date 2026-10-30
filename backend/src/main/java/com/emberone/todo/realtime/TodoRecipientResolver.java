/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.realtime;

import com.emberone.todo.domain.Todo;
import com.emberone.todo.domain.TodoAssigneeGroup;
import com.emberone.todo.domain.TodoExcludedUser;
import com.emberone.userorganizationgroupmembership.domain.UserOrganizationGroupMembership;
import com.emberone.userorganizationgroupmembership.repository.UserOrganizationGroupMembershipRepository;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TodoRecipientResolver {

    private final UserOrganizationGroupMembershipRepository membershipRepository;

    private static void excludeHiddenUsers(Todo todo, Set<Long> recipients) {
        if (todo.getExcludedUsers() == null) {
            return;
        }
        for (TodoExcludedUser excluded : todo.getExcludedUsers()) {
            if (excluded.getUserId() != null) {
                recipients.remove(excluded.getUserId());
            }
        }
    }

    private static Set<Long> organizationGroupIdsOf(Todo todo) {
        Set<Long> groupIds = new LinkedHashSet<>();
        if (todo.getAssigneeGroups() == null) {
            return groupIds;
        }
        for (TodoAssigneeGroup group : todo.getAssigneeGroups()) {
            if (group.getOrganizationGroupId() != null) {
                groupIds.add(group.getOrganizationGroupId());
            }
        }
        return groupIds;
    }

    public Set<Long> resolveRecipientUserIds(Todo todo) {
        Set<Long> recipients = new LinkedHashSet<>();
        if (todo.getAssigneeUserId() != null) {
            recipients.add(todo.getAssigneeUserId());
        }
        recipients.addAll(resolveActiveMembersOfGroups(organizationGroupIdsOf(todo)));
        excludeHiddenUsers(todo, recipients);
        return recipients;
    }

    /**
     * Active members of the given groups, with the same exclusion as {@link
     * #resolveRecipientUserIds(Todo)} (assignee user is not included).
     */
    public Set<Long> resolveGroupRecipientUserIds(Todo todo, Set<Long> organizationGroupIds) {
        Set<Long> recipients =
                new LinkedHashSet<>(resolveActiveMembersOfGroups(organizationGroupIds));
        excludeHiddenUsers(todo, recipients);
        return recipients;
    }

    private Set<Long> resolveActiveMembersOfGroups(Set<Long> organizationGroupIds) {
        Set<Long> recipients = new LinkedHashSet<>();
        if (organizationGroupIds == null || organizationGroupIds.isEmpty()) {
            return recipients;
        }
        Instant now = Instant.now();
        for (Long groupId : organizationGroupIds) {
            if (groupId == null) {
                continue;
            }
            for (UserOrganizationGroupMembership membership :
                    membershipRepository.findByOrganizationGroup_IdWithUser(groupId)) {
                if (membership.isActiveAt(now) && membership.getUser() != null) {
                    recipients.add(membership.getUser().getId());
                }
            }
        }
        return recipients;
    }
}
