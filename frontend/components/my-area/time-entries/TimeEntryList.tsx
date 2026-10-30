/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer } from '@/components/ui/list';
import { EmptyStateCard, ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useUrlSyncedListState } from '@/lib/list';
import { timeEntryListDefinition } from '@/lib/list/timeEntryList';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useCreateUserTimeEntry,
  useDeleteUserTimeEntry,
  useHasActiveOrganizationMembership,
  useUpdateUserTimeEntry,
  useUserTimeEntries,
} from '@/lib/store';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import type { UserTimeEntryDto } from '@/types/timeEntry';
import { TimeEntryCard } from './TimeEntryCard';
import { ModePickerModal, TimeEntryFormModal } from './TimeEntryFormModal';

type FormMode = 'stamp' | 'manual';

export function TimeEntryList() {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const listState = useUrlSyncedListState(timeEntryListDefinition);
  const {
    data: entries = [],
    pages,
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useUserTimeEntries({ listQuery: listState.listQuery });
  const { hasOrganizationMembership, isLoading: membershipLoading } =
    useHasActiveOrganizationMembership();
  const canManageTimeEntries = hasOrganizationMembership;
  const createEntry = useCreateUserTimeEntry();
  const updateEntry = useUpdateUserTimeEntry();
  const deleteEntry = useDeleteUserTimeEntry();

  const [isPickerOpen, setIsPickerOpen] = useState(false);
  const [formMode, setFormMode] = useState<FormMode | null>(null);
  const [editingEntry, setEditingEntry] = useState<UserTimeEntryDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<UserTimeEntryDto | null>(null);

  const openAddForm = () => {
    setEditingEntry(null);
    setIsPickerOpen(true);
  };

  const handleModeSelect = (mode: FormMode) => {
    setIsPickerOpen(false);
    setEditingEntry(null);
    setFormMode(mode);
  };

  const closeForm = () => {
    setFormMode(null);
  };

  const handleStampOut = async (entry: UserTimeEntryDto) => {
    try {
      await updateEntry.mutateAsync({
        entryId: entry.id,
        data: {
          organizationId: entry.organizationId,
          organizationGroupId: entry.organizationGroupId,
          startTime: entry.startTime,
          endTime: getOrganizationWallClockNow(),
          description: entry.description,
        },
      });
      showSuccess(t('myArea.timeEntries.success.stampedOut'));
    } catch (err) {
      showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
    }
  };

  const subtitle = useMemo(() => {
    const count = pages?.[0]?.totalElements ?? entries.length;
    return count === 0
      ? t('myArea.timeEntries.subtitle')
      : t('myArea.timeEntries.count', { count });
  }, [entries.length, pages, t]);

  const addLabel = t('common.add');

  const header = (
    <PageHeader
      title={t('myArea.timeEntries.title')}
      description={subtitle}
      size="compact"
      action={
        canManageTimeEntries ? (
          <Button type="button" onClick={openAddForm} size="xs" fullWidth="responsive">
            <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
            {addLabel}
          </Button>
        ) : undefined
      }
    />
  );

  if (!membershipLoading && !canManageTimeEntries) {
    return (
      <>
        {header}
        <PaginatedListView
          definition={timeEntryListDefinition}
          state={listState}
          totalItems={0}
          emptyState={<EmptyStateCard>{t('myArea.absence.noOrganizations')}</EmptyStateCard>}
        >
          {null}
        </PaginatedListView>
      </>
    );
  }

  return (
    <>
      {header}

      <PaginatedListView
        definition={timeEntryListDefinition}
        state={listState}
        totalItems={entries.length}
        isPending={isPending}
        error={
          isError ? (
            <ErrorState message={t('myArea.error.loadFailed')} className="mt-1 sm:mt-0" />
          ) : undefined
        }
        emptyState={paginatedListEmptyCard(t('myArea.timeEntries.empty'))}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('myArea.timeEntries.title')}>
          {entries.map((entry) => {
            const rowBusy =
              (updateEntry.isPending && updateEntry.variables?.entryId === entry.id) ||
              (deleteEntry.isPending && deleteTarget?.id === entry.id);
            const stampOutBusy =
              !entry.endTime &&
              updateEntry.isPending &&
              updateEntry.variables?.entryId === entry.id;

            return (
              <TimeEntryCard
                key={entry.id}
                entry={entry}
                href={`${MY_AREA_BASE_PATH}/time-entries/${entry.id}`}
                onStampOut={
                  canManageTimeEntries && !entry.endTime
                    ? () => void handleStampOut(entry)
                    : undefined
                }
                isStampOutPending={stampOutBusy}
                onEdit={canManageTimeEntries ? () => setEditingEntry(entry) : undefined}
                onDelete={() => setDeleteTarget(entry)}
                actionsDisabled={rowBusy}
                showActions={canManageTimeEntries}
              />
            );
          })}
        </ListContainer>
      </PaginatedListView>

      {canManageTimeEntries ? (
        <>
          <ModePickerModal
            isOpen={isPickerOpen}
            onClose={() => setIsPickerOpen(false)}
            onSelect={handleModeSelect}
          />

          <TimeEntryFormModal
            isOpen={formMode !== null}
            mode={formMode ?? 'manual'}
            onClose={closeForm}
            entry={undefined}
            onSave={async (data) => {
              await createEntry.mutateAsync(data);
            }}
          />

          <TimeEntryFormModal
            isOpen={editingEntry !== null}
            mode="manual"
            onClose={() => setEditingEntry(null)}
            entry={editingEntry ?? undefined}
            onSave={async (data) => {
              if (!editingEntry) return;
              await updateEntry.mutateAsync({ entryId: editingEntry.id, data });
            }}
          />

          <ConfirmDialog
            isOpen={deleteTarget !== null}
            onClose={() => setDeleteTarget(null)}
            onConfirm={async () => {
              if (!deleteTarget) return;
              try {
                await deleteEntry.mutateAsync(deleteTarget.id);
                showSuccess(t('myArea.timeEntries.success.deleted'));
                setDeleteTarget(null);
              } catch (err) {
                showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
              }
            }}
            title={t('myArea.timeEntries.deleteConfirmTitle')}
            message={t('myArea.timeEntries.deleteConfirm')}
            confirmText={t('common.delete')}
            isLoading={deleteEntry.isPending}
          />
        </>
      ) : null}
    </>
  );
}
