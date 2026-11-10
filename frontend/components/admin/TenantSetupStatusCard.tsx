/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { AlertCircle, CheckCircle2, ChevronRight, Circle } from 'lucide-react';
import Link from 'next/link';
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
  standardsHref: string;
  orgTreeAnchorId: string;
  onCreateRootOrg: () => void;
  /** Detail page of the first organization without a leader. */
  firstLeaderIssueHref?: string | null;
}

export function TenantSetupStatusCard({
  standardsConfigured,
  hasOrganizations,
  leaderSummary,
  hasLeaderIssueHint,
  standardsHref,
  orgTreeAnchorId,
  onCreateRootOrg,
  firstLeaderIssueHref,
}: TenantSetupStatusCardProps) {
  const t = useTranslation();

  const leaderIssues = leaderSummary
    ? totalLeaderIssues(leaderSummary)
    : hasLeaderIssueHint
      ? 1
      : 0;
  const leadersOk = hasOrganizations && leaderIssues === 0;
  const setupComplete = standardsConfigured && hasOrganizations && leadersOk;
  const scrollToOrgTree = () =>
    document
      .getElementById(orgTreeAnchorId)
      ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
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
                href={standardsHref}
              />

              <SetupStep
                done={hasOrganizations}
                doneLabel={t('admin.tenantSetup.orgsDone')}
                pendingLabel={t('admin.tenantSetup.orgsStep')}
                href={standardsConfigured ? undefined : standardsHref}
                onSelect={standardsConfigured ? onCreateRootOrg : undefined}
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
                  href={firstLeaderIssueHref ?? undefined}
                  onSelect={firstLeaderIssueHref ? undefined : scrollToOrgTree}
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
  href,
  onSelect,
}: {
  done: boolean;
  doneLabel: string;
  pendingLabel: string;
  href?: string;
  onSelect?: () => void;
}) {
  const actionable = !done && (href || onSelect);
  const label = done ? doneLabel : pendingLabel;
  const content = (
    <>
      {done ? (
        <CheckCircle2
          className={cn('mt-0.5 h-4 w-4 shrink-0', statusIconToneClasses.success)}
          aria-hidden
        />
      ) : (
        <Circle className="mt-0.5 h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
      )}
      <span
        className={cn(
          'flex-1',
          done ? 'text-[var(--text-primary)]' : 'text-[var(--text-secondary)]',
          actionable &&
            'underline-offset-2 group-hover:text-[var(--text-primary)] group-hover:underline',
        )}
      >
        {label}
      </span>
      {actionable ? (
        <ChevronRight
          className="mt-0.5 h-4 w-4 shrink-0 text-[var(--text-secondary)]"
          aria-hidden
        />
      ) : null}
    </>
  );
  const rowClass = 'group flex w-full items-start gap-2 text-left text-sm';
  return (
    <li>
      {actionable && href ? (
        <Link href={href} className={rowClass}>
          {content}
        </Link>
      ) : actionable ? (
        <button type="button" onClick={onSelect} className={rowClass}>
          {content}
        </button>
      ) : (
        <div className={rowClass}>{content}</div>
      )}
    </li>
  );
}
