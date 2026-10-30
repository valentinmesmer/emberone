/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { QualificationOrgDecisionActions } from '@/components/organization/user-management/QualificationOrgDecisionActions';
import { QualificationOrgDecisionGrantModal } from '@/components/organization/user-management/QualificationOrgDecisionGrantModal';
import { QualificationOrgDecisionRevokeModal } from '@/components/organization/user-management/QualificationOrgDecisionRevokeModal';
import { Badge } from '@/components/ui/badge';
import { List, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import type { QualificationStatusItemDto } from '@/lib/api/qualifications';
import {
  findActiveGrantInterval,
  isOrgDecisionIntervalActive,
} from '@/lib/qualifications/orgDecisionIntervals';
import { formatDateShort } from '@/lib/utils/date';

interface QualificationOrgDecisionPanelProps {
  item: QualificationStatusItemDto;
  canWrite: boolean;
  isGrantPending?: boolean;
  isEndDatePending?: boolean;
  onGrant: (params: { startDate: string; endDate?: string }) => void | Promise<void>;
  onSetEndDate: (params: {
    decisionId: number;
    startDate: string;
    endDate: string;
  }) => void | Promise<void>;
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

export function QualificationOrgDecisionPanel({
  item,
  canWrite,
  isGrantPending = false,
  isEndDatePending = false,
  onGrant,
  onSetEndDate,
}: QualificationOrgDecisionPanelProps) {
  const t = useTranslation();
  const [grantModalOpen, setGrantModalOpen] = useState(false);
  const [revokeModalOpen, setRevokeModalOpen] = useState(false);

  if (!item.requiresOrgDecision) {
    return null;
  }

  const presentation = resolveOrgDecisionPresentation(item);
  const intervals = item.orgDecisions ?? [];
  const activeGrant = findActiveGrantInterval(intervals);
  const showGrant = canWrite && item.prerequisitesMet && !activeGrant;
  const showRevoke = canWrite && !!activeGrant;

  const handleGrantConfirm = async (params: { startDate: string; endDate?: string }) => {
    try {
      await onGrant(params);
      setGrantModalOpen(false);
    } catch {
      // Caller shows errors; keep modal open.
    }
  };

  const handleEndDateConfirm = async (params: { endDate: string }) => {
    if (!activeGrant) return;
    try {
      await onSetEndDate({
        decisionId: activeGrant.id,
        startDate: activeGrant.startDate,
        endDate: params.endDate,
      });
      setRevokeModalOpen(false);
    } catch {
      // Caller shows errors; keep modal open.
    }
  };

  return (
    <>
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
            {showGrant || showRevoke ? (
              <QualificationOrgDecisionActions
                variant="button"
                canGrant={showGrant}
                canRevoke={showRevoke}
                isGrantPending={isGrantPending}
                isRevokePending={isEndDatePending}
                onGrant={() => setGrantModalOpen(true)}
                onRevoke={() => setRevokeModalOpen(true)}
              />
            ) : null}
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

      <QualificationOrgDecisionGrantModal
        isOpen={grantModalOpen}
        onClose={() => setGrantModalOpen(false)}
        qualificationName={item.name}
        isLoading={isGrantPending}
        onConfirm={handleGrantConfirm}
      />

      {activeGrant ? (
        <QualificationOrgDecisionRevokeModal
          isOpen={revokeModalOpen}
          onClose={() => setRevokeModalOpen(false)}
          qualificationName={item.name}
          intervalStartDate={activeGrant.startDate}
          currentEndDate={activeGrant.endDate}
          isLoading={isEndDatePending}
          onConfirm={handleEndDateConfirm}
        />
      ) : null}
    </>
  );
}
