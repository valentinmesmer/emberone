/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.workflow.spi;

import com.emberone.workflow.exception.WorkflowConfigurationException;
import java.util.ArrayList;
import java.util.List;

/**
 * Stable keys of the approver slot roles. A role says in which capacity an organization approves a
 * request; group, auto-approve and self-approve are configured per role in the standard.
 *
 * <ul>
 *   <li>{@link #CONTEXT}: the context organization of the request (every type has it).
 *   <li>{@link #ADDITIONAL}: extra orgs from the tenant workflow settings and routing rules (the
 *       engine appends it to every type).
 *   <li>Handler roles for dynamic approver orgs, declared by {@link
 *       WorkflowHandler#approverSlotRoles()}: {@link #NEW_MAIN_ORG}, {@link #TARGET_ORG}, {@link
 *       #HOME_ORG}.
 * </ul>
 *
 * The keys are stored in the database and returned by the API; never rename them.
 */
public final class WorkflowApproverSlotRoles {

    public static final String CONTEXT = "CONTEXT";
    public static final String ADDITIONAL = "ADDITIONAL";

    /** The main organization the user changes to (main organization change workflows). */
    public static final String NEW_MAIN_ORG = "NEW_MAIN_ORG";

    /** The organization a join request is addressed to. */
    public static final String TARGET_ORG = "TARGET_ORG";

    /** The home organization of the course participant (course registration / cancellation). */
    public static final String HOME_ORG = "HOME_ORG";

    private WorkflowApproverSlotRoles() {}

    /**
     * All roles of a handler in display order: the handler roles ({@code CONTEXT} first), then
     * {@code ADDITIONAL}.
     */
    public static List<String> rolesOf(WorkflowHandler<?> handler) {
        List<String> declared = handler.approverSlotRoles();
        List<String> roles = new ArrayList<>();
        if (declared == null || !declared.contains(CONTEXT)) {
            roles.add(CONTEXT);
        }
        if (declared != null) {
            for (String role : declared) {
                if (!ADDITIONAL.equals(role) && !roles.contains(role)) {
                    roles.add(role);
                }
            }
        }
        roles.add(ADDITIONAL);
        return List.copyOf(roles);
    }

    /** Rejects handlers that declare a role twice or declare the engine role {@code ADDITIONAL}. */
    static void validateDeclaredRoles(WorkflowHandler<?> handler) {
        List<String> declared = handler.approverSlotRoles();
        if (declared == null || !declared.contains(CONTEXT)) {
            throw new WorkflowConfigurationException(
                    "Workflow handler for type id "
                            + handler.workflowTypeId()
                            + " must declare the approver slot role "
                            + CONTEXT);
        }
        if (declared.contains(ADDITIONAL)) {
            throw new WorkflowConfigurationException(
                    "Workflow handler for type id "
                            + handler.workflowTypeId()
                            + " must not declare "
                            + ADDITIONAL
                            + "; the engine adds it");
        }
        if (declared.stream().distinct().count() != declared.size()) {
            throw new WorkflowConfigurationException(
                    "Workflow handler for type id "
                            + handler.workflowTypeId()
                            + " declares an approver slot role twice");
        }
    }
}
