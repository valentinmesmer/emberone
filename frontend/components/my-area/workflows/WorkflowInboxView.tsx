/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { WorkflowBeginModalHost } from '@/components/my-area/workflows/WorkflowBeginModalHost';
import { WorkflowTypePickerModal } from '@/components/my-area/workflows/WorkflowTypePickerModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ListContainer, ListRow } from '@/components/ui/list';
import { ListControls, ListLoadMore, ListNoResults } from '@/components/ui/list-filter';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import { usePaginatedListRuntimeOptions } from '@/lib/list/useListFilterOptions';
import { workflowInboxListDefinition } from '@/lib/list/workflowList';
import { getMyAreaWorkflowDetailPath } from '@/lib/my-area/myAreaSections';
import {
  useBlockedBeginnableWorkflowTypeIds,
  useMyBeginnableWorkflowTypes,
  useMyWorkflowOpenedPage,
  useMyWorkflowParticipationsPage,
  useSelf,
  useUrlSyncedWorkflowInboxListState,
} from '@/lib/store';
import {
  workflowParticipationListStatusLabel,
  workflowStatusBadgeTone,
  workflowStatusRequiresAction,
  workflowTypeTranslationKey,
} from '@/lib/utils/workflowLabels';
import type { WorkflowDto, WorkflowStatus } from '@/types/workflow';

