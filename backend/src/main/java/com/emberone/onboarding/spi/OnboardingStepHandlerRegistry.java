/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.spi;

import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.platform.exception.ConflictException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OnboardingStepHandlerRegistry {

    private final Map<OnboardingStepType, OnboardingStepHandler> handlersByType;

    public OnboardingStepHandlerRegistry(List<OnboardingStepHandler> handlers) {
        this.handlersByType =
                handlers.stream()
                        .collect(
                                Collectors.toUnmodifiableMap(
                                        OnboardingStepHandler::type, Function.identity()));
        log.debug(
                "Registered {} onboarding step handler(s): {}",
                handlers.size(),
                handlers.stream().map(OnboardingStepHandler::type).toList());
    }

    public List<OnboardingStepType> availableTypes() {
        return List.copyOf(handlersByType.keySet());
    }

    public OnboardingStepHandler require(OnboardingStepType type) {
        OnboardingStepHandler handler = handlersByType.get(type);
        if (handler == null) {
            throw new ConflictException("Unknown onboarding step type: " + type);
        }
        return handler;
    }
}
