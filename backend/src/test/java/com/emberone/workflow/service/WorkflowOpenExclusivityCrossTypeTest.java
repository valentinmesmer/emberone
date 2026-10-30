/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.emberone.workflow.dto.MainOrganizationChangeWorkflowPayload;
import com.emberone.workflow.dto.OrganizationLeaveRequestWorkflowPayload;
import com.emberone.workflow.dto.OrganizationMembershipEndRequestWorkflowPayload;
import com.emberone.workflow.dto.OrganizationUserMainOrganizationChangeWorkflowPayload;
import com.emberone.workflow.mainorganizationchange.MainOrganizationChangeWorkflowHandler;
import com.emberone.workflow.organizationleaverequest.OrganizationLeaveRequestWorkflowHandler;
import com.emberone.workflow.organizationmembershipend.OrganizationMembershipEndRequestWorkflowHandler;
import com.emberone.workflow.organizationusermainorganizationchange.OrganizationUserMainOrganizationChangeWorkflowHandler;
import com.emberone.workflow.spi.WorkflowOpenContext;
import com.emberone.workflow.spi.WorkflowOpenExclusivityKeys;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Self-service and org-management workflow types that share the same business operation must use
 * identical exclusivity keys.
 */
class WorkflowOpenExclusivityCrossTypeTest {

    private static final long MEMBER_USER_ID = 42L;
    private static final long ORGANIZATION_ID = 99L;

    @Test
    void membershipExitSelfAndOrgShareExclusivityKey() {
        OrganizationLeaveRequestWorkflowHandler selfHandler = handlerWithoutMocks();
        OrganizationMembershipEndRequestWorkflowHandler orgHandler = orgHandlerWithoutMocks();

        WorkflowOpenContext<OrganizationLeaveRequestWorkflowPayload> selfCtx =
                new WorkflowOpenContext<>(
                        MEMBER_USER_ID,
                        1L,
                        OrganizationLeaveRequestWorkflowPayload.builder()
                                .organizationId(ORGANIZATION_ID)
                                .build());

        WorkflowOpenContext<OrganizationMembershipEndRequestWorkflowPayload> orgCtx =
                new WorkflowOpenContext<>(
                        7L,
                        1L,
                        OrganizationMembershipEndRequestWorkflowPayload.builder()
                                .organizationId(ORGANIZATION_ID)
                                .targetUserId(MEMBER_USER_ID)
                                .build());

        List<String> selfKeys = selfHandler.resolveOpenExclusivityKeys(selfCtx, null);
        List<String> orgKeys = orgHandler.resolveOpenExclusivityKeys(orgCtx, null);

        assertThat(selfKeys).isEqualTo(orgKeys);
        assertThat(selfKeys)
                .containsExactly(
                        WorkflowOpenExclusivityKeys.organizationMembershipExit(
                                MEMBER_USER_ID, ORGANIZATION_ID));
    }

    @Test
    void mainOrganizationSelfAndOrgShareExclusivityKey() {
        MainOrganizationChangeWorkflowHandler selfHandler = mainOrgSelfHandlerWithoutMocks();
        OrganizationUserMainOrganizationChangeWorkflowHandler orgHandler =
                mainOrgOrgHandlerWithoutMocks();

        WorkflowOpenContext<MainOrganizationChangeWorkflowPayload> selfCtx =
                new WorkflowOpenContext<>(MEMBER_USER_ID, 1L, null);

        WorkflowOpenContext<OrganizationUserMainOrganizationChangeWorkflowPayload> orgCtx =
                new WorkflowOpenContext<>(
                        7L,
                        1L,
                        OrganizationUserMainOrganizationChangeWorkflowPayload.builder()
                                .targetUserId(MEMBER_USER_ID)
                                .build());

        List<String> selfKeys = selfHandler.resolveOpenExclusivityKeys(selfCtx, null);
        List<String> orgKeys = orgHandler.resolveOpenExclusivityKeys(orgCtx, null);

        assertThat(selfKeys).isEqualTo(orgKeys);
        assertThat(selfKeys)
                .containsExactly(
                        WorkflowOpenExclusivityKeys.userMainOrganizationChange(MEMBER_USER_ID));
    }

    private static OrganizationLeaveRequestWorkflowHandler handlerWithoutMocks() {
        return new OrganizationLeaveRequestWorkflowHandler(
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static OrganizationMembershipEndRequestWorkflowHandler orgHandlerWithoutMocks() {
        return new OrganizationMembershipEndRequestWorkflowHandler(
                null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static MainOrganizationChangeWorkflowHandler mainOrgSelfHandlerWithoutMocks() {
        return new MainOrganizationChangeWorkflowHandler(
                null, null, null, null, null, null, null, null, null);
    }

    private static OrganizationUserMainOrganizationChangeWorkflowHandler
            mainOrgOrgHandlerWithoutMocks() {
        return new OrganizationUserMainOrganizationChangeWorkflowHandler(
                null, null, null, null, null, null, null, null, null, null);
    }
}
