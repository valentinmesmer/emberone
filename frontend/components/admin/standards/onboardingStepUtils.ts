/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OnboardingStepDto, OnboardingStepType } from '@/lib/api/standards';

export type LocalOnboardingStep = OnboardingStepDto;

export function normalizeOnboardingSteps(steps: LocalOnboardingStep[]): LocalOnboardingStep[] {
  return steps.map((step, index) => ({ ...step, sortOrder: index }));
}

export function readOnboardingDocumentKeys(config: Record<string, unknown>): string[] {
  const value = config.documentConfigKeys;
  if (!Array.isArray(value)) return [];
  return value.filter((item): item is string => typeof item === 'string');
}

export function readOnboardingMinimumCount(config: Record<string, unknown>): number {
  const value = config.minimumCount;
  if (typeof value === 'number') return value;
  if (typeof value === 'string' && value.trim()) return Number.parseInt(value, 10);
  return 1;
}

export function onboardingStepTypeKey(type: OnboardingStepType): string {
  switch (type) {
    case 'ENTRY_DOCUMENTS':
      return 'entryDocuments';
    case 'ORGANIZATION_ENTRY_DOCUMENTS':
      return 'organizationEntryDocuments';
    case 'EMERGENCY_CONTACTS':
      return 'emergencyContacts';
    case 'TRAINING_COURSES':
      return 'trainingCourses';
    case 'CERTIFICATIONS':
      return 'certifications';
    case 'DRIVING_LICENSE':
      return 'drivingLicense';
    case 'RANK':
      return 'rank';
  }
}

export function onboardingStepHasConfig(type: OnboardingStepType): boolean {
  return type === 'ENTRY_DOCUMENTS' || type === 'EMERGENCY_CONTACTS';
}

export function defaultOnboardingStepConfig(type: OnboardingStepType): Record<string, unknown> {
  if (type === 'ENTRY_DOCUMENTS') return { documentConfigKeys: [] };
  if (type === 'EMERGENCY_CONTACTS') return { minimumCount: 1 };
  return {};
}

export function parseOnboardingStepType(value: string): OnboardingStepType | null {
  switch (value) {
    case 'ENTRY_DOCUMENTS':
    case 'EMERGENCY_CONTACTS':
    case 'ORGANIZATION_ENTRY_DOCUMENTS':
    case 'TRAINING_COURSES':
    case 'CERTIFICATIONS':
    case 'DRIVING_LICENSE':
    case 'RANK':
      return value;
    default:
      return null;
  }
}
