/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { type ReactNode, useMemo, useState } from 'react';
import { EmptyState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  AppointmentParticipantEntryDto,
  AppointmentParticipationStatsDto,
} from '@/types/appointment';

interface AppointmentParticipationStatsProps {
  stats: AppointmentParticipationStatsDto;
  showUncertain: boolean;
}

function formatParticipantName(p: AppointmentParticipantEntryDto): string {
  const first = p.userFirstName?.trim() ?? '';
  const last = p.userLastName?.trim() ?? '';
  if (first && last) return `${first} ${last}`;
  if (last) return last;
  if (first) return first;
  return `#${p.userId}`;
}

function StatContainer({
  title,
  onClick,
  ariaLabel,
  children,
}: {
  title: string;
  onClick?: () => void;
  ariaLabel?: string;
  children: ReactNode;
}) {
  const inner = (
    <>
      <div className="flex flex-wrap items-center justify-between gap-2">
        <p className="text-sm font-semibold text-[var(--text-primary)]">{title}</p>
      </div>
      <div className="space-y-1">{children}</div>
    </>
  );

  const cardClass =
    'space-y-2 rounded-lg border border-[var(--border-color)] p-3 transition-colors';

  if (onClick) {
    return (
      <button
        type="button"
        onClick={onClick}
        aria-label={ariaLabel}
        className={`${cardClass} w-full cursor-pointer text-left hover:bg-[var(--hover-bg)] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]`}
      >
        {inner}
      </button>
    );
  }

  return <section className={cardClass}>{inner}</section>;
}

function ParticipantList({ people }: { people: AppointmentParticipantEntryDto[] }) {
  const t = useTranslation();
  if (people.length === 0) {
    return <EmptyState>{t('appointments.participation.sectionEmpty')}</EmptyState>;
  }
  return (
    <ul className="list-none space-y-1.5 text-sm text-[var(--text-primary)]">
      {people.map((p) => (
        <li key={p.userId} className="pl-0">
          {formatParticipantName(p)}
        </li>
      ))}
    </ul>
  );
}

export function AppointmentParticipationStats({
  stats,
  showUncertain,
}: AppointmentParticipationStatsProps) {
  const t = useTranslation();
  const [detailOpen, setDetailOpen] = useState(false);

  const yesList = stats.yesParticipants ?? [];
  const noList = stats.noParticipants ?? [];
  const uncertainList = stats.uncertainParticipants ?? [];

  const hasAnyResponse = useMemo(
    () => stats.yesCount + stats.noCount + stats.uncertainCount > 0,
    [stats.yesCount, stats.noCount, stats.uncertainCount],
  );

  return (
    <>
      <StatContainer
        title={t('appointments.participation.attendance')}
        ariaLabel={t('appointments.participation.detailModalTitle')}
        onClick={() => setDetailOpen(true)}
      >
        <StatRow label={t('common.yes')} count={stats.yesCount} />
        <StatRow label={t('common.no')} count={stats.noCount} />
        {showUncertain && (
          <StatRow label={t('appointments.response.uncertain')} count={stats.uncertainCount} />
        )}
      </StatContainer>

      <Modal
        isOpen={detailOpen}
        onClose={() => setDetailOpen(false)}
        title={t('appointments.participation.detailModalTitle')}
        size="md"
      >
        <div className="space-y-6 text-sm">
          {!hasAnyResponse ? (
            <EmptyState>{t('appointments.participation.noResponsesYet')}</EmptyState>
          ) : (
            <>
              <section>
                <h3 className="mb-2 font-semibold text-[var(--text-primary)]">
                  {t('common.yes')} ({stats.yesCount})
                </h3>
                <ParticipantList people={yesList} />
              </section>
              <section>
                <h3 className="mb-2 font-semibold text-[var(--text-primary)]">
                  {t('common.no')} ({stats.noCount})
                </h3>
                <ParticipantList people={noList} />
              </section>
              {showUncertain && (
                <section>
                  <h3 className="mb-2 font-semibold text-[var(--text-primary)]">
                    {t('appointments.response.uncertain')} ({stats.uncertainCount})
                  </h3>
                  <ParticipantList people={uncertainList} />
                </section>
              )}
            </>
          )}
        </div>
      </Modal>
    </>
  );
}

function StatRow({ label, count }: { label: string; count: number }) {
  return (
    <div className="flex items-baseline justify-between gap-3 text-sm">
      <span className="text-[var(--text-secondary)]">{label}</span>
      <span className="font-semibold tabular-nums text-[var(--text-primary)]">{count}</span>
    </div>
  );
}
