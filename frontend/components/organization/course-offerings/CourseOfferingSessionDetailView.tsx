/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useLayoutEffect, useMemo, useState } from 'react';
import { CourseSessionFormModal } from '@/components/organization/course-offerings/CourseSessionFormModal';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState, NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useDeleteCourseSession,
  useOrganizationCourseOffering,
  useUpdateCourseSession,
} from '@/lib/store';
import {
  canEditCourseOffering,
  courseSessionTypeLabelKey,
  getOrganizationCourseOfferingDetailPath,
} from '@/lib/utils/courseOffering';
import { formatAppointmentDateTimeRange } from '@/lib/utils/date';
import type { CourseSessionRequest } from '@/types/courseOffering';
import { PermissionTypeEnum } from '@/types/permission';

interface Props {
  organizationId: number;
  offeringId: number;
  sessionId: number;
}

export function CourseOfferingSessionDetailView({ organizationId, offeringId, sessionId }: Props) {
  const t = useTranslation();
  const router = useRouter();
  const { showSuccess, showError } = useToast();
  const { hasPermission } = usePermissions();
  const setBackHref = useSetBackHref();

  const canRead = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.COURSE_OFFERING_WRITE);

  const offeringPath = useMemo(
    () => getOrganizationCourseOfferingDetailPath(organizationId, offeringId),
    [organizationId, offeringId],
  );

  useLayoutEffect(() => {
    setBackHref(offeringPath);
    return () => setBackHref(null);
  }, [offeringPath, setBackHref]);

  const {
    data: offering,
    isLoading,
    isError,
  } = useOrganizationCourseOffering(canRead ? organizationId : null, offeringId, canRead);

  const updateSession = useUpdateCourseSession(organizationId, offeringId);
  const deleteSession = useDeleteCourseSession(organizationId, offeringId);

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const session = offering?.sessions.find((s) => s.id === sessionId) ?? null;
  const editable = Boolean(canWrite && offering && canEditCourseOffering(offering.status));

  const saveEdit = async (payload: CourseSessionRequest) => {
    try {
      await updateSession.mutateAsync({ sessionId, data: payload });
      showSuccess(t('courseOfferings.sessions.saved'));
      setEditOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.sessions.saveFailed'));
    }
  };

  const confirmDelete = async () => {
    try {
      await deleteSession.mutateAsync(sessionId);
      showSuccess(t('courseOfferings.sessions.deleted'));
      setDeleteOpen(false);
      router.push(offeringPath);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.sessions.deleteFailed'));
    }
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (isError) {
    return <ErrorState message={t('courseOfferings.list.loadFailed')} />;
  }

  if (!offering || !session) {
    return <NotFoundState message={t('courseOfferings.sessions.notFound')} />;
  }

  const dateTimeRange = formatAppointmentDateTimeRange(session.start, session.end);

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          eyebrow={offering.trainingCourseTypeName}
          title={t(courseSessionTypeLabelKey(session.type))}
          subtitle={<p>{dateTimeRange}</p>}
          actions={
            editable ? (
              <DetailCardAction
                label={t('common.edit')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={() => setEditOpen(true)}
              />
            ) : undefined
          }
        />

        <div className="grid grid-cols-1 gap-3 border-t border-[var(--border-color)] pt-4 text-sm sm:grid-cols-2">
          <FieldDisplay
            label={t('courseOfferings.sessions.schedule')}
            value={dateTimeRange}
            className="sm:col-span-2"
          />
          <FieldDisplay
            label={t('courseOfferings.sessions.location')}
            value={session.location?.trim() || undefined}
            fallback={t('common.notAvailable')}
          />
          <FieldDisplay
            label={t('courseOfferings.sessions.notes')}
            value={session.notes?.trim() || undefined}
            fallback={t('common.notAvailable')}
            multiline
          />
        </div>
      </Card>

      {editable ? (
        <ActionsSection title={t('courseOfferings.actions.title')}>
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('courseOfferings.sessions.deleteAction')}
            subtitle={t('courseOfferings.sessions.deleteConfirm')}
            onClick={() => setDeleteOpen(true)}
          />
        </ActionsSection>
      ) : null}

      {editOpen ? (
        <CourseSessionFormModal
          session={session}
          isSaving={updateSession.isPending}
          onClose={() => setEditOpen(false)}
          onSave={saveEdit}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => void confirmDelete()}
        title={t('courseOfferings.sessions.deleteTitle')}
        message={t('courseOfferings.sessions.deleteConfirm')}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={deleteSession.isPending}
      />
    </div>
  );
}
