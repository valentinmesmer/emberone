/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { RetentionCategory, RetentionPeriodUnit } from '@/lib/api/admin';

type Translate = (key: string, params?: Record<string, string | number>) => string;

const RETENTION_CATEGORIES: readonly RetentionCategory[] = [
  'EXIT_IMMEDIATE',
  'EXIT_SETTLEMENT',
  'DRIVING_LICENSE',
  'EVIDENCE',
  'ABSENCE',
  'TIME_ENTRY',
  'EVENT',
  'APPOINTMENT',
  'WORKFLOW_STALE',
  'WORKFLOW',
  'TODO',
  'NOTIFICATION',
  'ONBOARDING',
  'AUDIT',
  'USER_MASTER',
] as const;

export function isRetentionCategory(value: string): value is RetentionCategory {
  return (RETENTION_CATEGORIES as readonly string[]).includes(value);
}

export function formatRetentionPeriod(
  t: Translate,
  amount: number,
  unit: RetentionPeriodUnit,
): string {
  if (amount === 0 && unit === 'DAYS') {
    return t('admin.retention.period.immediate');
  }
  const plural = amount === 1 ? 'one' : 'other';
  return t(`admin.retention.period.${unit}_${plural}`, { count: amount });
}

export function retentionProfileLabel(t: Translate, profileKey: string): string {
  const key = `admin.retention.profiles.${profileKey}`;
  const label = t(key);
  return label === key ? profileKey : label;
}

export function retentionTableLabel(t: Translate, tableName: string): string {
  const key = `admin.retention.tables.${tableName}`;
  const label = t(key);
  return label === key ? tableName : label;
}

export function retentionCategoryTitle(t: Translate, category: RetentionCategory): string {
  return t(`admin.retention.categories.${category}.title`);
}

export function retentionCategoryStart(t: Translate, category: RetentionCategory): string {
  return t(`admin.retention.categories.${category}.start`);
}
