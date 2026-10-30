/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.spi;

import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingStepType;
import java.util.Map;

public interface OnboardingStepHandler {

    OnboardingStepType type();

    OnboardingStepConfig parseConfig(Map<String, Object> raw);

    void validateConfig(long tenantId, OnboardingStepConfig config);

    Map<String, Object> toConfigMap(OnboardingStepConfig config);

    boolean isFulfilled(OnboardingStepFulfillmentContext context, OnboardingStepConfig config);

    default void validateOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {}

    default Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        return payload == null ? Map.of() : payload;
    }

    /**
     * Fulfillment against invitation draft data (not the live user tables).
     *
     * @param payload stored step payload, empty when the step was never saved
     */
    boolean isOnboardingFulfilled(
            OnboardingStepFulfillmentContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload);

    /**
     * Whether this invitation collected answers for the step. Ignores live user tables so promoted
     * drafts still count after approve. Default delegates to {@link #isOnboardingFulfilled} with an
     * empty context; handlers that treat live reuse as onboarding fulfillment must override.
     */
    default boolean hasInvitationWork(OnboardingStepConfig config, Map<String, Object> payload) {
        return isOnboardingFulfilled(
                new OnboardingStepFulfillmentContext(0L, 0L, 0L, 0L), config, payload);
    }

    /**
     * Writes draft payload into live user tables after onboarding approval. No-op when {@link
     * #isFulfilled} is already true or the step was skipped via {@code noneDeclared}.
     */
    default void promote(
            OnboardingPromotionContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {}
}
