/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCourseAudienceTargetName } from '@/components/organization/course-offerings/CourseAudienceGroupModal';
import { CourseOfferingCancelledBanner } from '@/components/organization/course-offerings/CourseOfferingCancelledBanner';
import { Card } from '@/components/ui/card';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { useOfferingRegistrationSummary, useOrganizationCourseOffering } from '@/lib/store';
import {
  courseAudienceKindLabelKey,
  getOrganizationCourseCompletionPath,
  getOrganizationCourseRegistrationsPath,
} from '@/lib/utils/courseOffering';

interface Props {
  organizationId: number;
  offeringId: number;
}

/** Seat occupancy plus links to registrations and results. */
export function CourseOfferingParticipantsSection({ organizationId, offeringId }: Props) {
  const t = useTranslation();
  const targetName = useCourseAudienceTargetName(organizationId);
  const { data: offering, isPending: offeringPending } = useOrganizationCourseOffering(
    organizationId,
    offeringId,
  );
  const { data: summary, isPending: summaryPending } = useOfferingRegistrationSummary(
    organizationId,
    offeringId,
  );

  if (offeringPending || summaryPending) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!offering || !summary) {
    return null;
  }

  return (
    <div className="space-y-3">
      <CourseOfferingCancelledBanner offering={offering} />
      <Card className="p-3 sm:p-4">
        <SectionHeader title={t('courseOfferings.registrations.summaryTitle')} />
        <div className="mt-3 grid grid-cols-2 gap-2 sm:grid-cols-4">
          <SummaryStat
            label={t('courseOfferings.registrations.summaryParticipants')}
            value={summary.participants}
          />
          <SummaryStat
            label={t('courseOfferings.registrations.summaryFreeSeats')}
            value={summary.freeSeats}
          />
          <SummaryStat
            label={t('courseOfferings.registrations.summaryPending')}
            value={summary.pending}
            highlight={summary.pending > 0}
          />
          <SummaryStat
            label={t('courseOfferings.registrations.summaryWaitlist')}
            value={summary.waitlist}
          />
        </div>
        {summary.quotas.length > 0 ? (
          <div className="mt-3 space-y-2 border-t border-[var(--border-color)] pt-3">
            <p className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
              {t('courseOfferings.registrations.quotasTitle')}
            </p>
            <ul className="space-y-2">
              {summary.quotas.map((quota) => (
                <QuotaRow
                  key={quota.audienceId}
                  name={targetName(quota) ?? t(courseAudienceKindLabelKey(quota.kind))}
                  participants={quota.participants}
                  quota={quota.quota}
                  waitlist={quota.waitlist}
                />
              ))}
            </ul>
          </div>
        ) : null}
        <ListContainer
          className="mt-3"
          variant="inset"
          label={t('courseOfferings.registrations.summaryTitle')}
        >
          <ListRow
            href={getOrganizationCourseRegistrationsPath(organizationId, offeringId)}
            title={t('courseOfferings.detail.participants')}
          />
          <ListRow
            href={getOrganizationCourseCompletionPath(organizationId, offeringId)}
            title={t('courseOfferings.detail.completion')}
          />
        </ListContainer>
      </Card>
    </div>
  );
}

function SummaryStat({
  label,
  value,
  highlight = false,
}: {
  label: string;
  value: number;
  highlight?: boolean;
}) {
  return (
    <div
      className={`rounded-lg border px-3 py-2 ${
        highlight
          ? 'border-[var(--accent-primary)] bg-[var(--accent-primary)]/10'
          : 'border-[var(--border-color)] bg-[var(--bg-secondary)]'
      }`}
    >
      <div className="text-xl font-semibold leading-tight tabular-nums">{value}</div>
      <div className="text-xs text-[var(--text-secondary)]">{label}</div>
    </div>
  );
}

function QuotaRow({
  name,
  participants,
  quota,
  waitlist,
}: {
  name: string;
  participants: number;
  quota: number;
  waitlist: number;
}) {
  const t = useTranslation();
  const fill = quota > 0 ? Math.min(1, participants / quota) : 0;
  const full = quota > 0 && participants >= quota;

  return (
    <li className="rounded-lg border border-[var(--border-color)] bg-[var(--bg-secondary)] px-3 py-2.5">
      <div className="flex items-baseline justify-between gap-3">
        <span className="min-w-0 truncate text-sm font-medium text-[var(--text-primary)]">
          {name}
        </span>
        <span className="shrink-0 text-sm font-semibold tabular-nums text-[var(--text-primary)]">
          {t('courseOfferings.registrations.quotaSeats', { participants, quota })}
        </span>
      </div>
      <div
        className="mt-2 h-1.5 overflow-hidden rounded-full bg-[var(--border-color)]"
        role="progressbar"
        aria-valuenow={participants}
        aria-valuemin={0}
        aria-valuemax={quota}
        aria-label={t('courseOfferings.registrations.quotaSeats', { participants, quota })}
      >
        <div
          className={`h-full rounded-full transition-[width] ${
            full ? 'bg-[var(--fire-red)]' : 'bg-[var(--text-secondary)]'
          }`}
          style={{ width: `${fill * 100}%` }}
        />
      </div>
      {waitlist > 0 ? (
        <div className="mt-2 flex flex-wrap gap-1.5 text-xs text-[var(--text-secondary)]">
          <span className="rounded-md bg-[var(--card-bg)] px-1.5 py-0.5 tabular-nums">
            {t('courseOfferings.registrations.quotaWaitlist', { count: waitlist })}
          </span>
        </div>
      ) : null}
    </li>
  );
}
