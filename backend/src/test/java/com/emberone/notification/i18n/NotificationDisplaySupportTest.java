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
import com.emberone.notification.domain.NotificationParamKeys;
import com.emberone.platform.i18n.LocalizationService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationDisplaySupportTest {

    @Mock LocalizationService localizationService;
    @InjectMocks NotificationDisplaySupport displaySupport;

    @Test
    void resolveCommentsLineInterpolatesSingleComment() {
        when(localizationService.getInterpolatedMessage(
                        eq("notification.commentLine"),
                        eq(Map.of("comment", "Unvollständig")),
                        eq(SupportedLocale.DE)))
                .thenReturn(" Kommentar: Unvollständig");

        assertThat(displaySupport.resolveCommentsLine(List.of("Unvollständig"), SupportedLocale.DE))
                .isEqualTo(" Kommentar: Unvollständig");
    }

    @Test
    void resolveCommentsLineInterpolatesMultipleComments() {
        when(localizationService.getInterpolatedMessage(
                        eq("notification.commentsLine"),
                        eq(Map.of("comments", "Anna: Gut\nBen: OK")),
                        eq(SupportedLocale.DE)))
                .thenReturn("\nKommentare:\nAnna: Gut\nBen: OK");

        assertThat(
                        displaySupport.resolveCommentsLine(
                                List.of("Anna: Gut", "Ben: OK"), SupportedLocale.DE))
                .isEqualTo("\nKommentare:\nAnna: Gut\nBen: OK");
    }

    @Test
    void resolveCommentsLineSkipsBlank() {
        assertThat(displaySupport.resolveCommentsLine(null, SupportedLocale.DE)).isEmpty();
        assertThat(displaySupport.resolveCommentsLine(List.of(), SupportedLocale.DE)).isEmpty();
    }

    @Test
    void formatDisplayNameListReturnsSingleNameAsIs() {
        assertThat(displaySupport.formatDisplayNameList(List.of("Anna"), SupportedLocale.DE))
                .isEqualTo("Anna");
    }

    @Test
    void formatDisplayNameListJoinsTwoNames() {
        when(localizationService.getInterpolatedMessage(
                        eq("notification.nameList.two"),
                        eq(Map.of("first", "Anna", "second", "Ben")),
                        eq(SupportedLocale.DE)))
                .thenReturn("Anna und Ben");

        assertThat(displaySupport.formatDisplayNameList(List.of("Anna", "Ben"), SupportedLocale.DE))
                .isEqualTo("Anna und Ben");
    }

    @Test
    void formatDisplayNameListJoinsManyNames() {
        when(localizationService.getInterpolatedMessage(
                        eq("notification.nameList.many"),
                        eq(Map.of("initial", "Anna, Ben", "last", "Chris")),
                        eq(SupportedLocale.DE)))
                .thenReturn("Anna, Ben und Chris");

        assertThat(
                        displaySupport.formatDisplayNameList(
                                List.of("Anna", "Ben", "Chris"), SupportedLocale.DE))
                .isEqualTo("Anna, Ben und Chris");
    }

    @Test
    void resolveApproverSuffixReturnsPeriodWhenNamesMissing() {
        assertThat(displaySupport.resolveApproverSuffix(null, SupportedLocale.DE)).isEqualTo(".");
        assertThat(displaySupport.resolveApproverSuffix("  ", SupportedLocale.DE)).isEqualTo(".");
    }

    @Test
    void resolveApproverSuffixInterpolatesNames() {
        when(localizationService.getInterpolatedMessage(
                        eq("notification.approverSuffix"),
                        eq(Map.of("names", "Anna Approver")),
                        eq(SupportedLocale.DE)))
                .thenReturn(" von Anna Approver.");

        assertThat(displaySupport.resolveApproverSuffix("Anna Approver", SupportedLocale.DE))
                .isEqualTo(" von Anna Approver.");
    }

    @Test
    void resolveWorkflowTypeLabelUsesKnownKey() {
        when(localizationService.getMessageOrNull("workflow.type.NAME_CHANGE", SupportedLocale.DE))
                .thenReturn("Namensänderung");

        assertThat(displaySupport.resolveWorkflowTypeLabel("NAME_CHANGE", SupportedLocale.DE))
                .isEqualTo("Namensänderung");
    }

    @Test
    void resolveWorkflowTypeLabelFallsBackToUnknown() {
        when(localizationService.getMessageOrNull("workflow.type.UNKNOWN_TYPE", SupportedLocale.DE))
                .thenReturn(null);
        when(localizationService.getMessage("workflow.type.UNKNOWN", SupportedLocale.DE))
                .thenReturn("Unbekannter Antrag");

        assertThat(displaySupport.resolveWorkflowTypeLabel("UNKNOWN_TYPE", SupportedLocale.DE))
                .isEqualTo("Unbekannter Antrag");
    }

    @Test
    void extractDisplayNameListFiltersBlankEntries() {
        List<Object> names = new ArrayList<>();
        names.add("Anna");
        names.add("  ");
        names.add(null);
        names.add("Ben");
        Map<String, Object> params = new HashMap<>();
        params.put(NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES, names);

        assertThat(
                        NotificationDisplaySupport.extractDisplayNameList(
                                params, NotificationParamKeys.DECIDED_BY_DISPLAY_NAMES))
                .containsExactly("Anna", "Ben");
    }

    @Test
    void extractDisplayNameListReturnsEmptyForMissingOrInvalidValue() {
        assertThat(NotificationDisplaySupport.extractDisplayNameList(Map.of(), "missing"))
                .isEmpty();
        assertThat(
                        NotificationDisplaySupport.extractDisplayNameList(
                                Map.of("missing", "not-a-list"), "missing"))
                .isEmpty();
    }
}
