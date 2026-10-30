/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.qualification.service;

import com.emberone.certification.domain.CertificationStatus;
import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.service.CertificationStatusService;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.drivinglicense.domain.UserDrivingLicense;
import com.emberone.drivinglicense.domain.UserDrivingLicenseClass;
import com.emberone.drivinglicense.repository.UserDrivingLicenseRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.platform.time.OrganizationWallClockTime;
import com.emberone.qualification.domain.QualificationRequirementMode;
import com.emberone.qualification.dto.QualificationPrerequisiteDetailDto;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmissionGrantedType;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Evaluates training course, certification and driving license prerequisites (by config key, {@code
 * ALL}/{@code ANY}) against what a user holds. Shared by qualifications and course offerings.
 */
@Service
@RequiredArgsConstructor
public class PrerequisiteEvaluationService {

    private final CertificationStatusService certificationStatusService;
    private final TrainingCourseUserSubmissionRepository submissionRepository;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    private final UserDrivingLicenseRepository drivingLicenseRepository;
    private final TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;
    private final TenantCertDefinitionRepository certDefinitionRepository;

    /** What a user currently holds, loaded once and evaluated against many prerequisite sets. */
    public record Holdings(
            Set<Long> trainingCourseTypeIds,
            Set<Long> drivingLicenseDefinitionIds,
            Map<Long, CertificationStatusItemDto> certStatusByDefinitionId) {}

    @Transactional(readOnly = true)
    public Holdings loadHoldings(Long userId) {
        Map<Long, CertificationStatusItemDto> certByDefinitionId =
                certificationStatusService.getStatusForUser(userId).getItems().stream()
                        .filter(item -> item.getTenantCertDefinitionId() != null)
                        .collect(
                                Collectors.toMap(
                                        CertificationStatusItemDto::getTenantCertDefinitionId,
                                        item -> item,
                                        (left, right) -> left,
                                        LinkedHashMap::new));
        return new Holdings(
                loadGrantedTrainingCourseTypeIds(userId),
                loadGrantedDrivingLicenseClassIds(userId),
                certByDefinitionId);
    }

    public boolean trainingCoursesMet(
            List<String> configKeys, QualificationRequirementMode mode, Holdings holdings) {
        if (configKeys.isEmpty()) {
            return true;
        }
        Set<Long> requiredTypeIds =
                trainingCourseTypeRepository.findByConfigKeyIn(configKeys).stream()
                        .map(TenantTrainingCourseType::getId)
                        .collect(Collectors.toSet());
        if (requiredTypeIds.isEmpty()) {
            return false;
        }
        return matchesMode(mode, requiredTypeIds, holdings.trainingCourseTypeIds());
    }

    public boolean drivingLicensesMet(
            List<String> configKeys, QualificationRequirementMode mode, Holdings holdings) {
        if (configKeys.isEmpty()) {
            return true;
        }
        Set<Long> requiredDefinitionIds =
                drivingLicenseDefinitionRepository.findByConfigKeyIn(configKeys).stream()
                        .map(TenantDrivingLicenseDefinition::getId)
                        .collect(Collectors.toSet());
        if (requiredDefinitionIds.isEmpty()) {
            return false;
        }
        return matchesMode(mode, requiredDefinitionIds, holdings.drivingLicenseDefinitionIds());
    }

    public boolean certDefinitionsMet(
            List<String> configKeys, QualificationRequirementMode mode, Holdings holdings) {
        if (configKeys.isEmpty()) {
            return true;
        }
        List<Long> requiredDefinitionIds =
                certDefinitionRepository.findByConfigKeyIn(configKeys).stream()
                        .map(TenantCertDefinition::getId)
                        .toList();
        if (requiredDefinitionIds.isEmpty()) {
            return false;
        }
        Set<Long> validDefinitionIds =
                requiredDefinitionIds.stream()
                        .filter(id -> isCertValid(holdings.certStatusByDefinitionId().get(id)))
                        .collect(Collectors.toSet());
        return matchesMode(mode, new LinkedHashSet<>(requiredDefinitionIds), validDefinitionIds);
    }

