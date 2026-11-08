/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Badge } from '@/components/ui/badge';
import { List, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import type { QualificationStatusItemDto } from '@/lib/api/qualifications';
import { isOrgDecisionIntervalActive } from '@/lib/qualifications/orgDecisionIntervals';
import { formatDateShort } from '@/lib/utils/date';

interface QualificationOrgDecisionPanelProps {
  item: QualificationStatusItemDto;
}

function resolveOrgDecisionPresentation(item: QualificationStatusItemDto): {
  labelKey: string;
  tone: 'success' | 'neutral' | 'warning' | 'danger';
} {
  if (item.orgDecisionGranted) {
    return {
      labelKey: 'qualifications.orgDecision.status.granted',
      tone: 'success',
    };
  }
  if (item.status === 'ORG_DECISION_REVOKED') {
    return {
      labelKey: 'qualifications.orgDecision.status.revoked',
      tone: 'warning',
    };
  }
  return {
    labelKey: 'qualifications.orgDecision.status.pending',
    tone: 'neutral',
  };
}

function formatIntervalLabel(
  startDate: string,
  endDate: string | null,
  openEndedLabel: string,
): string {
  if (!endDate) {
    return `${formatDateShort(startDate)} - ${openEndedLabel}`;
  }
  return `${formatDateShort(startDate)} - ${formatDateShort(endDate)}`;
}

export function QualificationOrgDecisionPanel({ item }: QualificationOrgDecisionPanelProps) {
  const t = useTranslation();

  if (!item.requiresOrgDecision) {
    return null;
  }

  const presentation = resolveOrgDecisionPresentation(item);
  const intervals = item.orgDecisions ?? [];

  return (
    <div className="mt-3 border-t border-[var(--border-color)]/30 pt-3">
      <div className="flex flex-col gap-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between sm:gap-4">
          <div className="min-w-0 space-y-1">
            <p className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
              {t('qualifications.detail.orgDecisionLabel')}
            </p>
            <Badge tone={presentation.tone} pill className="py-0.5">
              {t(presentation.labelKey)}
            </Badge>
          </div>
        </div>

        {intervals.length > 0 ? (
          <div className="space-y-1.5">
            <p className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
              {t('qualifications.orgDecision.intervalsTitle')}
            </p>
            <List variant="inset" label={t('qualifications.orgDecision.intervalsTitle')}>
              {intervals.map((interval) => (
                <ListRow
                  key={interval.id}
                  compact
                  accent={false}
                  title={formatIntervalLabel(
                    interval.startDate,
                    interval.endDate,
                    t('qualifications.orgDecision.endDateOpen'),
                  )}
                  badges={
                    <Badge
                      tone={isOrgDecisionIntervalActive(interval) ? 'success' : 'neutral'}
                      pill
                      className="py-0"
                    >
                      {isOrgDecisionIntervalActive(interval)
                        ? t('qualifications.orgDecision.intervalStatus.GRANTED')
                        : t(`qualifications.orgDecision.intervalStatus.${interval.status}`)}
                    </Badge>
                  }
                />
              ))}
            </List>
          </div>
        ) : null}
      </div>
    </div>
  );
}
