/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { forwardRef } from 'react';
import type { OnboardingStepType } from '@/lib/api/standards';
import type {
  OnboardingInvitationAccess,
  OnboardingInvitationCatalogDto,
} from '@/types/onboardingInvitation';
import { OnboardingCertificationsStep } from './OnboardingCertificationsStep';
import { OnboardingDrivingLicenseStep } from './OnboardingDrivingLicenseStep';
import { OnboardingEmergencyContactsStep } from './OnboardingEmergencyContactsStep';
import { OnboardingEntryDocumentsStep } from './OnboardingEntryDocumentsStep';
import { OnboardingRankStep } from './OnboardingRankStep';
import { OnboardingTrainingCoursesStep } from './OnboardingTrainingCoursesStep';
import type { OnboardingStepHandle } from './onboardingStepHandle';

export const OnboardingStepBody = forwardRef<
  OnboardingStepHandle,
  {
    access: OnboardingInvitationAccess;
    stepType: OnboardingStepType;
    payload: Record<string, unknown>;
    config: Record<string, unknown>;
    catalog: OnboardingInvitationCatalogDto;
    editable: boolean;
    isSaving: boolean;
    onUpload?: (configKey: string, file: File) => Promise<void>;
  }
>(function OnboardingStepBody(
  { access, stepType, payload, config, catalog, editable, isSaving, onUpload },
  ref,
) {
  switch (stepType) {
    case 'EMERGENCY_CONTACTS':
      return (
        <OnboardingEmergencyContactsStep
          ref={ref}
          payload={payload}
          config={config}
          editable={editable}
          isSaving={isSaving}
        />
      );
    case 'ENTRY_DOCUMENTS':
      return (
        <OnboardingEntryDocumentsStep
          ref={ref}
          access={access}
          payload={payload}
          documents={catalog.documents ?? []}
          documentScope="tenant"
          editable={editable}
          isSaving={isSaving}
          onUpload={onUpload}
        />
      );
    case 'ORGANIZATION_ENTRY_DOCUMENTS':
      return (
        <OnboardingEntryDocumentsStep
          ref={ref}
          access={access}
          payload={payload}
          documents={catalog.organizationDocuments ?? []}
          documentScope="organization"
          editable={editable}
          isSaving={isSaving}
          onUpload={onUpload}
        />
      );
    case 'TRAINING_COURSES':
      return (
        <OnboardingTrainingCoursesStep
          ref={ref}
          access={access}
          payload={payload}
          types={catalog.trainingCourseTypes}
          editable={editable}
          isSaving={isSaving}
        />
      );
    case 'CERTIFICATIONS':
      return (
        <OnboardingCertificationsStep
          ref={ref}
          access={access}
          payload={payload}
          definitions={catalog.certifications}
          editable={editable}
          isSaving={isSaving}
        />
      );
    case 'DRIVING_LICENSE':
      return (
        <OnboardingDrivingLicenseStep
          ref={ref}
          access={access}
          payload={payload}
          definitions={catalog.drivingLicenseClasses}
          editable={editable}
          isSaving={isSaving}
        />
      );
    case 'RANK':
      return (
        <OnboardingRankStep
          ref={ref}
          access={access}
          payload={payload}
          ranks={catalog.ranks ?? []}
          editable={editable}
          isSaving={isSaving}
        />
      );
    default:
      return null;
  }
});
