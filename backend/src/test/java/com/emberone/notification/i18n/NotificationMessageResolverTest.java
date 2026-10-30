/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.notification.i18n;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.emberone.admin.tenant.domain.SupportedLocale;
import com.emberone.notification.domain.Notification;
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.notification.domain.NotificationType;
import com.emberone.notification.type.NotificationTypeDefinitionRegistry;
import com.emberone.notification.type.course.CourseOfferingCancelledNotificationDefinition;
import com.emberone.notification.type.course.CourseOfferingSessionsChangedNotificationDefinition;
import com.emberone.notification.type.course.CourseOfferingStartReminderNotificationDefinition;
import com.emberone.notification.type.course.CourseRegistrationRemovedNotificationDefinition;
import com.emberone.notification.type.identity.UsernameChangedNotificationDefinition;
import com.emberone.notification.type.todo.TodoAssignedNotificationDefinition;
import com.emberone.notification.type.workflow.*;
import com.emberone.platform.i18n.LocalizationService;
import com.emberone.platform.mail.i18n.MailMessageResolver;
import com.emberone.todo.domain.TodoParamKeys;
import com.emberone.todo.domain.TodoType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationMessageResolverTest {

    @Mock LocalizationService localizationService;
    @Mock MailMessageResolver mailMessageResolver;

    NotificationMessageResolver resolver;

    @BeforeEach
    void setUp() {
        NotificationDisplaySupport displaySupport =
                new NotificationDisplaySupport(localizationService);
        NotificationTypeDefinitionRegistry registry =
                new NotificationTypeDefinitionRegistry(
                        List.of(
                                new WorkflowOpenedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowApprovedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowRejectedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowCancelledNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new WorkflowInputChangeRequestedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new TodoAssignedNotificationDefinition(
                                        displaySupport, mailMessageResolver),
                                new UsernameChangedNotificationDefinition(mailMessageResolver),
                                new CourseRegistrationRemovedNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingCancelledNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingSessionsChangedNotificationDefinition(
                                        mailMessageResolver),
                                new CourseOfferingStartReminderNotificationDefinition(
                                        mailMessageResolver)));
        resolver = new NotificationMessageResolver(localizationService, displaySupport, registry);
    }

    @Test
    void resolvesTodoAssignedUsingTodoMessageKeys() {
        Notification notification =
                Notification.builder()
                        .type(NotificationType.TODO_ASSIGNED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.TODO_TYPE_KEY,
                                                TodoType.WORKFLOW_START_REQUESTED.name(),
                                        TodoParamKeys.WORKFLOW_TYPE_KEY, "NAME_CHANGE",
                                        TodoParamKeys.OPENED_BY_DISPLAY_NAME, "Max Mustermann",
                                        TodoParamKeys.ORGANIZATION_GROUP_NAME, "Leitung"))
                        .build();

        when(localizationService.getMessageOrNull("workflow.type.NAME_CHANGE", SupportedLocale.DE))
                .thenReturn("Namensänderung");
        when(localizationService.getMessageOrNull(
                        "todo.WORKFLOW_START_REQUESTED.title", SupportedLocale.DE))
                .thenReturn("Freigabe {workflowTypeLabel} übernehmen");
        when(localizationService.getMessageOrNull(
                        "todo.WORKFLOW_START_REQUESTED.body", SupportedLocale.DE))
                .thenReturn(
                        "{openedByDisplayName} wartet auf eine Freigabe in der Gruppe {organizationGroupName}. Bitte übernimm den Vorgang.");
        when(localizationService.getInterpolatedMessage(
                        eq("todo.WORKFLOW_START_REQUESTED.title"),
                        eq(
                                Map.of(
                                        TodoParamKeys.OPENED_BY_DISPLAY_NAME, "Max Mustermann",
                                        TodoParamKeys.ORGANIZATION_GROUP_NAME, "Leitung",
                                        TodoParamKeys.WORKFLOW_TYPE_LABEL, "Namensänderung")),
                        eq(SupportedLocale.DE)))
                .thenReturn("Freigabe Namensänderung übernehmen");
        when(localizationService.getInterpolatedMessage(
                        eq("todo.WORKFLOW_START_REQUESTED.body"),
                        eq(
                                Map.of(
                                        TodoParamKeys.OPENED_BY_DISPLAY_NAME, "Max Mustermann",
                                        TodoParamKeys.ORGANIZATION_GROUP_NAME, "Leitung",
                                        TodoParamKeys.WORKFLOW_TYPE_LABEL, "Namensänderung")),
                        eq(SupportedLocale.DE)))
                .thenReturn(
                        "Max Mustermann wartet auf eine Freigabe in der Gruppe Leitung. Bitte übernimm den Vorgang.");

        assertThat(resolver.resolveTitle(notification, SupportedLocale.DE))
                .isEqualTo("Freigabe Namensänderung übernehmen");
        assertThat(resolver.resolveBody(notification, SupportedLocale.DE))
                .isEqualTo(
                        "Max Mustermann wartet auf eine Freigabe in der Gruppe Leitung. Bitte übernimm den Vorgang.");
    }

    @Test
    void resolvesCertificationExpiringTodoWithLocalizedStatusLabel() {
        Notification notification =
                Notification.builder()
                        .type(NotificationType.TODO_ASSIGNED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.TODO_TYPE_KEY,
                                                TodoType.CERTIFICATION_EXPIRING.name(),
                                        TodoParamKeys.CERT_DEFINITION_NAME, "G.26.3",
                                        TodoParamKeys.STATUS, "EXPIRED"))
                        .build();

        when(localizationService.getMessageOrNull(
                        "certification.status.EXPIRED", SupportedLocale.DE))
                .thenReturn("abgelaufen");
        when(localizationService.getMessageOrNull(
                        "todo.CERTIFICATION_EXPIRING.body", SupportedLocale.DE))
                .thenReturn(
                        "Dein Nachweis fuer \"{certDefinitionName}\" ist {statusLabel}. Bitte reiche einen aktuellen Nachweis ein.");
        when(localizationService.getInterpolatedMessage(
                        eq("todo.CERTIFICATION_EXPIRING.body"),
                        eq(
                                Map.of(
                                        TodoParamKeys.CERT_DEFINITION_NAME, "G.26.3",
                                        TodoParamKeys.STATUS_LABEL, "abgelaufen")),
                        eq(SupportedLocale.DE)))
                .thenReturn(
                        "Dein Nachweis fuer \"G.26.3\" ist abgelaufen. Bitte reiche einen aktuellen Nachweis ein.");

        assertThat(resolver.resolveBody(notification, SupportedLocale.DE))
                .isEqualTo(
                        "Dein Nachweis fuer \"G.26.3\" ist abgelaufen. Bitte reiche einen aktuellen Nachweis ein.");
    }

    @Test
    void interpolatesScalarParamsWithoutEnricher() {
        Notification notification =
                Notification.builder()
                        .type(NotificationType.USERNAME_CHANGED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.PREVIOUS_USERNAME, "old.user",
                                        NotificationParamKeys.NEW_USERNAME, "new.user"))
                        .build();
        when(localizationService.getMessageOrNull(
                        "notification.USERNAME_CHANGED.body", SupportedLocale.DE))
                .thenReturn(
                        "Bisheriger Nutzername: {previousUsername}\nNeuer Nutzername: {newUsername}");
        when(localizationService.getInterpolatedMessage(
                        "notification.USERNAME_CHANGED.body",
                        Map.of(
                                NotificationParamKeys.PREVIOUS_USERNAME, "old.user",
                                NotificationParamKeys.NEW_USERNAME, "new.user"),
                        SupportedLocale.DE))
                .thenReturn("Bisheriger Nutzername: old.user\nNeuer Nutzername: new.user");

        assertThat(resolver.resolveBody(notification, SupportedLocale.DE))
                .isEqualTo("Bisheriger Nutzername: old.user\nNeuer Nutzername: new.user");
    }

    @Test
    void returnsTypeNameWhenTemplateMissing() {
        Notification notification =
                Notification.builder()
                        .type(NotificationType.USERNAME_CHANGED)
                        .params(Map.of(NotificationParamKeys.NEW_USERNAME, "ada"))
                        .build();
        when(localizationService.getMessageOrNull(
                        "notification.USERNAME_CHANGED.title", SupportedLocale.DE))
                .thenReturn(null);

        assertThat(resolver.resolveTitle(notification, SupportedLocale.DE))
                .isEqualTo("USERNAME_CHANGED");
    }

    @Test
    void excludesDerivedSourceKeysAndAppliesWorkflowEnricher() {
        Notification notification =
                Notification.builder()
                        .type(NotificationType.WORKFLOW_APPROVED)
                        .params(
                                Map.of(
                                        NotificationParamKeys.WORKFLOW_ID,
                                        42L,
                                        NotificationParamKeys.WORKFLOW_TYPE_KEY,
                                        "NAME_CHANGE",
                                        NotificationParamKeys.OPENED_BY_DISPLAY_NAME,
                                        "Max Mustermann",
                                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES,
                                        List.of("Anna Approver"),
                                        NotificationParamKeys.COMMENTS,
                                        List.of("Sieht gut aus")))
                        .build();

        when(localizationService.getMessageOrNull("workflow.type.NAME_CHANGE", SupportedLocale.DE))
                .thenReturn("Namensänderung");
        when(localizationService.getMessageOrNull(
                        "notification.WORKFLOW_APPROVED.body", SupportedLocale.DE))
                .thenReturn(
                        "Dein Antrag auf {workflowTypeLabel} wurde genehmigt{approverSuffix}{commentLine}");
        when(localizationService.getInterpolatedMessage(
                        eq("notification.approverSuffix"),
                        eq(Map.of("names", "Anna Approver")),
                        eq(SupportedLocale.DE)))
                .thenReturn(" von Anna Approver.");
        when(localizationService.getInterpolatedMessage(
                        eq("notification.commentLine"),
                        eq(Map.of("comment", "Sieht gut aus")),
                        eq(SupportedLocale.DE)))
                .thenReturn("\nKommentar: Sieht gut aus");
        when(localizationService.getInterpolatedMessage(
                        eq("notification.WORKFLOW_APPROVED.body"),
                        eq(
                                Map.of(
                                        NotificationParamKeys.WORKFLOW_ID, 42L,
                                        NotificationParamKeys.OPENED_BY_DISPLAY_NAME,
                                                "Max Mustermann",
                                        NotificationParamKeys.WORKFLOW_TYPE_LABEL, "Namensänderung",
                                        NotificationParamKeys.DECIDED_BY_DISPLAY_NAME,
                                                "Anna Approver",
                                        NotificationParamKeys.APPROVER_SUFFIX,
                                                " von Anna Approver.",
                                        NotificationParamKeys.COMMENT_LINE,
                                                "\nKommentar: Sieht gut aus")),
                        eq(SupportedLocale.DE)))
                .thenReturn(
                        "Dein Antrag auf Namensänderung wurde genehmigt von Anna Approver. Kommentar: Sieht gut aus");

        assertThat(resolver.resolveBody(notification, SupportedLocale.DE))
                .isEqualTo(
                        "Dein Antrag auf Namensänderung wurde genehmigt von Anna Approver. Kommentar: Sieht gut aus");
    }
}
