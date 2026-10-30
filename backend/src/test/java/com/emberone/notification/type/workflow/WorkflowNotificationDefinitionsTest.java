/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.type.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.i18n.NotificationDisplaySupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowNotificationDefinitionsTest {

    @Mock NotificationDisplaySupport displaySupport;

    WorkflowOpenedNotificationDefinition openedDefinition;
    WorkflowApprovedNotificationDefinition approvedDefinition;
    WorkflowRejectedNotificationDefinition rejectedDefinition;

    @BeforeEach
    void setUp() {
        openedDefinition = new WorkflowOpenedNotificationDefinition(displaySupport, null);
        approvedDefinition = new WorkflowApprovedNotificationDefinition(displaySupport, null);
        rejectedDefinition = new WorkflowRejectedNotificationDefinition(displaySupport, null);
    }

    @Test
    void openedDefinitionExposesWorkflowSourceKeysWithoutComment() {
        assertThat(openedDefinition.type()).isEqualTo(NotificationType.WORKFLOW_OPENED);
        assertThat(openedDefinition.derivedSourceKeys())
                .containsExactly(
                        NotificationParamKeys.WORKFLOW_TYPE_KEY,
                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES);
    }

    @Test
    void approvedDefinitionIncludesCommentsInDerivedSourceKeys() {
        assertThat(approvedDefinition.derivedSourceKeys())
                .containsExactly(
                        NotificationParamKeys.WORKFLOW_TYPE_KEY,
                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES,
                        NotificationParamKeys.COMMENTS);
    }

    @Test
    void enrichAddsWorkflowTypeLabelForOpenedNotification() {
        Map<String, Object> rawParams =
                Map.of(NotificationParamKeys.WORKFLOW_TYPE_KEY, "NAME_CHANGE");
        Map<String, Object> display = new HashMap<>();

        when(displaySupport.resolveWorkflowTypeLabel("NAME_CHANGE", SupportedLocale.DE))
                .thenReturn("Namensänderung");
        when(displaySupport.formatDisplayNameList(List.of(), SupportedLocale.DE)).thenReturn("");

        openedDefinition.enrichDisplayParams(rawParams, display, SupportedLocale.DE);

        assertThat(display)
                .containsEntry(NotificationParamKeys.WORKFLOW_TYPE_LABEL, "Namensänderung")
                .doesNotContainKeys(
                        NotificationParamKeys.APPROVER_SUFFIX, NotificationParamKeys.COMMENT_LINE);
        verify(displaySupport).resolveWorkflowTypeLabel("NAME_CHANGE", SupportedLocale.DE);
        verify(displaySupport, never()).resolveApproverSuffix(any(), any());
        verify(displaySupport, never()).resolveCommentsLine(any(), any());
    }

    @Test
    void enrichAddsDecisionFieldsForApprovedNotification() {
        Map<String, Object> rawParams =
                Map.of(
                        NotificationParamKeys.WORKFLOW_TYPE_KEY, "NAME_CHANGE",
                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES, List.of("Anna Approver"),
                        NotificationParamKeys.COMMENTS, List.of("Sieht gut aus"));
        Map<String, Object> display = new HashMap<>();

        when(displaySupport.resolveWorkflowTypeLabel("NAME_CHANGE", SupportedLocale.DE))
                .thenReturn("Namensänderung");
        when(displaySupport.formatDisplayNameList(List.of("Anna Approver"), SupportedLocale.DE))
                .thenReturn("Anna Approver");
        when(displaySupport.resolveApproverSuffix("Anna Approver", SupportedLocale.DE))
                .thenReturn(" von Anna Approver.");
        when(displaySupport.resolveCommentsLine(List.of("Sieht gut aus"), SupportedLocale.DE))
                .thenReturn("\nKommentar: Sieht gut aus");

        approvedDefinition.enrichDisplayParams(rawParams, display, SupportedLocale.DE);

        assertThat(display)
                .containsAllEntriesOf(
                        Map.of(
                                NotificationParamKeys.WORKFLOW_TYPE_LABEL, "Namensänderung",
                                NotificationParamKeys.DECIDED_BY_DISPLAY_NAME, "Anna Approver",
                                NotificationParamKeys.APPROVER_SUFFIX, " von Anna Approver.",
                                NotificationParamKeys.COMMENT_LINE, "\nKommentar: Sieht gut aus"));
    }

    @Test
    void enrichOmitsDecidedByDisplayNameWhenNameListIsEmpty() {
        Map<String, Object> rawParams =
                Map.of(
                        NotificationParamKeys.WORKFLOW_TYPE_KEY,
                        "NAME_CHANGE",
                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES,
                        List.of());
        Map<String, Object> display = new HashMap<>();

        when(displaySupport.resolveWorkflowTypeLabel("NAME_CHANGE", SupportedLocale.DE))
                .thenReturn("Namensänderung");
        when(displaySupport.formatDisplayNameList(List.of(), SupportedLocale.DE)).thenReturn("");
        when(displaySupport.resolveApproverSuffix("", SupportedLocale.DE)).thenReturn(".");
        when(displaySupport.resolveCommentsLine(List.of(), SupportedLocale.DE)).thenReturn("");

        rejectedDefinition.enrichDisplayParams(rawParams, display, SupportedLocale.DE);

        assertThat(display)
                .containsEntry(NotificationParamKeys.WORKFLOW_TYPE_LABEL, "Namensänderung")
                .containsEntry(NotificationParamKeys.APPROVER_SUFFIX, ".")
                .containsEntry(NotificationParamKeys.COMMENT_LINE, "")
                .doesNotContainKey(NotificationParamKeys.DECIDED_BY_DISPLAY_NAME);
    }
}
