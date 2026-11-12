/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.dto;

import com.emberone.certification.dto.CertificationStatusItemDto;
import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Org view of one qualification for one user with the evidence of its prerequisites. Evidence
 * sections are empty when the caller lacks the matching read permission.
 */
@Value
@Builder
public class UserQualificationDetailDto {
    QualificationStatusItemDto item;
    List<CertificationStatusItemDto> certifications;
    List<QualificationTrainingRecordRefDto> trainingRecords;
    QualificationDrivingLicenseDto drivingLicense;
    List<DrivingLicenseDefinitionDto> drivingLicenseDefinitions;
}
