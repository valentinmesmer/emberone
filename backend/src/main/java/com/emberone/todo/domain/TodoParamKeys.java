/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.todo.domain;

import lombok.experimental.UtilityClass;

@UtilityClass
public class TodoParamKeys {

    public static final String WORKFLOW_ID = "workflowId";
    public static final String PARTICIPANT_ID = "participantId";
    public static final String WORKFLOW_TYPE_KEY = "workflowTypeKey";
    public static final String ORGANIZATION_GROUP_ID = "organizationGroupId";
    public static final String ORGANIZATION_GROUP_NAME = "organizationGroupName";
    public static final String OPENED_BY_DISPLAY_NAME = "openedByDisplayName";
    public static final String OPENED_BY_USER_ID = "openedByUserId";
    public static final String INPUT_CHANGE_MESSAGE = "inputChangeMessage";

    public static final String STATUS = "status";
    public static final String CERT_DEFINITION_ID = "certDefinitionId";
    public static final String CERT_DEFINITION_NAME = "certDefinitionName";

    public static final String WORKFLOW_TYPE_LABEL = "workflowTypeLabel";
    public static final String STATUS_LABEL = "statusLabel";
}
