/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { AbsenceCard } from '@/components/my-area/absence/AbsenceCard';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer } from '@/components/ui/list';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { absenceOrgUserListDefinition } from '@/lib/list/absenceList';
import { getOrganizationUserAbsenceDetailPath } from '@/lib/navigation/userManagementSections';
import {
  useCreateOrganizationUserAbsence,
  useDeleteOrganizationUserAbsence,
  useOrganizationUserAbsenceListState,
  useOrganizationUserAbsences,
  useUpdateOrganizationUserAbsence,
} from '@/lib/store';
import { OrganizationUserAbsenceFormModal } from './OrganizationUserAbsenceFormModal';

interface UserAbsencesSectionProps {
  organizationId: number;
  userId: number;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
}

export function UserAbsencesSection({
  organizationId,
  userId,
  canWrite,
  isAddOpen,
  onAddClose,
}: UserAbsencesSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const listState = useOrganizationUserAbsenceListState();
  const {
    data: absences = [],
    isPending,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useOrganizationUserAbsences({
    organizationId,
    userId,
    listQuery: listState.listQuery,
  });
  const createAbsence = useCreateOrganizationUserAbsence(organizationId, userId);
  const updateAbsence = useUpdateOrganizationUserAbsence(organizationId, userId);
  const deleteAbsence = useDeleteOrganizationUserAbsence(organizationId, userId);

  const [editingAbsenceId, setEditingAbsenceId] = useState<number | null>(null);
  const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);

  const editingAbsence = editingAbsenceId
    ? absences.find((absence) => absence.id === editingAbsenceId)
    : undefined;

  return (
    <>
      <PaginatedListView
        definition={absenceOrgUserListDefinition}
        state={listState}
        organizationUserAbsenceFilterOptions={{
          scope: 'organizationUserAbsences',
          organizationId,
          userId,
        }}
        totalItems={absences.length}
        isPending={isPending}
        emptyState={paginatedListEmptyCard(t('myArea.absence.emptyAll'))}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
      >
        <ListContainer label={t('userManagement.sections.absences')}>
          {absences.map((absence) => (
            <AbsenceCard
              key={absence.id}
              absence={absence}
              href={getOrganizationUserAbsenceDetailPath(organizationId, userId, absence.id)}
              onEdit={canWrite ? () => setEditingAbsenceId(absence.id) : () => {}}
              onDelete={canWrite ? () => setDeleteTargetId(absence.id) : () => {}}
              actionsDisabled={!canWrite || deleteAbsence.isPending || updateAbsence.isPending}
            />
          ))}
        </ListContainer>
      </PaginatedListView>

      {canWrite ? (
        <>
          <OrganizationUserAbsenceFormModal
            isOpen={isAddOpen || editingAbsenceId !== null}
            onClose={() => {
              onAddClose();
              setEditingAbsenceId(null);
            }}
            organizationId={organizationId}
            absence={editingAbsence}
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
