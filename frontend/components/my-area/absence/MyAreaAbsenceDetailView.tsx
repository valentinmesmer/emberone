/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { getAbsenceReasonLabelKey } from '@/lib/my-area/absenceReasonOptions';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useDeleteUserAbsence,
  useHasActiveOrganizationMembership,
  useMyOrganizations,
  useUpdateUserAbsence,
  useUserAbsence,
} from '@/lib/store';
import { formatDateLong } from '@/lib/utils';
import { AbsenceFormModal } from './AbsenceFormModal';

export interface MyAreaAbsenceDetailViewProps {
  absenceId: number;
}

export function MyAreaAbsenceDetailView({ absenceId }: MyAreaAbsenceDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { data: absence, isLoading } = useUserAbsence(absenceId);
  const { data: memberships = [] } = useMyOrganizations();
  const { hasOrganizationMembership } = useHasActiveOrganizationMembership();
  const canManageAbsences = hasOrganizationMembership;
  const updateAbsence = useUpdateUserAbsence();
  const deleteAbsence = useDeleteUserAbsence();

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const orgNameById = useMemo(() => {
    const map = new Map<number, string>();
    for (const m of memberships) {
      if (m.organization?.id != null) {
        map.set(m.organization.id, m.organization.name);
      }
    }
    return map;
  }, [memberships]);

  const loading = isLoading;

  const organizationsLabel = useMemo(() => {
    if (!absence?.organizationIds?.length) {
      return t('myArea.absence.orgUnknown');
    }
    return absence.organizationIds
      .map((id) => orgNameById.get(id) ?? `${t('myArea.absence.orgUnknown')} (${id})`)
      .join(', ');
  }, [absence, orgNameById, t]);

  const handleDelete = async () => {
    if (!absence) return;
    try {
      await deleteAbsence.mutateAsync(absence.id);
      showSuccess(t('myArea.absence.success.deleted'));
      router.push(`${MY_AREA_BASE_PATH}/absence`);
    } catch (err) {
      showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!absence) {
    return <NotFoundState message={t('myArea.absence.error.notFound')} />;
  }

  const title = t(getAbsenceReasonLabelKey(absence.reason));

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          title={title}
          actions={
            canManageAbsences ? (
              <>
                <DetailCardAction
                  label={t('myArea.absence.editTitle')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setIsEditOpen(true)}
                />
                <DetailCardAction
                  label={t('myArea.absence.delete')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => setIsDeleteOpen(true)}
                />
              </>
            ) : undefined
          }
        />

        <div className="space-y-4">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <FieldDisplay
              label={t('myArea.absence.fields.startDate')}
              value={formatDateLong(absence.startDate)}
            />
            <FieldDisplay
              label={t('myArea.absence.fields.endDate')}
              value={formatDateLong(absence.endDate)}
            />
          </div>
          <FieldDisplay
            label={t('myArea.absence.fields.organizations')}
            value={organizationsLabel}
          />
          <FieldDisplay
            label={t('myArea.absence.fields.comment')}
            value={
              absence.comment && absence.comment.trim() !== ''
                ? absence.comment
                : t('common.notAvailable')
            }
          />
        </div>
      </Card>

      {canManageAbsences ? (
        <>
          <AbsenceFormModal
            isOpen={isEditOpen}
            onClose={() => setIsEditOpen(false)}
            absence={absence}
            onSave={async (data) => {
              await updateAbsence.mutateAsync({ absenceId: absence.id, data });
            }}
          />

          <ConfirmDialog
            isOpen={isDeleteOpen}
            onClose={() => setIsDeleteOpen(false)}
            onConfirm={() => void handleDelete()}
            title={t('myArea.absence.deleteConfirmTitle')}
            message={t('myArea.absence.deleteConfirm')}
            confirmText={t('common.delete')}
            isLoading={deleteAbsence.isPending}
          />
        </>
      ) : null}
    </div>
  );
}
