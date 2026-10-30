/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { useState } from 'react';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer } from '@/components/ui/list';
import { EmptyStateCard, ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { absenceListDefinition } from '@/lib/list/absenceList';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useCreateUserAbsence,
  useDeleteUserAbsence,
  useHasActiveOrganizationMembership,
  useUpdateUserAbsence,
  useUrlSyncedAbsenceListState,
  useUserAbsences,
} from '@/lib/store';
import { AbsenceCard } from './AbsenceCard';
import { AbsenceFormModal } from './AbsenceFormModal';

export function AbsenceList() {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const listState = useUrlSyncedAbsenceListState();
  const {
    data: absences,
    pages,
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useUserAbsences({ listQuery: listState.listQuery });
  const { hasOrganizationMembership, isLoading: membershipLoading } =
    useHasActiveOrganizationMembership();
  const canManageAbsences = hasOrganizationMembership;
  const createAbsence = useCreateUserAbsence();
  const updateAbsence = useUpdateUserAbsence();
  const deleteAbsence = useDeleteUserAbsence();

  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingAbsenceId, setEditingAbsenceId] = useState<number | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);

  const openAddForm = () => {
    setEditingAbsenceId(null);
    setIsFormOpen(true);
  };

  const actionsBusy = deleteAbsence.isPending || updateAbsence.isPending || createAbsence.isPending;

  const totalCount = pages?.[0]?.totalElements ?? absences.length;
  const subtitle =
    totalCount > 0
      ? t('myArea.absence.count', { count: totalCount })
      : t('myArea.absence.subtitle');

  const addLabel = t('common.add');

  const header = (
    <PageHeader
      title={t('myArea.absence.title')}
      description={subtitle}
      size="compact"
      action={
        canManageAbsences ? (
          <Button type="button" onClick={openAddForm} size="xs" fullWidth="responsive">
            <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
            {addLabel}
          </Button>
        ) : undefined
      }
    />
  );

  if (!membershipLoading && !canManageAbsences) {
    return (
      <>
        {header}
        <PaginatedListView
          definition={absenceListDefinition}
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
        definition={absenceListDefinition}
        state={listState}
        totalItems={absences.length}
        isPending={isPending}
        error={
          isError ? (
            <ErrorState message={t('myArea.error.loadFailed')} className="mt-1 sm:mt-0" />
          ) : undefined
        }
        emptyState={paginatedListEmptyCard(t('myArea.absence.emptyAll'))}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('myArea.absence.title')}>
          {absences.map((a) => (
            <AbsenceCard
              key={a.id}
              absence={a}
              href={`${MY_AREA_BASE_PATH}/absence/${a.id}`}
              onEdit={() => {
                setIsFormOpen(false);
                setEditingAbsenceId(a.id);
              }}
              onDelete={() => setDeleteTargetId(a.id)}
              actionsDisabled={actionsBusy}
              showActions={canManageAbsences}
            />
          ))}
        </ListContainer>
      </PaginatedListView>

      {canManageAbsences ? (
        <>
          <AbsenceFormModal
            isOpen={isFormOpen || editingAbsenceId !== null}
            onClose={() => {
              setIsFormOpen(false);
              setEditingAbsenceId(null);
            }}
            absence={
              editingAbsenceId !== null
                ? absences.find((x) => x.id === editingAbsenceId)
                : undefined
            }
            onSave={async (data) => {
              if (editingAbsenceId !== null) {
                await updateAbsence.mutateAsync({ absenceId: editingAbsenceId, data });
              } else {
                await createAbsence.mutateAsync(data);
              }
            }}
          />

          <ConfirmDialog
            isOpen={deleteTargetId !== null}
            onClose={() => setDeleteTargetId(null)}
            onConfirm={async () => {
              if (deleteTargetId === null) return;
              try {
                await deleteAbsence.mutateAsync(deleteTargetId);
                showSuccess(t('myArea.absence.success.deleted'));
                setDeleteTargetId(null);
              } catch (err) {
                showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
              }
            }}
            title={t('myArea.absence.deleteConfirmTitle')}
            message={t('myArea.absence.deleteConfirm')}
            confirmText={t('common.delete')}
            isLoading={deleteAbsence.isPending}
          />
        </>
      ) : null}
    </>
  );
}
