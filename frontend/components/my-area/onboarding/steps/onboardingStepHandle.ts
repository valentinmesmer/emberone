/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
