/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.handler;

import com.emberone.emergencycontact.domain.EmergencyContactRelationship;
import com.emberone.emergencycontact.dto.CreateEmergencyContactRequest;
import com.emberone.emergencycontact.repository.UserEmergencyContactRepository;
import com.emberone.emergencycontact.service.EmergencyContactService;
import com.emberone.onboarding.config.EmergencyContactsStepConfig;
import com.emberone.onboarding.config.OnboardingStepConfig;
import com.emberone.onboarding.domain.OnboardingStepType;
import com.emberone.onboarding.payload.OnboardingStepPayloads;
import com.emberone.onboarding.spi.OnboardingPromotionContext;
import com.emberone.onboarding.spi.OnboardingStepFulfillmentContext;
import com.emberone.onboarding.spi.OnboardingStepHandler;
import com.emberone.platform.exception.ConflictException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmergencyContactsStepHandler implements OnboardingStepHandler {

    static final String MINIMUM_COUNT = "minimumCount";

    private final UserEmergencyContactRepository emergencyContactRepository;
    private final EmergencyContactService emergencyContactService;

    @Override
    public OnboardingStepType type() {
        return OnboardingStepType.EMERGENCY_CONTACTS;
    }

    @Override
    public OnboardingStepConfig parseConfig(Map<String, Object> raw) {
        return new EmergencyContactsStepConfig(readMinimumCount(raw.get(MINIMUM_COUNT)));
    }

    @Override
    public void validateConfig(long tenantId, OnboardingStepConfig config) {
        EmergencyContactsStepConfig typed = requireType(config);
        if (typed.minimumCount() < 1) {
            throw new ConflictException("Emergency contacts step requires minimumCount >= 1");
        }
    }

    @Override
    public Map<String, Object> toConfigMap(OnboardingStepConfig config) {
        EmergencyContactsStepConfig typed = requireType(config);
        return Map.of(MINIMUM_COUNT, typed.minimumCount());
    }

    @Override
    public boolean isFulfilled(
            OnboardingStepFulfillmentContext context, OnboardingStepConfig config) {
        EmergencyContactsStepConfig typed = requireType(config);
        int count = emergencyContactRepository.countByUser_Id(context.userId());
        return count >= typed.minimumCount();
    }

    @Override
    public void promote(
            OnboardingPromotionContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        EmergencyContactsStepConfig typed = requireType(config);
        OnboardingStepFulfillmentContext fulfillmentContext =
                new OnboardingStepFulfillmentContext(
                        context.tenantId(),
                        context.userId(),
                        context.organizationId(),
                        context.invitationId());
        if (isFulfilled(fulfillmentContext, config)) {
            return;
        }
        for (Map<String, Object> contact :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.CONTACTS)) {
            emergencyContactService.createFromOnboarding(
                    context.tenantId(),
                    context.userId(),
                    CreateEmergencyContactRequest.builder()
                            .relationship(
                                    parseRelationship(
                                            OnboardingStepPayloads.readString(
                                                    contact, OnboardingStepPayloads.RELATIONSHIP)))
                            .firstName(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.FIRST_NAME))
                            .lastName(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.LAST_NAME))
                            .street(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.STREET))
                            .city(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.CITY))
                            .postalCode(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.POSTAL_CODE))
                            .country(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.COUNTRY))
                            .phone(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.PHONE))
                            .email(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.EMAIL))
                            .build());
        }
    }

    @Override
    public void validateOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireType(config);
        for (Map<String, Object> contact :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.CONTACTS)) {
            requireNonBlank(contact, OnboardingStepPayloads.FIRST_NAME);
            requireNonBlank(contact, OnboardingStepPayloads.LAST_NAME);
            requireNonBlank(contact, OnboardingStepPayloads.STREET);
            requireNonBlank(contact, OnboardingStepPayloads.CITY);
            requireNonBlank(contact, OnboardingStepPayloads.POSTAL_CODE);
            requireNonBlank(contact, OnboardingStepPayloads.COUNTRY);
            requireNonBlank(contact, OnboardingStepPayloads.PHONE);
            requireNonBlank(contact, OnboardingStepPayloads.EMAIL);
            parseRelationship(
                    OnboardingStepPayloads.readString(
                            contact, OnboardingStepPayloads.RELATIONSHIP));
        }
    }

    @Override
    public Map<String, Object> normalizeOnboardingPayload(
            OnboardingStepConfig config, Map<String, Object> payload) {
        requireType(config);
        List<Map<String, Object>> contacts = new ArrayList<>();
        for (Map<String, Object> contact :
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.CONTACTS)) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put(
                    OnboardingStepPayloads.RELATIONSHIP,
                    parseRelationship(
                                    OnboardingStepPayloads.readString(
                                            contact, OnboardingStepPayloads.RELATIONSHIP))
                            .name());
            normalized.put(
                    OnboardingStepPayloads.FIRST_NAME,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.FIRST_NAME));
            normalized.put(
                    OnboardingStepPayloads.LAST_NAME,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.LAST_NAME));
            normalized.put(
                    OnboardingStepPayloads.STREET,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.STREET));
            normalized.put(
                    OnboardingStepPayloads.CITY,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.CITY));
            normalized.put(
                    OnboardingStepPayloads.POSTAL_CODE,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.POSTAL_CODE));
            normalized.put(
                    OnboardingStepPayloads.COUNTRY,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.COUNTRY));
            normalized.put(
                    OnboardingStepPayloads.PHONE,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.PHONE));
            normalized.put(
                    OnboardingStepPayloads.EMAIL,
                    OnboardingStepPayloads.readString(contact, OnboardingStepPayloads.EMAIL));
            contacts.add(normalized);
        }
        Map<String, Object> normalized = OnboardingStepPayloads.emptyPayload();
        normalized.put(OnboardingStepPayloads.CONTACTS, contacts);
        return normalized;
    }

    @Override
    public boolean isOnboardingFulfilled(
            OnboardingStepFulfillmentContext context,
            OnboardingStepConfig config,
            Map<String, Object> payload) {
        EmergencyContactsStepConfig typed = requireType(config);
        int count =
                OnboardingStepPayloads.readObjectList(payload, OnboardingStepPayloads.CONTACTS)
                        .size();
        return count >= typed.minimumCount();
    }

    private static EmergencyContactRelationship parseRelationship(String value) {
        if (value == null || value.isBlank()) {
            throw new ConflictException("Emergency contact relationship is required");
        }
        try {
            return EmergencyContactRelationship.valueOf(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ConflictException("Unknown emergency contact relationship: " + value);
        }
    }

    private static void requireNonBlank(Map<String, Object> contact, String field) {
        if (OnboardingStepPayloads.readString(contact, field).isBlank()) {
            throw new ConflictException("Emergency contact " + field + " is required");
        }
    }

    private static EmergencyContactsStepConfig requireType(OnboardingStepConfig config) {
        if (!(config instanceof EmergencyContactsStepConfig typed)) {
            throw new ConflictException("Expected emergency contacts step config");
        }
        return typed;
    }

    private static int readMinimumCount(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text.trim());
        }
        return 0;
    }
}
