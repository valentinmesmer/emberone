/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.certification.service;

import com.emberone.certification.domain.CertEvidenceType;
import com.emberone.certification.tenant.TenantCertDefinitionEvidenceType;
import com.emberone.certification.tenant.TenantCertDefinitionEvidenceTypeRepository;
import com.emberone.event.domain.EventType;
import com.emberone.platform.exception.ConflictException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantCertDefinitionEvidenceTypeService {

    private final TenantCertDefinitionEvidenceTypeRepository evidenceTypeRepository;

    static Set<CertEvidenceType> parseAndValidate(Set<String> requestedTypes) {
        if (requestedTypes == null || requestedTypes.isEmpty()) {
            return Set.of();
        }
        Set<CertEvidenceType> normalized = new LinkedHashSet<>();
        for (String raw : requestedTypes) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                normalized.add(CertEvidenceType.valueOf(raw.trim()));
            } catch (IllegalArgumentException ex) {
                throw new ConflictException("Invalid evidence type: " + raw);
            }
        }
        return normalized;
    }

    public static Set<String> toStringSet(Set<CertEvidenceType> evidenceTypes) {
        return evidenceTypes.stream()
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Only service/operation event types configured - blocks manual and self-service workflow
     * submission.
     */
    public static boolean isEventOnlyEvidence(Set<CertEvidenceType> evidenceTypes) {
        if (evidenceTypes == null || evidenceTypes.isEmpty()) {
            return false;
        }
        for (CertEvidenceType type : evidenceTypes) {
            if (type != CertEvidenceType.SERVICE_EVENT
                    && type != CertEvidenceType.OPERATION_EVENT) {
                return false;
            }
        }
        return true;
    }

    /**
     * Self-service / manual allowed unless event-only. Empty set = confirmation without attachment.
     */
    public static boolean allowsManualSubmission(Set<CertEvidenceType> evidenceTypes) {
        return !isEventOnlyEvidence(evidenceTypes);
    }

    public static boolean allowsWorkflowSubmission(Set<CertEvidenceType> evidenceTypes) {
        return allowsManualSubmission(evidenceTypes);
    }

    /** Attachment required when {@link CertEvidenceType#DOCUMENT} is among the configured types. */
    public static boolean requiresWorkflowAttachment(Set<CertEvidenceType> evidenceTypes) {
        return evidenceTypes != null && evidenceTypes.contains(CertEvidenceType.DOCUMENT);
    }

    public static boolean allowsEventGrantForEventType(
            Set<CertEvidenceType> evidenceTypes, EventType eventType) {
        if (evidenceTypes == null || evidenceTypes.isEmpty()) {
            return false;
        }
        return switch (eventType) {
            case SERVICE -> evidenceTypes.contains(CertEvidenceType.SERVICE_EVENT);
            case OPERATION -> evidenceTypes.contains(CertEvidenceType.OPERATION_EVENT);
        };
    }

    @Transactional(readOnly = true)
    public Set<CertEvidenceType> getEvidenceTypes(Long tenantCertDefinitionId) {
        return evidenceTypeRepository.findByTenantCertDefinitionId(tenantCertDefinitionId).stream()
                .map(TenantCertDefinitionEvidenceType::getEvidenceType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional
    public Set<CertEvidenceType> replaceEvidenceTypes(
            Long tenantId, Long tenantCertDefinitionId, Set<String> requestedTypes) {
        Set<CertEvidenceType> normalized = parseAndValidate(requestedTypes);
        Set<CertEvidenceType> existing = getEvidenceTypes(tenantCertDefinitionId);
        Set<CertEvidenceType> toRemove = new LinkedHashSet<>(existing);
        toRemove.removeAll(normalized);
        if (!toRemove.isEmpty()) {
            evidenceTypeRepository.deleteByTenantCertDefinitionIdAndEvidenceTypeIn(
                    tenantCertDefinitionId, toRemove);
        }
        Set<CertEvidenceType> toAdd = new LinkedHashSet<>(normalized);
        toAdd.removeAll(existing);
        for (CertEvidenceType evidenceType : toAdd) {
            evidenceTypeRepository.save(
                    TenantCertDefinitionEvidenceType.builder()
                            .tenantId(tenantId)
                            .tenantCertDefinitionId(tenantCertDefinitionId)
                            .evidenceType(evidenceType)
                            .build());
        }
        return normalized;
    }

    @Transactional
    public void seedDefaultDocumentEvidenceType(Long tenantId, Long tenantCertDefinitionId) {
        Set<CertEvidenceType> existing = getEvidenceTypes(tenantCertDefinitionId);
        if (!existing.isEmpty()) {
            return;
        }
        evidenceTypeRepository.save(
                TenantCertDefinitionEvidenceType.builder()
                        .tenantId(tenantId)
                        .tenantCertDefinitionId(tenantCertDefinitionId)
                        .evidenceType(CertEvidenceType.DOCUMENT)
                        .build());
    }
}
