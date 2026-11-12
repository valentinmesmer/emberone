/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.service;

import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.certification.tenant.TenantCertDefinition;
import com.emberone.certification.tenant.TenantCertDefinitionRepository;
import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import com.emberone.drivinglicense.repository.UserDrivingLicenseRepository;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinition;
import com.emberone.drivinglicense.tenant.TenantDrivingLicenseDefinitionRepository;
import com.emberone.platform.exception.ResourceNotFoundException;
import com.emberone.qualification.dto.*;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmission;
import com.emberone.trainingcourse.domain.TrainingCourseUserSubmissionGrantedType;
import com.emberone.trainingcourse.repository.TrainingCourseUserSubmissionRepository;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseType;
import com.emberone.trainingcourse.tenant.TenantTrainingCourseTypeRepository;
import com.emberone.userorganizationmembership.domain.UserOrganizationMembership;
import com.emberone.userorganizationmembership.repository.UserOrganizationMembershipRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One qualification for one user with the evidence of its prerequisites (certification status,
 * training records, driving license). Replaces the client-side fan-out of the detail views.
 */
@Service
@RequiredArgsConstructor
public class QualificationDetailService {

    /** Which evidence sections the caller may read; denied sections stay empty. */
    public record EvidenceAccess(
            boolean certifications, boolean trainingCourses, boolean drivingLicenses) {}

    private final QualificationStatusService statusService;
    private final PrerequisiteEvaluationService prerequisiteEvaluationService;
    private final UserOrganizationMembershipRepository membershipRepository;
    private final TenantCertDefinitionRepository certDefinitionRepository;
    private final TenantTrainingCourseTypeRepository trainingCourseTypeRepository;
    private final TrainingCourseUserSubmissionRepository submissionRepository;
    private final UserDrivingLicenseRepository drivingLicenseRepository;
    private final TenantDrivingLicenseDefinitionRepository drivingLicenseDefinitionRepository;

    @Transactional(readOnly = true)
    public UserQualificationDetailDto getOrgDetail(
            Long organizationId, Long userId, Long definitionId, EvidenceAccess access) {
        PrerequisiteEvaluationService.Holdings holdings =
                prerequisiteEvaluationService.loadHoldings(userId);
        QualificationStatusItemDto item =
                statusService
                        .findStatusItem(organizationId, userId, definitionId, holdings)
                        .orElseThrow(() -> notFound(definitionId));
        List<String> licenseKeys = configKeys(item.getDrivingLicenseDetails());
        return UserQualificationDetailDto.builder()
                .item(item)
                .certifications(
                        access.certifications() ? certifications(item, holdings) : List.of())
                .trainingRecords(
                        access.trainingCourses() ? trainingRecords(item, userId) : List.of())
                .drivingLicense(access.drivingLicenses() ? drivingLicense(userId) : null)
                .drivingLicenseDefinitions(
                        access.drivingLicenses()
                                ? drivingLicenseDefinitions(licenseKeys)
                                : List.of())
                .build();
    }

    @Transactional(readOnly = true)
    public SelfQualificationDetailDto getSelfDetail(Long userId, Long definitionId) {
        PrerequisiteEvaluationService.Holdings holdings =
                prerequisiteEvaluationService.loadHoldings(userId);
        List<SelfQualificationStatusItemDto> organizations = new ArrayList<>();
        for (UserOrganizationMembership membership :
                membershipRepository.findActiveByUser_Id(userId, Instant.now())) {
            Long organizationId = membership.getOrganization().getId();
            statusService
                    .findStatusItem(organizationId, userId, definitionId, holdings)
                    .ifPresent(item ->
                            organizations.add(
                                    SelfQualificationStatusItemDto.builder()
                                            .organizationId(organizationId)
                                            .item(item)
                                            .build()));
        }
        if (organizations.isEmpty()) {
            throw notFound(definitionId);
        }
        QualificationStatusItemDto item = organizations.get(0).getItem();
        return SelfQualificationDetailDto.builder()
                .organizations(organizations)
                .certifications(certifications(item, holdings))
                .trainingRecords(trainingRecords(item, userId))
                .drivingLicense(drivingLicense(userId))
                .build();
    }

