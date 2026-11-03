/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

export interface OnboardingStepHandle {
  getPayload: () => Record<string, unknown>;
  hasChanges: () => boolean;
  /** Upload pending attachments before the wizard persists step payload. */
  prepareSave?: () => Promise<boolean>;
}

export interface OnboardingStepProps {
  payload: Record<string, unknown>;
  editable: boolean;
  isSaving: boolean;
}

export function payloadsEqual(a: Record<string, unknown>, b: Record<string, unknown>): boolean {
  return JSON.stringify(a) === JSON.stringify(b);
}
