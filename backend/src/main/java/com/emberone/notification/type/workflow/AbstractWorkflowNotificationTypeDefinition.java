/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type.workflow;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.i18n.NotificationDisplaySupport;
import com.emberone.notification.type.AbstractNotificationTypeDefinition;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.util.StringUtils;

/**
 * Shared display-param enrichment for workflow lifecycle notifications. Subclasses only declare
 * their {@link NotificationType} and whether decision context applies.
 */
public abstract class AbstractWorkflowNotificationTypeDefinition
        extends AbstractNotificationTypeDefinition {

    private final boolean includesDecisionContext;
    private final NotificationDisplaySupport displaySupport;

    protected AbstractWorkflowNotificationTypeDefinition(
            NotificationType type,
            boolean includesDecisionContext,
            NotificationDisplaySupport displaySupport,
            MailMessageResolver mailMessageResolver) {
        super(type, IN_APP_AND_EMAIL, mailMessageResolver);
        this.includesDecisionContext = includesDecisionContext;
        this.displaySupport = displaySupport;
    }

    @Override
    public Collection<String> derivedSourceKeys() {
        if (includesDecisionContext) {
            return List.of(
                    NotificationParamKeys.WORKFLOW_TYPE_KEY,
                    NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES,
                    NotificationParamKeys.COMMENTS);
        }
        return List.of(
                NotificationParamKeys.WORKFLOW_TYPE_KEY,
                NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES);
    }

    @Override
    public void enrichDisplayParams(
            Map<String, Object> rawParams, Map<String, Object> display, SupportedLocale locale) {
        Object workflowTypeKey = rawParams.get(NotificationParamKeys.WORKFLOW_TYPE_KEY);
        if (workflowTypeKey instanceof String key && StringUtils.hasText(key)) {
            display.put(
                    NotificationParamKeys.WORKFLOW_TYPE_LABEL,
                    displaySupport.resolveWorkflowTypeLabel(key, locale));
        }

        List<String> decidedByNames =
                NotificationDisplaySupport.extractDisplayNameList(
                        rawParams, NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES);
        String formattedDecisionNames =
                displaySupport.formatDisplayNameList(decidedByNames, locale);
        if (StringUtils.hasText(formattedDecisionNames)) {
            display.put(NotificationParamKeys.DECIDED_BY_DISPLAY_NAME, formattedDecisionNames);
        }

        if (includesDecisionContext) {
            display.put(
                    NotificationParamKeys.APPROVER_SUFFIX,
                    displaySupport.resolveApproverSuffix(formattedDecisionNames, locale));
            List<String> comments =
                    NotificationDisplaySupport.extractStringList(
                            rawParams, NotificationParamKeys.COMMENTS);
            display.put(
                    NotificationParamKeys.COMMENT_LINE,
                    displaySupport.resolveCommentsLine(comments, locale));
        }
    }
}
