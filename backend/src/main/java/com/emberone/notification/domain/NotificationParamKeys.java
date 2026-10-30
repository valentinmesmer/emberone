/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.domain;

import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NotificationParamKeys {

    public static final String TODO_TYPE_KEY = "todoTypeKey";
    public static final String WORKFLOW_ID = "workflowId";
    public static final String WORKFLOW_TYPE_KEY = "workflowTypeKey";
    public static final String OPENED_BY_DISPLAY_NAME = "openedByDisplayName";
    public static final String DECIDED_BY_DISPLAY_NAMES = "decidedByDisplayNames";

    /**
     * Human decision comments, chronologically ordered; each entry may include the approver name.
     */
    public static final String COMMENTS = "comments";

    public static final String INPUT_CHANGE_MESSAGE = "inputChangeMessage";
    public static final String PREVIOUS_USERNAME = "previousUsername";
    public static final String NEW_USERNAME = "newUsername";
    public static final String COURSE_OFFERING_TOPIC = "courseOfferingTopic";
    public static final String COURSE_REGISTRATION_REASON = "courseRegistrationReason";
    public static final String COURSE_OFFERING_CANCEL_REASON = "courseOfferingCancelReason";
    public static final String COURSE_OFFERING_START = "courseOfferingStart";

    public static final String WORKFLOW_TYPE_LABEL = "workflowTypeLabel";
    public static final String STATUS_LABEL = "statusLabel";
    public static final String DECIDED_BY_DISPLAY_NAME = "decidedByDisplayName";
    public static final String APPROVER_SUFFIX = "approverSuffix";
    public static final String COMMENT_LINE = "commentLine";

    private static final Set<String> DERIVED_DISPLAY_KEYS =
            Set.of(
                    WORKFLOW_TYPE_LABEL,
                    STATUS_LABEL,
                    DECIDED_BY_DISPLAY_NAME,
                    APPROVER_SUFFIX,
                    COMMENT_LINE);

    /**
     * Display-only keys produced by {@link
     * com.emberone.notification.type.NotificationTypeDefinition} implementations. Source keys are
     * declared per definition via {@code derivedSourceKeys()}.
     */
    public static boolean isDerivedDisplayKey(String key) {
        return DERIVED_DISPLAY_KEYS.contains(key);
    }
}
