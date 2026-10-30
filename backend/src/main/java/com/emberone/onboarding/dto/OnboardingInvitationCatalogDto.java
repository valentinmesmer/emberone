/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.emberone.onboarding.dto;

import com.emberone.certification.dto.CertificationDefinitionDto;
import com.emberone.drivinglicense.dto.DrivingLicenseDefinitionDto;
import com.emberone.rank.dto.TenantRankDto;
import com.emberone.trainingcourse.dto.TrainingCourseTypeDto;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OnboardingInvitationCatalogDto {
    List<OnboardingDocumentCatalogItemDto> documents;
    List<OnboardingDocumentCatalogItemDto> organizationDocuments;
    List<TrainingCourseTypeDto> trainingCourseTypes;
    List<CertificationDefinitionDto> certifications;
    List<DrivingLicenseDefinitionDto> drivingLicenseClasses;
    List<TenantRankDto> ranks;
}