    private static ResourceNotFoundException notFound(Long definitionId) {
        return new ResourceNotFoundException("Qualification not found: " + definitionId);
    }

    private static List<String> configKeys(List<QualificationPrerequisiteDetailDto> details) {
        return details.stream().map(QualificationPrerequisiteDetailDto::getConfigKey).toList();
    }

    private List<CertificationStatusItemDto> certifications(
            QualificationStatusItemDto item, PrerequisiteEvaluationService.Holdings holdings) {
        List<String> keys = configKeys(item.getCertDefinitionDetails());
        if (keys.isEmpty()) {
            return List.of();
        }
        return certDefinitionRepository.findByConfigKeyIn(keys).stream()
                .map(TenantCertDefinition::getId)
                .map(holdings.certStatusByDefinitionId()::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private List<QualificationTrainingRecordRefDto> trainingRecords(
            QualificationStatusItemDto item, Long userId) {
        List<String> keys = configKeys(item.getTrainingCourseDetails());
        if (keys.isEmpty()) {
            return List.of();
        }
        Map<Long, String> configKeyByTypeId =
                trainingCourseTypeRepository.findByConfigKeyIn(keys).stream()
                        .collect(
                                Collectors.toMap(
                                        TenantTrainingCourseType::getId,
                                        TenantTrainingCourseType::getConfigKey));
        Comparator<TrainingCourseUserSubmission> newestFirst =
                Comparator.comparing(TrainingCourseUserSubmission::getEndDate)
                        .thenComparing(TrainingCourseUserSubmission::getId)
                        .reversed();
        Map<String, Long> recordIdByConfigKey = new HashMap<>();
        for (TrainingCourseUserSubmission submission :
                submissionRepository.findByUserId(userId).stream().sorted(newestFirst).toList()) {
            for (TrainingCourseUserSubmissionGrantedType granted : submission.getGrantedTypes()) {
                String configKey = configKeyByTypeId.get(granted.getTenantTrainingCourseTypeId());
                if (configKey != null) {
                    recordIdByConfigKey.putIfAbsent(configKey, submission.getId());
                }
            }
        }
        return recordIdByConfigKey.entrySet().stream()
                .map(entry ->
                        QualificationTrainingRecordRefDto.builder()
                                .configKey(entry.getKey())
                                .recordId(entry.getValue())
                                .build())
                .toList();
    }

    private QualificationDrivingLicenseDto drivingLicense(Long userId) {
        return drivingLicenseRepository
                .findByUserId(userId)
                .map(license ->
                        QualificationDrivingLicenseDto.builder()
                                .documentExpirationDate(license.getDocumentExpirationDate())
                                .classes(
                                        license.getClasses().stream()
                                                .map(licenseClass ->
                                                        QualificationDrivingLicenseClassDto
                                                                .builder()
                                                                .tenantDrivingLicenseDefinitionId(
                                                                        licenseClass
                                                                                .getTenantDrivingLicenseDefinitionId())
                                                                .grantDate(
                                                                        licenseClass.getGrantDate())
                                                                .expirationDate(
                                                                        licenseClass
                                                                                .getExpirationDate())
                                                                .build())
                                                .toList())
                                .build())
                .orElse(null);
    }

    private List<DrivingLicenseDefinitionDto> drivingLicenseDefinitions(List<String> keys) {
        if (keys.isEmpty()) {
            return List.of();
        }
        return drivingLicenseDefinitionRepository.findByConfigKeyIn(keys).stream()
                .map(QualificationDetailService::toDefinitionDto)
                .toList();
    }

    private static DrivingLicenseDefinitionDto toDefinitionDto(
            TenantDrivingLicenseDefinition definition) {
        return DrivingLicenseDefinitionDto.builder()
                .id(definition.getId())
                .name(definition.getName())
                .active(definition.isActive())
                .configKey(definition.getConfigKey())
                .build();
    }
}
