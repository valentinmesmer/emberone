/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Labels for tags stored by onboarding handlers before they reused document-promotion metadata. */
const LEGACY_DOCUMENT_TAG_KEYS: Record<string, string> = {
  'training-course': 'appLink.types.TRAINING_COURSE_RECORD',
  'driving-license': 'appLink.types.DRIVING_LICENSE_RECORD',
  certification: 'myArea.menu.certifications',
  onboarding: 'myArea.menu.onboarding',
};

export function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export function formatUserDocumentTag(tag: string, t: (key: string) => string): string {
  const key = LEGACY_DOCUMENT_TAG_KEYS[tag];
  return key ? t(key) : tag;
}
