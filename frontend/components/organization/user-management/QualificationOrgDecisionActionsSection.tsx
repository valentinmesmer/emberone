/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Check, X } from 'lucide-react';
import { useState } from 'react';
import { QualificationOrgDecisionGrantModal } from '@/components/organization/user-management/QualificationOrgDecisionGrantModal';
import { QualificationOrgDecisionRevokeModal } from '@/components/organization/user-management/QualificationOrgDecisionRevokeModal';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import type { QualificationStatusItemDto } from '@/lib/api/qualifications';
import { findActiveGrantInterval } from '@/lib/qualifications/orgDecisionIntervals';

interface QualificationOrgDecisionActionsSectionProps {
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

/** Page-bottom org decision actions (grant / revoke) for a qualification detail page. */
export function QualificationOrgDecisionActionsSection({
  item,
  canWrite,
  isGrantPending = false,
  isEndDatePending = false,
  onGrant,
  onSetEndDate,
}: QualificationOrgDecisionActionsSectionProps) {
  const t = useTranslation();
  const [grantModalOpen, setGrantModalOpen] = useState(false);
  const [revokeModalOpen, setRevokeModalOpen] = useState(false);

  if (!item.requiresOrgDecision) {
    return null;
  }

  const activeGrant = findActiveGrantInterval(item.orgDecisions ?? []);
  const showGrant = canWrite && item.prerequisitesMet && !activeGrant;
  const showRevoke = canWrite && !!activeGrant;
  const pending = isGrantPending || isEndDatePending;

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
      {showGrant || showRevoke ? (
        <ActionsSection>
          {showGrant ? (
            <ListRow
              leading={<ActionRowIcon icon={Check} />}
              title={t('qualifications.orgDecision.grant')}
              subtitle={t('qualifications.orgDecision.grantHint')}
              onClick={pending ? undefined : () => setGrantModalOpen(true)}
            />
          ) : null}
          {showRevoke ? (
            <ListRow
              leading={<ActionRowIcon icon={X} />}
              title={t('qualifications.orgDecision.revoke')}
              subtitle={t('qualifications.orgDecision.revokeHint')}
              onClick={pending ? undefined : () => setRevokeModalOpen(true)}
            />
          ) : null}
        </ActionsSection>
      ) : null}

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
