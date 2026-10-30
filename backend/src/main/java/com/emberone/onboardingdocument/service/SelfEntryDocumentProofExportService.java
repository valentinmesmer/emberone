/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboardingdocument.service;

import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillment;
import com.emberone.onboardingdocument.domain.UserEntryDocumentFulfillmentEvent;
import com.emberone.onboardingdocument.organization.OrganizationOnboardingDocumentDefinitionRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentEventRepository;
import com.emberone.onboardingdocument.repository.UserEntryDocumentFulfillmentRepository;
import com.emberone.onboardingdocument.tenant.TenantOnboardingDocumentDefinitionRepository;
import com.emberone.organization.repository.OrganizationRepository;
import com.emberone.user.domain.User;
import com.emberone.user.repository.UserRepository;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SelfEntryDocumentProofExportService {

    private final UserEntryDocumentFulfillmentRepository fulfillmentRepository;
    private final UserEntryDocumentFulfillmentEventRepository fulfillmentEventRepository;
    private final TenantOnboardingDocumentDefinitionRepository tenantDefinitionRepository;
    private final OrganizationOnboardingDocumentDefinitionRepository orgDefinitionRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EntryDocumentProofPdfRenderer pdfRenderer;

    @Transactional(readOnly = true)
    public byte[] exportProofPdf(long userId) {
        List<UserEntryDocumentFulfillment> currentStates =
                fulfillmentRepository.findByUserId(userId).stream()
                        .sorted(compareFulfillments())
                        .toList();
        List<UserEntryDocumentFulfillmentEvent> events =
                fulfillmentEventRepository.findByUserIdOrderByConfirmedAtAscIdAsc(userId);

        Map<Long, String> userDisplayNames = resolveUserDisplayNames(events);
        EntryDocumentProofPdfRenderer.DocumentNameResolver documentNames =
                (configKey, organizationId) -> resolveDocumentName(organizationId, configKey);
        EntryDocumentProofPdfRenderer.OrganizationNameResolver organizationNames =
                organizationId ->
                        organizationRepository
                                .findById(organizationId)
                                .map(org -> org.getName())
                                .orElse("Organisation #" + organizationId);

        return pdfRenderer.render(
                currentStates, events, userDisplayNames, documentNames, organizationNames);
    }

    private Comparator<UserEntryDocumentFulfillment> compareFulfillments() {
        return Comparator.comparing(
                        UserEntryDocumentFulfillment::getOrganizationId,
                        Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(UserEntryDocumentFulfillment::getConfigKey);
    }

    private String resolveDocumentName(Long organizationId, String configKey) {
        if (organizationId == null) {
            return tenantDefinitionRepository
                    .findByConfigKey(configKey)
                    .map(definition -> definition.getName())
                    .orElse(configKey);
        }
        return orgDefinitionRepository
                .findByOrganizationIdAndConfigKey(organizationId, configKey)
                .map(definition -> definition.getName())
                .orElse(configKey);
    }

    private Map<Long, String> resolveUserDisplayNames(
            List<UserEntryDocumentFulfillmentEvent> events) {
        Set<Long> userIds = new HashSet<>();
        for (UserEntryDocumentFulfillmentEvent event : events) {
            if (event.getActorUserId() != null) {
                userIds.add(event.getActorUserId());
            }
            if (event.getWorkflowReviewerUserId() != null) {
                userIds.add(event.getWorkflowReviewerUserId());
            }
        }
        Map<Long, String> displayNames = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            displayNames.put(user.getId(), formatUserDisplayName(user));
        }
        return displayNames;
    }

    private static String formatUserDisplayName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String combined = (firstName + " " + lastName).trim();
        if (!combined.isEmpty()) {
            return combined;
        }
        return user.getUsername();
    }
}
