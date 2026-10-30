/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { OrganizationSetRankModal } from '@/components/organization/OrganizationSetRankModal';
import { Badge } from '@/components/ui/badge';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  OrganizationRankMemberDto,
  UserRankCurrentDto,
  UserRankSummaryDto,
} from '@/lib/api/ranks';
import {
  formatDutyFunctionListSubtitle,
  formatDutyFunctionPeriod,
  hasMultipleDutyFunctionOrganizations,
} from '@/lib/ranks/dutyFunctionDisplay';
import { buildMyAreaRankListEntries, isFunctionEntry } from '@/lib/ranks/myAreaRankListEntries';
import { useUserDutyFunctions, useUserRank } from '@/lib/store';
import { formatDateShort } from '@/lib/utils/date';

interface Props {
  organizationId: number;
  userId: number;
  firstName: string;
  lastName: string;
  canWrite: boolean;
  isMainOrganization: boolean;
  setRankOpen: boolean;
  onSetRankClose: () => void;
}

function toRankSummary(current: UserRankCurrentDto | null): UserRankSummaryDto | null {
  if (!current) {
    return null;
  }
  return {
    tenantRankId: current.tenantRankId,
    configKey: current.configKey,
    name: current.name,
    abbreviation: current.abbreviation,
    sortOrder: 0,
    startDate: current.startDate,
  };
}

export function UserRankSection({
  organizationId,
  userId,
  firstName,
  lastName,
  canWrite,
  isMainOrganization,
  setRankOpen,
  onSetRankClose,
}: Props) {
  const t = useTranslation();
  const {
    data: rankProfile,
    isLoading: rankLoading,
    error: rankError,
  } = useUserRank(organizationId, userId);
  const { data: functions, isLoading: functionsLoading } = useUserDutyFunctions(
    organizationId,
    userId,
  );
  const listEntries = useMemo(
    () =>
      buildMyAreaRankListEntries({
        functions,
        openRankWorkflows: [],
        current: rankProfile?.current ?? null,
        history: rankProfile?.history ?? [],
      }),
    [functions, rankProfile],
  );

  const functionEntries = useMemo(() => listEntries.filter(isFunctionEntry), [listEntries]);
  const rankEntries = useMemo(
    () => listEntries.filter((entry) => !isFunctionEntry(entry)),
    [listEntries],
  );
  const showSectionLabels = functionEntries.length > 0 && rankEntries.length > 0;
  const multipleFunctionOrganizations = hasMultipleDutyFunctionOrganizations(functions);

  const rankMember = useMemo((): OrganizationRankMemberDto => {
    return {
      userId,
      firstName,
      lastName,
      mainOrganization: isMainOrganization,
      currentRank: toRankSummary(rankProfile?.current ?? null),
    };
  }, [userId, firstName, lastName, isMainOrganization, rankProfile?.current]);

  const canSetRank = canWrite && isMainOrganization;

  if (rankLoading || functionsLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }
  if (rankError) {
    return <NoticeCard>{t('ranks.userSection.loadFailed')}</NoticeCard>;
  }

  return (
    <>
      <div className="space-y-4">
        {listEntries.length === 0 ? (
          <EmptyStateCard>{t('ranks.userSection.emptyCombined')}</EmptyStateCard>
        ) : (
          <>
            {functionEntries.length > 0 ? (
              <section aria-label={t('ranks.myArea.functions')}>
                {showSectionLabels ? (
                  <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                    {t('ranks.myArea.functions')}
                  </h2>
                ) : null}
                <ListContainer label={t('ranks.myArea.functions')}>
                  {functionEntries.map((entry) => {
                    const fn = entry.function;
                    if (entry.kind === 'function_history') {
                      return (
                        <ListRow
                          key={entry.id}
                          compact
                          accent={false}
                          title={fn.functionName}
                          subtitle={formatDutyFunctionListSubtitle(
                            fn,
                            multipleFunctionOrganizations,
                            formatDutyFunctionPeriod(fn),
                          )}
                          badges={<Badge tone="neutral">{fn.functionAbbreviation}</Badge>}
                        />
                      );
                    }
                    return (
                      <ListRow
                        key={entry.id}
                        title={fn.functionName}
                        subtitle={formatDutyFunctionListSubtitle(
                          fn,
                          multipleFunctionOrganizations,
                          t('ranks.myArea.since', {
                            date: formatDateShort(fn.membershipStartDate),
                          }),
                        )}
                        tone="active"
                        selected
                        badges={<Badge tone="accent">{fn.functionAbbreviation}</Badge>}
                      />
                    );
                  })}
                </ListContainer>
              </section>
            ) : null}

            {rankEntries.length > 0 ? (
              <section aria-label={t('ranks.myArea.rankSection')}>
                {showSectionLabels ? (
                  <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                    {t('ranks.myArea.rankSection')}
                  </h2>
                ) : null}
                <ListContainer label={t('ranks.myArea.rankSection')}>
                  {rankEntries.map((entry) => {
                    if (entry.kind === 'rank_current') {
                      const rank = entry.rank;
                      return (
                        <ListRow
                          key={entry.id}
                          title={rank.name}
                          subtitle={t('ranks.myArea.since', {
                            date: formatDateShort(rank.startDate),
                          })}
                          tone="active"
                          selected
                          badges={<Badge tone="accent">{rank.abbreviation}</Badge>}
                        />
                      );
                    }

                    if (entry.kind !== 'rank_history') {
                      return null;
                    }

                    const item = entry.item;
                    return (
                      <ListRow
                        key={entry.id}
                        compact
                        accent={false}
                        title={item.name ?? t('ranks.userSection.unknownRank')}
                        subtitle={`${formatDateShort(item.startDate)} - ${
                          item.endDate
                            ? formatDateShort(item.endDate)
                            : t('ranks.userSection.present')
                        }`}
                        badges={
                          item.abbreviation ? (
                            <Badge tone="neutral">{item.abbreviation}</Badge>
                          ) : null
                        }
                      />
                    );
                  })}
                </ListContainer>
              </section>
            ) : null}
          </>
        )}
      </div>

      {canSetRank && setRankOpen ? (
        <OrganizationSetRankModal
          organizationId={organizationId}
          member={rankMember}
          isOpen={setRankOpen}
          onClose={onSetRankClose}
        />
      ) : null}
    </>
  );
}
