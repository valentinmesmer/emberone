/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.userorganizationmembership.schedule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.platform.tenant.TenantBatchRunner;
import com.emberone.userorganizationgroupmembership.service.UserOrganizationGroupMembershipService;
import com.emberone.userorganizationmembership.service.UserOrganizationMembershipService;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class MembershipEndCleanupSchedulerTest {

    @Test
    void scheduledInvocationRunsOrgThenGroupCleanupPerTenant() {
        UserOrganizationMembershipService organizationMembershipService =
                mock(UserOrganizationMembershipService.class);
        UserOrganizationGroupMembershipService groupMembershipService =
                mock(UserOrganizationGroupMembershipService.class);
        TenantBatchRunner batchRunner = mock(TenantBatchRunner.class);
        MembershipEndCleanupScheduler scheduler =
                new MembershipEndCleanupScheduler(
                        batchRunner, organizationMembershipService, groupMembershipService);

        doAnswer(
                        invocation -> {
                            Consumer<Long> action = invocation.getArgument(0);
                            action.accept(7L);
                            action.accept(8L);
                            return null;
                        })
                .when(batchRunner)
                .forEachTenant(any());

        scheduler.cleanupEndedMemberships();

        verify(batchRunner).forEachTenant(any());
        InOrder order = inOrder(organizationMembershipService, groupMembershipService);
        order.verify(organizationMembershipService).processDueEndedMemberships();
        order.verify(groupMembershipService).processDueEndedMemberships();
        order.verify(organizationMembershipService).processDueEndedMemberships();
        order.verify(groupMembershipService).processDueEndedMemberships();
    }
}
