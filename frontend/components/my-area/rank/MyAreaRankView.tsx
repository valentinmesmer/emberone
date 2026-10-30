/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { RankRequestSubmitModal } from '@/components/my-area/rank/RankRequestSubmitModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, NoticeCard } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaWorkflowDetailPath } from '@/lib/my-area/myAreaSections';
import {
  formatDutyFunctionListSubtitle,
  formatDutyFunctionPeriod,
  hasMultipleDutyFunctionOrganizations,
} from '@/lib/ranks/dutyFunctionDisplay';
import { buildMyAreaRankListEntries, isFunctionEntry } from '@/lib/ranks/myAreaRankListEntries';
import {
  filterOpenRankRequestWorkflows,
  resolveRankRequestWorkflowTitle,
} from '@/lib/ranks/openRankRequestWorkflows';
import {
  useMyAvailableRanks,
  useMyBeginnableWorkflowTypes,
  useMyDutyFunctions,
  useMyRank,
  useMyWorkflowOpenedPage,
} from '@/lib/store';
import { formatDateShort } from '@/lib/utils/date';
import { workflowStatusBadgeTone } from '@/lib/utils/workflowLabels';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export function MyAreaRankView() {
  const t = useTranslation();
  const { data: rankProfile, isLoading, error } = useMyRank();
  const { data: openedWorkflows = [], isPending: inboxLoading } = useMyWorkflowOpenedPage();
  const { data: functions, isLoading: functionsLoading } = useMyDutyFunctions();
  const { data: beginnableTypes = [], isLoading: beginnableLoading } =
    useMyBeginnableWorkflowTypes();
  const [requestOpen, setRequestOpen] = useState(false);

  const openRankWorkflows = useMemo(
    () => filterOpenRankRequestWorkflows(openedWorkflows),
    [openedWorkflows],
  );
  const { data: availableRanks = [] } = useMyAvailableRanks(openRankWorkflows.length > 0);

  const current = rankProfile?.current ?? null;
  const history = rankProfile?.history ?? [];
  const hasOpenRankRequest = openRankWorkflows.length > 0;
  const rankRequestBeginnable = useMemo(
    () => beginnableTypes.some((type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.RANK_REQUEST),
    [beginnableTypes],
  );
  const canRequestFirstRank = rankRequestBeginnable && !current && !hasOpenRankRequest;

  const listEntries = useMemo(
    () =>
      buildMyAreaRankListEntries({
        functions,
        openRankWorkflows,
        current,
        history,
      }),
    [functions, openRankWorkflows, current, history],
  );

  const functionEntries = useMemo(() => listEntries.filter(isFunctionEntry), [listEntries]);
  const rankEntries = useMemo(
    () => listEntries.filter((entry) => !isFunctionEntry(entry)),
    [listEntries],
  );
  const showSectionLabels = functionEntries.length > 0 && rankEntries.length > 0;
  const multipleFunctionOrganizations = hasMultipleDutyFunctionOrganizations(functions);
  const pageDescription = t('ranks.myArea.subtitle');

  const requestRankAction = canRequestFirstRank ? (
    <Button type="button" onClick={() => setRequestOpen(true)} size="xs" fullWidth="responsive">
      {t('ranks.myArea.requestRank')}
    </Button>
  ) : undefined;

  if (isLoading || inboxLoading || functionsLoading || beginnableLoading) {
    return (
      <>
        <PageHeader title={t('myArea.menu.rank')} description={pageDescription} size="compact" />
        <div className="mt-1 w-full sm:mt-0">
          <LoadingState variant="embedded" size="sm" />
        </div>
      </>
    );
  }

  if (error) {
    return (
      <>
        <PageHeader title={t('myArea.menu.rank')} description={pageDescription} size="compact" />
        <div className="mt-1 w-full sm:mt-0">
          <NoticeCard>{t('ranks.myArea.loadFailed')}</NoticeCard>
        </div>
      </>
    );
  }

  return (
    <>
      <PageHeader
        title={t('myArea.menu.rank')}
        description={pageDescription}
        size="compact"
        action={requestRankAction}
      />

      <div className="mt-1 w-full space-y-3 sm:mt-0">
        {listEntries.length === 0 ? (
          <EmptyStateCard>{t('ranks.myArea.emptyCombined')}</EmptyStateCard>
        ) : (
          <div className="space-y-4">
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
                    if (entry.kind === 'rank_pending') {
                      const workflow = entry.workflow;
                      return (
                        <ListRow
                          key={entry.id}
                          tone="warning"
                          href={getMyAreaWorkflowDetailPath(workflow.id)}
                          title={resolveRankRequestWorkflowTitle(workflow, availableRanks)}
                          badges={
                            <>
                              <Badge tone="accent" pill>
                                {t('ranks.myArea.tag.application')}
                              </Badge>
                              <Badge tone={workflowStatusBadgeTone(workflow.status)}>
                                {t(`myArea.workflows.status.${workflow.status}`)}
                              </Badge>
                            </>
                          }
                        />
                      );
                    }

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
          </div>
        )}
      </div>

      {requestOpen ? <RankRequestSubmitModal isOpen onClose={() => setRequestOpen(false)} /> : null}
    </>
  );
}