export function WorkflowInboxView() {
  const t = useTranslation();
  const router = useRouter();
  const listState = useUrlSyncedWorkflowInboxListState();
  const runtimeOptions = usePaginatedListRuntimeOptions({
    definition: workflowInboxListDefinition,
    state: listState,
  });
  const {
    data: openedWorkflows = [],
    pages: openedPages,
    isPending: openedLoading,
    error: openedError,
    hasNextPage: openedHasNextPage,
    isFetchingNextPage: openedFetchingNextPage,
    fetchNextPage: fetchNextOpenedPage,
  } = useMyWorkflowOpenedPage({ listQuery: listState.listQuery });
  const {
    data: participations = [],
    pages: participationPages,
    isPending: participationsLoading,
    error: participationsError,
    hasNextPage: participationsHasNextPage,
    isFetchingNextPage: participationsFetchingNextPage,
    fetchNextPage: fetchNextParticipationsPage,
  } = useMyWorkflowParticipationsPage({ listQuery: listState.listQuery });
  const isLoading = openedLoading || participationsLoading;
  const error = openedError ?? participationsError;
  const { data: self } = useSelf();
  const beginnableTypesQuery = useMyBeginnableWorkflowTypes();
  const beginnableTypes = useMemo(
    () => beginnableTypesQuery.data ?? [],
    [beginnableTypesQuery.data],
  );

  const [pickerOpen, setPickerOpen] = useState(false);
  const [beginTypeId, setBeginTypeId] = useState<number | null>(null);
  const [beginModalOpen, setBeginModalOpen] = useState(false);
  const [beginModalKey, setBeginModalKey] = useState(0);
  const blockedTypeIds = useBlockedBeginnableWorkflowTypeIds(beginnableTypes);
  const availableTypes = useMemo(
    () =>
      beginnableTypes.filter(
        (type) => type.listedInBeginnableCatalog && !blockedTypeIds.has(type.workflowTypeId),
      ),
    [beginnableTypes, blockedTypeIds],
  );
  const showAddWorkflow = availableTypes.length > 0;

  const openBeginModal = (workflowTypeId: number) => {
    setBeginTypeId(workflowTypeId);
    setBeginModalKey((key) => key + 1);
    setBeginModalOpen(true);
  };

  const handleAddWorkflow = () => {
    if (availableTypes.length === 1) {
      openBeginModal(availableTypes[0].workflowTypeId);
      return;
    }
    setPickerOpen(true);
  };

  const handleTypePicked = (workflowTypeId: number) => {
    setPickerOpen(false);
    openBeginModal(workflowTypeId);
  };

  const openedTotal = openedPages?.[0]?.totalElements;
  const participationsTotal = participationPages?.[0]?.totalElements;
  const totalCount =
    openedTotal != null && participationsTotal != null
      ? openedTotal + participationsTotal
      : openedWorkflows.length + participations.length;
  const description =
    totalCount === 0
      ? t('myArea.workflowsInbox.subtitle')
      : t('myArea.workflowsInbox.count', { count: totalCount });

  const openWorkflowDetail = (wf: WorkflowDto) => {
    router.push(getMyAreaWorkflowDetailPath(wf.id));
  };

  const renderParticipationRow = (wf: WorkflowDto) => {
    const typeLabel = t(workflowTypeTranslationKey(wf.workflowTypeId));
    const statusBadge = workflowParticipationListStatusLabel(wf, t);
    const openerName =
      typeof wf.openedByDisplayName === 'string' && wf.openedByDisplayName.trim() !== ''
        ? wf.openedByDisplayName.trim()
        : t('common.notAvailable');

    return (
      <ListRow
        key={wf.id}
        tone={statusBadge.rowTone}
        title={typeLabel}
        onClick={() => openWorkflowDetail(wf)}
        detailHref={getMyAreaWorkflowDetailPath(wf.id)}
        subtitle={<span>{t('myArea.workflows.fields.openedBy', { name: openerName })}</span>}
        badges={
          <Badge tone={statusBadge.tone} pill>
            {statusBadge.label}
          </Badge>
        }
      />
    );
  };

  const renderOpenedRow = (wf: WorkflowDto) => {
    const typeLabel = t(workflowTypeTranslationKey(wf.workflowTypeId));
    const status = String(wf.status).toUpperCase() as WorkflowStatus;
    const statusLabel = t(`myArea.workflows.status.${status}`);

    return (
      <ListRow
        key={wf.id}
        tone={workflowStatusRequiresAction(status) ? 'active' : 'default'}
        title={typeLabel}
        onClick={() => openWorkflowDetail(wf)}
        detailHref={getMyAreaWorkflowDetailPath(wf.id)}
        badges={
          <Badge tone={workflowStatusBadgeTone(status)} pill>
            {statusLabel}
          </Badge>
        }
      />
    );
  };

  const listControls = (
    <ListControls
      definition={workflowInboxListDefinition}
      state={listState}
      runtimeOptions={runtimeOptions}
    />
  );

  const header = (
    <>
      <PageHeader
        title={t('myArea.workflowsInbox.title')}
        description={description}
        size="compact"
        action={
          showAddWorkflow ? (
            <Button type="button" onClick={handleAddWorkflow} size="xs" fullWidth="responsive">
              <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
              {t('common.add')}
            </Button>
          ) : null
        }
      />
      {listControls}
    </>
  );

  if (error) {
    return (
      <>
        {header}
        <ErrorState message={t('myArea.error.loadFailed')} className="mt-1 sm:mt-0" />
      </>
    );
  }

  const bothEmpty = !isLoading && participations.length === 0 && openedWorkflows.length === 0;

  if (bothEmpty && listState.hasActiveFilters) {
    return (
      <>
        {header}
        <ListNoResults onReset={listState.reset} />
      </>
    );
  }

  return (
    <>
      {header}
      {self ? (
        <>
          <WorkflowTypePickerModal
            isOpen={pickerOpen}
            onClose={() => setPickerOpen(false)}
            types={availableTypes}
            onSelect={handleTypePicked}
          />
          <WorkflowBeginModalHost
            workflowTypeId={beginTypeId}
            isOpen={beginModalOpen}
            onClose={() => setBeginModalOpen(false)}
            userData={self}
            modalKey={beginModalKey}
          />
        </>
      ) : null}
      <div className="mt-1 w-full space-y-4 sm:mt-0">
        {isLoading ? (
          <LoadingState variant="embedded" size="sm" />
        ) : (
          <>
            <section aria-label={t('myArea.workflowsParticipations.title')}>
              <Card className="overflow-hidden !rounded-lg !p-0">
                <div className="border-b border-[var(--border-color)] px-3 py-2.5">
                  <p className="text-sm font-semibold text-[var(--text-primary)]">
                    {t('myArea.workflowsParticipations.title')}
                  </p>
                  <p className="text-xs text-[var(--text-secondary)]">
                    {t('myArea.workflowsParticipations.subtitle')}
                  </p>
                </div>
                {participations.length === 0 ? (
                  <p className="px-3 py-3 text-sm text-[var(--text-secondary)]">
                    {t('myArea.workflowsParticipations.empty')}
                  </p>
                ) : (
                  <>
                    <ListContainer
                      label={t('myArea.workflowsParticipations.title')}
                      variant="flush"
                    >
                      {participations.map(renderParticipationRow)}
                    </ListContainer>
                    <ListLoadMore
                      hasNextPage={!!participationsHasNextPage}
                      isFetchingNextPage={participationsFetchingNextPage}
                      onLoadMore={() => void fetchNextParticipationsPage()}
                    />
                  </>
                )}
              </Card>
            </section>

            <section aria-label={t('myArea.workflowsOpened.title')}>
              <Card className="overflow-hidden !rounded-lg !p-0">
                <div className="border-b border-[var(--border-color)] px-3 py-2.5">
                  <p className="text-sm font-semibold text-[var(--text-primary)]">
                    {t('myArea.workflowsOpened.title')}
                  </p>
                  <p className="text-xs text-[var(--text-secondary)]">
                    {t('myArea.workflowsOpened.subtitle')}
                  </p>
                </div>
                {openedWorkflows.length === 0 ? (
                  <p className="px-3 py-3 text-sm text-[var(--text-secondary)]">
                    {t('myArea.workflowsOpened.empty')}
                  </p>
                ) : (
                  <>
                    <ListContainer label={t('myArea.workflowsOpened.title')} variant="flush">
                      {openedWorkflows.map(renderOpenedRow)}
                    </ListContainer>
                    <ListLoadMore
                      hasNextPage={!!openedHasNextPage}
                      isFetchingNextPage={openedFetchingNextPage}
                      onLoadMore={() => void fetchNextOpenedPage()}
                    />
                  </>
                )}
              </Card>
            </section>
          </>
        )}
      </div>
    </>
  );
}
