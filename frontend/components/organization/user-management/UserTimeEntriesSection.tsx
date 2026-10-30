/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { TimeEntryCard } from '@/components/my-area/time-entries/TimeEntryCard';
import { TimeEntryFormModal } from '@/components/my-area/time-entries/TimeEntryFormModal';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { timeEntryOrgUserListDefinition } from '@/lib/list/timeEntryList';
import { getOrganizationUserTimeEntryDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useCreateOrganizationUserTimeEntry,
  useDeleteOrganizationUserTimeEntry,
  useOrganizationUserTimeEntries,
  useOrganizationUserTimeEntryListState,
  useUpdateOrganizationUserTimeEntry,
} from '@/lib/store';
import { getOrganizationWallClockNow } from '@/lib/utils/date';
import type { UserTimeEntryDto } from '@/types/timeEntry';

interface UserTimeEntriesSectionProps {
  organizationId: number;
  userId: number;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
}

export function UserTimeEntriesSection({
  organizationId,
  userId,
  canWrite,
  isAddOpen,
  onAddClose,
}: UserTimeEntriesSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const listState = useOrganizationUserTimeEntryListState();
  const {
    data: entries = [],
    isPending,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationUserTimeEntries({
    organizationId,
    userId,
    listQuery: listState.listQuery,
  });
  const createEntry = useCreateOrganizationUserTimeEntry(organizationId, userId);
  const updateEntry = useUpdateOrganizationUserTimeEntry(organizationId, userId);
  const deleteEntry = useDeleteOrganizationUserTimeEntry(organizationId, userId);

  const [editingEntry, setEditingEntry] = useState<UserTimeEntryDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<UserTimeEntryDto | null>(null);

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

  return (
    <>
      <PaginatedListView
        definition={timeEntryOrgUserListDefinition}
        state={listState}
        organizationUserTimeEntryFilterOptions={{
          scope: 'organizationUserTimeEntries',
          organizationId,
          userId,
        }}
        totalItems={entries.length}
        isPending={isPending}
        emptyState={paginatedListEmptyCard(t('userManagement.timeEntries.empty'))}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('userManagement.timeEntries.title')}>
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
                href={getOrganizationUserTimeEntryDetailPath(organizationId, userId, entry.id)}
                onStampOut={
                  canWrite && !entry.endTime ? () => void handleStampOut(entry) : undefined
                }
                isStampOutPending={stampOutBusy}
                onEdit={canWrite ? () => setEditingEntry(entry) : undefined}
                onDelete={canWrite ? () => setDeleteTarget(entry) : () => {}}
                actionsDisabled={!canWrite || rowBusy}
              />
            );
          })}
        </ListContainer>
      </PaginatedListView>

      {canWrite ? (
        <>
          <TimeEntryFormModal
            isOpen={isAddOpen}
            mode="manual"
            onClose={onAddClose}
            entry={undefined}
            fixedOrganizationId={organizationId}
            onSave={async (data) => {
              await createEntry.mutateAsync(data);
            }}
          />

          <TimeEntryFormModal
            isOpen={editingEntry !== null}
            mode="manual"
            onClose={() => setEditingEntry(null)}
            entry={editingEntry ?? undefined}
            fixedOrganizationId={organizationId}
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
