/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

package com.emberone.qualification.dto;

import com.emberone.certification.dto.CertificationStatusItemDto;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Self view of one qualification: status per membership organization plus prerequisite evidence.
 */
@Value
@Builder
public class SelfQualificationDetailDto {
    List<SelfQualificationStatusItemDto> organizations;
    List<CertificationStatusItemDto> certifications;
    List<QualificationTrainingRecordRefDto> trainingRecords;
    QualificationDrivingLicenseDto drivingLicense;
}
