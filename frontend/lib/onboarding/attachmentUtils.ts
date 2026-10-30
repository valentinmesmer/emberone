/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OnboardingStepType } from '@/lib/api/standards';
import {
  readCertifications,
  readDrivingLicense,
  readTrainingCourses,
} from '@/lib/onboarding/stepPayloads';
import type { SelfOnboardingInvitationDetailDto } from '@/types/onboardingInvitation';

export function readOnboardingAttachmentFileNames(payload: {
  fileNames?: string[] | null;
  fileName?: string | null;
  uploaded?: boolean;
  attachments?: { fileName?: string | null }[] | null;
}): string[] {
  if (payload.fileNames?.length) {
    return payload.fileNames.filter((name) => name.trim().length > 0);
  }
  if (payload.uploaded && payload.fileName?.trim()) {
    return [payload.fileName.trim()];
  }
  if (payload.attachments?.length) {
    return payload.attachments
      .map((item) => item.fileName?.trim() ?? '')
      .filter((name) => name.length > 0);
  }
  return [];
}

export function hasOnboardingAttachments(fileNames: string[], pendingFiles: File[]): boolean {
  return fileNames.length > 0 || pendingFiles.length > 0;
}

export function withOnboardingAttachmentFileNames<
  T extends {
    fileName?: string | null;
    fileNames?: string[];
    uploaded?: boolean;
  },
>(item: T, fileNames: string[]): T {
  return {
    ...item,
    fileNames,
    fileName: fileNames[0] ?? null,
    uploaded: fileNames.length > 0,
  };
}

export async function uploadOnboardingPendingFiles(
  files: File[],
  uploadFile: (file: File) => Promise<void>,
): Promise<void> {
  for (const file of files) {
    await uploadFile(file);
  }
}

export function readRecordAttachmentFileNamesFromDetail(
  detail: SelfOnboardingInvitationDetailDto,
  stepType: Extract<OnboardingStepType, 'CERTIFICATIONS' | 'TRAINING_COURSES'>,
  recordKey: string,
): string[] {
  const step = detail.steps.find((entry) => entry.stepType === stepType);
  if (!step) return [];
  const records =
    stepType === 'CERTIFICATIONS'
      ? readCertifications(step.payload)
      : readTrainingCourses(step.payload);
  const record = records.find((entry) => entry.recordKey === recordKey);
  return record ? readOnboardingAttachmentFileNames(record) : [];
}

export function readDrivingLicenseAttachmentFileNamesFromDetail(
  detail: SelfOnboardingInvitationDetailDto,
): string[] {
  const step = detail.steps.find((entry) => entry.stepType === 'DRIVING_LICENSE');
  const license = step ? readDrivingLicense(step.payload) : null;
  return license ? readOnboardingAttachmentFileNames(license) : [];
}
