/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { AlertCircle, CheckCircle2, Circle } from 'lucide-react';
import { Card } from '@/components/ui/card';
import { statusCardToneClasses, statusIconToneClasses } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import type { LeaderIssueSummary } from '@/lib/admin/orgLeaderIssues';
import { totalLeaderIssues } from '@/lib/admin/orgLeaderIssues';
import { cn } from '@/lib/utils/cn';

export interface TenantSetupStatusCardProps {
  standardsConfigured: boolean;
  hasOrganizations: boolean;
  leaderSummary: LeaderIssueSummary | null;
  hasLeaderIssueHint: boolean;
}

export function TenantSetupStatusCard({
  standardsConfigured,
  hasOrganizations,
  leaderSummary,
  hasLeaderIssueHint,
}: TenantSetupStatusCardProps) {
  const t = useTranslation();

  const leaderIssues = leaderSummary
    ? totalLeaderIssues(leaderSummary)
    : hasLeaderIssueHint
      ? 1
      : 0;
  const leadersOk = hasOrganizations && leaderIssues === 0;
  const setupComplete = standardsConfigured && hasOrganizations && leadersOk;
  return (
    <Card className={cn('p-3 sm:p-4', !setupComplete && statusCardToneClasses.warning)}>
      <div className="flex items-start gap-2.5">
        {setupComplete ? (
          <CheckCircle2
            className={cn('mt-0.5 h-5 w-5 shrink-0', statusIconToneClasses.success)}
            aria-hidden
          />
        ) : (
          <AlertCircle
            className={cn('mt-0.5 h-5 w-5 shrink-0', statusIconToneClasses.warning)}
            aria-hidden
          />
        )}
        <div className="min-w-0 flex-1">
          <h2 className="text-sm font-semibold text-[var(--text-primary)]">
            {setupComplete ? t('admin.tenantSetup.allComplete') : t('admin.tenantSetup.title')}
          </h2>

          {!setupComplete ? (
            <ol className="mt-3 space-y-2">
              <SetupStep
                done={standardsConfigured}
                doneLabel={t('admin.tenantSetup.standardsDone')}
                pendingLabel={t('admin.tenantSetup.standardsStep')}
              />

              <SetupStep
                done={hasOrganizations}
                doneLabel={t('admin.tenantSetup.orgsDone')}
                pendingLabel={t('admin.tenantSetup.orgsStep')}
              />

              {hasOrganizations ? (
                <SetupStep
                  done={leadersOk}
                  doneLabel={t('admin.tenantSetup.leadersDone')}
                  pendingLabel={
                    leaderSummary && leaderSummary.directCount > 0
                      ? t('admin.tenantSetup.leadersMissingCount', {
                          count: leaderSummary.directCount,
                        })
                      : leaderIssues > 0
                        ? t('admin.tenantSetup.leadersMissingSubtree')
                        : t('admin.tenantSetup.leadersStep')
                  }
                />
              ) : null}
            </ol>
          ) : null}
        </div>
      </div>
    </Card>
  );
}

function SetupStep({
  done,
  doneLabel,
  pendingLabel,
}: {
  done: boolean;
  doneLabel: string;
  pendingLabel: string;
}) {
  return (
    <li className="flex items-start gap-2 text-sm">
      {done ? (
        <CheckCircle2
          className={cn('mt-0.5 h-4 w-4 shrink-0', statusIconToneClasses.success)}
          aria-hidden
        />
      ) : (
        <Circle className="mt-0.5 h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
      )}
      <span className={done ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]'}>
        {done ? doneLabel : pendingLabel}
      </span>
    </li>
  );
}