    public List<QualificationPrerequisiteDetailDto> trainingCourseDetails(
            List<String> configKeys, Holdings holdings) {
        return configKeys.stream()
                .map(
                        configKey -> {
                            Optional<TenantTrainingCourseType> type =
                                    trainingCourseTypeRepository.findByConfigKey(configKey);
                            boolean met =
                                    type.map(TenantTrainingCourseType::getId)
                                            .map(holdings.trainingCourseTypeIds()::contains)
                                            .orElse(false);
                            return detail(
                                    configKey,
                                    type.map(TenantTrainingCourseType::getConfigKey),
                                    type.map(TenantTrainingCourseType::getName),
                                    met);
                        })
                .toList();
    }

    public List<QualificationPrerequisiteDetailDto> certDefinitionDetails(
            List<String> configKeys, Holdings holdings) {
        return configKeys.stream()
                .map(
                        configKey -> {
                            Optional<TenantCertDefinition> cert =
                                    certDefinitionRepository.findByConfigKey(configKey);
                            boolean met =
                                    cert.map(TenantCertDefinition::getId)
                                            .map(holdings.certStatusByDefinitionId()::get)
                                            .map(PrerequisiteEvaluationService::isCertValid)
                                            .orElse(false);
                            return detail(
                                    configKey,
                                    cert.map(TenantCertDefinition::getConfigKey),
                                    cert.map(TenantCertDefinition::getName),
                                    met);
                        })
                .toList();
    }

    public List<QualificationPrerequisiteDetailDto> drivingLicenseDetails(
            List<String> configKeys, Holdings holdings) {
        return configKeys.stream()
                .map(
                        configKey -> {
                            Optional<TenantDrivingLicenseDefinition> definition =
                                    drivingLicenseDefinitionRepository.findByConfigKey(configKey);
                            boolean met =
                                    definition
                                            .map(TenantDrivingLicenseDefinition::getId)
                                            .map(holdings.drivingLicenseDefinitionIds()::contains)
                                            .orElse(false);
                            return detail(
                                    configKey,
                                    definition.map(TenantDrivingLicenseDefinition::getConfigKey),
                                    definition.map(TenantDrivingLicenseDefinition::getName),
                                    met);
                        })
                .toList();
    }

    private static QualificationPrerequisiteDetailDto detail(
            String configKey, Optional<String> abbreviation, Optional<String> name, boolean met) {
        return QualificationPrerequisiteDetailDto.builder()
                .configKey(configKey)
                .abbreviation(abbreviation.orElse(configKey))
                .name(name.orElse(configKey))
                .met(met)
                .build();
    }

    private static boolean isCertValid(CertificationStatusItemDto item) {
        return item != null && CertificationStatus.VALID.name().equals(item.getStatus());
    }

    private static boolean matchesMode(
            QualificationRequirementMode mode, Set<Long> required, Set<Long> satisfied) {
        if (mode == QualificationRequirementMode.ANY) {
            return required.stream().anyMatch(satisfied::contains);
        }
        return satisfied.containsAll(required);
    }

    private static boolean isDrivingLicenseDocumentValidOn(
            UserDrivingLicense license, LocalDate date) {
        LocalDate documentExpirationDate = license.getDocumentExpirationDate();
        return documentExpirationDate == null || !documentExpirationDate.isBefore(date);
    }

    private static boolean isDrivingLicenseClassValidOn(
            UserDrivingLicenseClass licenseClass, LocalDate date) {
        LocalDate expirationDate = licenseClass.getExpirationDate();
        return expirationDate == null || !expirationDate.isBefore(date);
    }

    private Set<Long> loadGrantedDrivingLicenseClassIds(Long userId) {
        LocalDate today = OrganizationWallClockTime.today();
        return drivingLicenseRepository.findByUserId(userId).stream()
                .filter(license -> isDrivingLicenseDocumentValidOn(license, today))
                .flatMap(license -> license.getClasses().stream())
                .filter(licenseClass -> isDrivingLicenseClassValidOn(licenseClass, today))
                .map(UserDrivingLicenseClass::getTenantDrivingLicenseDefinitionId)
                .collect(Collectors.toSet());
    }

    private Set<Long> loadGrantedTrainingCourseTypeIds(Long userId) {
        return submissionRepository.findByUserId(userId).stream()
                .flatMap(submission -> submission.getGrantedTypes().stream())
                .map(TrainingCourseUserSubmissionGrantedType::getTenantTrainingCourseTypeId)
                .collect(Collectors.toSet());
    }
}
