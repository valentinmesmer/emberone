/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { EventCertificationGrantModal } from '@/components/event/EventCertificationGrantModal';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type DepartmentEventSection,
  getEventDetailPath,
  getEventTypeForDepartmentEventSection,
} from '@/lib/department/sections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useDeleteEventCertificationGrant, useEventCertificationGrantDetail } from '@/lib/store';
import { formatDateLong } from '@/lib/utils/date';
import { PermissionTypeEnum } from '@/types/permission';

export interface EventCertificationGrantDetailViewProps {
  organizationId: number;
  departmentSection: DepartmentEventSection;
  eventId: number;
  grantId: number;
}

export function EventCertificationGrantDetailView({
  organizationId,
  departmentSection,
  eventId,
  grantId,
}: EventCertificationGrantDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();

  const eventType = getEventTypeForDepartmentEventSection(departmentSection);
  const eventPath = getEventDetailPath(organizationId, departmentSection, eventId);

  const canReadAttendance = hasPermission(organizationId, PermissionTypeEnum.EVENT_ATTENDANCE_READ);
  const canRead =
    canReadAttendance && hasPermission(organizationId, PermissionTypeEnum.CERTIFICATION_READ);
  const canWrite =
    canReadAttendance && hasPermission(organizationId, PermissionTypeEnum.CERTIFICATION_WRITE);

  const { data: grantDetail, isLoading } = useEventCertificationGrantDetail(
    organizationId,
    eventType,
    eventId,
    grantId,
    { enabled: canRead },
  );
  const deleteGrant = useDeleteEventCertificationGrant(organizationId, eventType, eventId);

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const grant = grantDetail?.grant ?? null;
  const recipients = (grantDetail?.recipients ?? []).filter((row) => row.inScope);

  const handleDelete = async () => {
    if (!grant) return;
    try {
      await deleteGrant.mutateAsync(grant.id);
      showSuccess(t('events.certificationGrant.removeSuccess'));
      router.push(eventPath);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('events.certificationGrant.removeError'));
    }
  };

  if (!canRead) {
    return <NotFoundState message={t('organization.manage.noPermission')} />;
  }

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!grant) {
    return <NotFoundState message={t('events.certificationGrant.notFound')} />;
  }

  const definitionName = grant.certDefinitionName ?? `#${grant.tenantCertDefinitionId}`;
  const recipientModeLabel =
    grant.recipientMode === 'ALL_ATTENDEES'
      ? t('events.certificationGrant.allAttendees')
      : t('events.certificationGrant.specificAttendees');

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          title={definitionName}
          badges={
            <Badge tone="neutral" pill>
              {recipientModeLabel}
            </Badge>
          }
          actions={
            canWrite ? (
              <>
                <DetailCardAction
                  label={t('common.edit')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setEditOpen(true)}
                />
                <DetailCardAction
                  label={t('events.certificationGrant.remove')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => setDeleteOpen(true)}
                />
              </>
            ) : undefined
          }
        />

        <div className="space-y-4">
          <FieldDisplay
            label={t('events.certificationGrant.definitionLabel')}
            value={definitionName}
          />
          <FieldDisplay
            label={t('events.certificationGrant.recipientModeLabel')}
            value={recipientModeLabel}
          />
          <FieldDisplay
            label={t('events.certificationGrant.completedDateLabel')}
            value={formatDateLong(grant.completedDate)}
          />
          <FieldDisplay
            label={t('events.certificationGrant.notesLabel')}
            value={grant.notes}
            fallback={t('common.notAvailable')}
            multiline
          />
        </div>
      </Card>

      {grant.recipientMode === 'SPECIFIC' ? (
        <Card className="p-3 sm:p-4">
          <div className="mb-3">
            <SectionHeader title={t('events.certificationGrant.selectRecipients')} />
          </div>
          {recipients.length === 0 ? (
            <EmptyState>{t('events.certificationGrant.noAttendees')}</EmptyState>
          ) : (
            <ListContainer variant="inset" label={t('events.certificationGrant.selectRecipients')}>
              {recipients.map((row) => (
                <ListRow
                  key={row.userId}
                  accent={false}
                  chevron={false}
                  compact
                  title={
                    <>
                      <span className="font-semibold">{row.lastName ?? '-'}</span>
                      {', '}
                      <span className="font-normal">{row.firstName ?? ''}</span>
                    </>
                  }
                />
              ))}
            </ListContainer>
          )}
        </Card>
      ) : null}

      {editOpen ? (
        <EventCertificationGrantModal
          isOpen
          onClose={() => setEditOpen(false)}
          organizationId={organizationId}
          eventType={eventType}
          eventId={eventId}
          eventDefinitions={[]}
          grantId={grant.id}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => !deleteGrant.isPending && setDeleteOpen(false)}
        onConfirm={() => void handleDelete()}
        title={t('events.certificationGrant.removeTitle')}
        message={t('events.certificationGrant.removeConfirm', { name: definitionName })}
        confirmText={t('events.certificationGrant.remove')}
        cancelText={t('common.cancel')}
        isLoading={deleteGrant.isPending}
      />
    </div>
  );
}
