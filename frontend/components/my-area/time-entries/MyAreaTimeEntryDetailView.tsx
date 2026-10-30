/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import {
  useDeleteUserTimeEntry,
  useHasActiveOrganizationMembership,
  useUpdateUserTimeEntry,
  useUserTimeEntry,
} from '@/lib/store';
import {
  formatDateTime,
  formatDurationBetween,
  getOrganizationWallClockNow,
} from '@/lib/utils/date';
import { TimeEntryFormModal } from './TimeEntryFormModal';
import { TimeEntryStampOutButton } from './TimeEntryStampOutButton';

export interface MyAreaTimeEntryDetailViewProps {
  entryId: number;
}

export function MyAreaTimeEntryDetailView({ entryId }: MyAreaTimeEntryDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { data: entry, isLoading } = useUserTimeEntry(entryId);
  const { hasOrganizationMembership } = useHasActiveOrganizationMembership();
  const canManageTimeEntries = hasOrganizationMembership;
  const updateEntry = useUpdateUserTimeEntry();
  const deleteEntry = useDeleteUserTimeEntry();

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [isDeleteOpen, setIsDeleteOpen] = useState(false);

  const loading = isLoading;

  const isOpen = Boolean(entry && !entry.endTime);

  const durationLabel = useMemo(() => {
    if (!entry?.endTime) {
      return t('common.notAvailable');
    }
    return formatDurationBetween(entry.startTime, entry.endTime);
  }, [entry, t]);

  const handleStampOut = async () => {
    if (!entry || entry.endTime) return;
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

  const handleDelete = async () => {
    if (!entry) return;
    try {
      await deleteEntry.mutateAsync(entry.id);
      showSuccess(t('myArea.timeEntries.success.deleted'));
      router.push(`${MY_AREA_BASE_PATH}/time-entries`);
    } catch (err) {
      showError(err instanceof Error ? err.message : t('myArea.error.loadFailed'));
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!entry) {
    return <NotFoundState message={t('myArea.timeEntries.error.notFound')} />;
  }

  const title = entry.organizationName;
  const groupName = entry.organizationGroupName?.trim() || t('myArea.timeEntries.noGroup');

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          title={title}
          badges={
            isOpen ? (
              <Badge tone="accent" pill>
                {t('myArea.timeEntries.running')}
              </Badge>
            ) : undefined
          }
          actions={
            canManageTimeEntries ? (
              <>
                <DetailCardAction
                  label={t('myArea.timeEntries.editTitle')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setIsEditOpen(true)}
                  disabled={updateEntry.isPending}
                />
                <DetailCardAction
                  label={t('myArea.timeEntries.delete')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => setIsDeleteOpen(true)}
                />
              </>
            ) : undefined
          }
        />

        {canManageTimeEntries && isOpen ? (
          <div className="mb-4">
            <TimeEntryStampOutButton
              onClick={() => void handleStampOut()}
              isLoading={updateEntry.isPending}
            />
          </div>
        ) : null}

        <div className="space-y-4">
          <FieldDisplay
            label={t('myArea.timeEntries.fields.organization')}
            value={entry.organizationName}
          />
          <FieldDisplay label={t('myArea.timeEntries.fields.group')} value={groupName} />
          <FieldDisplay
            label={t('myArea.timeEntries.fields.startTime')}
            value={formatDateTime(entry.startTime)}
          />
          <FieldDisplay
            label={t('myArea.timeEntries.fields.endTime')}
            value={entry.endTime ? formatDateTime(entry.endTime) : t('common.notAvailable')}
          />
          <FieldDisplay label={t('myArea.timeEntries.fields.duration')} value={durationLabel} />
          <FieldDisplay
            label={t('myArea.timeEntries.fields.description')}
            value={
              entry.description && entry.description.trim() !== ''
                ? entry.description
                : t('common.notAvailable')
            }
          />
        </div>
      </Card>

      {canManageTimeEntries ? (
        <>
          <TimeEntryFormModal
            isOpen={isEditOpen}
            mode="manual"
            onClose={() => setIsEditOpen(false)}
            entry={entry}
            onSave={async (data) => {
              await updateEntry.mutateAsync({ entryId: entry.id, data });
            }}
          />

          <ConfirmDialog
            isOpen={isDeleteOpen}
            onClose={() => setIsDeleteOpen(false)}
            onConfirm={() => void handleDelete()}
            title={t('myArea.timeEntries.deleteConfirmTitle')}
            message={t('myArea.timeEntries.deleteConfirm')}
            confirmText={t('common.delete')}
            isLoading={deleteEntry.isPending}
          />
        </>
      ) : null}
    </div>
  );
}
