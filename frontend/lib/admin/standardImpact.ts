/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OrgGroupRemovalImpactDto, StandardSwitchImpactDto } from '@/lib/api/standards';
import type { TranslationFunction } from '@/lib/utils/membership';

export function formatOrgGroupRemovalImpactMessage(
  impact: OrgGroupRemovalImpactDto,
  t: TranslationFunction,
): string {
  if (impact.level === 'NONE') {
    return t('admin.standards.impact.remove.none');
  }
  if (impact.level === 'HISTORICAL') {
    return t('admin.standards.impact.remove.historical', {
      orgCount: impact.totalOrgsAffected,
    });
  }
  return t('admin.standards.impact.remove.active', {
    membershipCount: impact.totalActiveMemberships,
    orgCount: impact.totalOrgsAffected,
  });
}

export function formatStandardSwitchImpactMessage(
  impact: StandardSwitchImpactDto,
  t: TranslationFunction,
): string {
  if (!impact.removedGroups.length) {
    return t('admin.standards.impact.switch.none');
  }
  const lines = impact.removedGroups.map((group) => {
    const detail = formatOrgGroupRemovalImpactMessage(group, t);
    return `• ${group.configKey}: ${detail}`;
  });
  return [t('admin.standards.impact.switch.intro'), ...lines].join('\n');
}

export function switchImpactRequiresConfirmation(impact: StandardSwitchImpactDto): boolean {
  return impact.removedGroups.some((group) => group.level !== 'NONE');
}

export function removalImpactRequiresConfirmation(impact: OrgGroupRemovalImpactDto): boolean {
  return impact.level !== 'NONE';
}
