/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.service;

import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboardingdocument.service.UserEntryDocumentFulfillmentService;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingEntryDocumentReuseService {

    private final UserEntryDocumentFulfillmentService fulfillmentService;

    @Transactional(readOnly = true)
    public Set<String> findReusableTenantConfigKeys(
            OnboardingStepFulfillmentContext context, Collection<String> configKeys) {
        return fulfillmentService.findFulfilledTenantConfigKeys(context.userId(), configKeys);
    }

    @Transactional(readOnly = true)
    public Set<String> findReusableOrganizationConfigKeys(
            OnboardingStepFulfillmentContext context, Collection<String> configKeys) {
        return fulfillmentService.findFulfilledOrganizationConfigKeys(
                context.organizationId(), context.userId(), configKeys);
    }

    @Transactional(readOnly = true)
    public Set<String> findDeclinedTenantConfigKeys(
            OnboardingStepFulfillmentContext context, Collection<String> configKeys) {
        return fulfillmentService.findDeclinedTenantConfigKeys(context.userId(), configKeys);
    }

    @Transactional(readOnly = true)
    public Set<String> findDeclinedOrganizationConfigKeys(
            OnboardingStepFulfillmentContext context, Collection<String> configKeys) {
        return fulfillmentService.findDeclinedOrganizationConfigKeys(
                context.organizationId(), context.userId(), configKeys);
    }

    @Transactional(readOnly = true)
    public boolean areAllTenantReusable(
            OnboardingStepFulfillmentContext context, Collection<String> configKeys) {
        return fulfillmentService.areAllTenantFulfilled(context.userId(), configKeys);
    }

    @Transactional(readOnly = true)
    public boolean areAllOrganizationReusable(
            OnboardingStepFulfillmentContext context, Collection<String> configKeys) {
        return fulfillmentService.areAllOrganizationFulfilled(
                context.organizationId(), context.userId(), configKeys);
    }
}
