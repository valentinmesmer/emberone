/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { CourseSessionFormModal } from '@/components/organization/course-offerings/CourseSessionFormModal';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow, ScheduleMetaRow } from '@/components/ui/list';
import { SectionEditAction, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useAddCourseSession, useDeleteCourseSession, useUpdateCourseSession } from '@/lib/store';
import {
  canEditCourseOffering,
  courseSessionTypeLabelKey,
  getOrganizationCourseSessionDetailPath,
} from '@/lib/utils/courseOffering';
import { formatAppointmentDateTimeRange } from '@/lib/utils/date';
import type {
  CourseOfferingDto,
  CourseSessionDto,
  CourseSessionRequest,
} from '@/types/courseOffering';

interface Props {
  organizationId: number;
  offering: CourseOfferingDto;
  canWrite: boolean;
}

export function CourseOfferingSessionsSection({ organizationId, offering, canWrite }: Props) {
  const t = useTranslation();
  const { showSuccess, showError } = useToast();
  const editable = canWrite && canEditCourseOffering(offering.status);

  const addSession = useAddCourseSession(organizationId, offering.id);
  const updateSession = useUpdateCourseSession(organizationId, offering.id);
  const deleteSession = useDeleteCourseSession(organizationId, offering.id);

  const [createOpen, setCreateOpen] = useState(false);
  const [editingSession, setEditingSession] = useState<CourseSessionDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CourseSessionDto | null>(null);

  const saveCreate = async (payload: CourseSessionRequest) => {
    try {
      await addSession.mutateAsync(payload);
      showSuccess(t('courseOfferings.sessions.saved'));
      setCreateOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.sessions.saveFailed'));
    }
  };

  const saveEdit = async (payload: CourseSessionRequest) => {
    if (!editingSession) return;
    try {
      await updateSession.mutateAsync({ sessionId: editingSession.id, data: payload });
      showSuccess(t('courseOfferings.sessions.saved'));
      setEditingSession(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.sessions.saveFailed'));
    }
  };

  const confirmDelete = async () => {
    if (!deleteTarget) return;
    try {
      await deleteSession.mutateAsync(deleteTarget.id);
      showSuccess(t('courseOfferings.sessions.deleted'));
      setDeleteTarget(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.sessions.deleteFailed'));
    }
  };

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        title={t('courseOfferings.sessions.title')}
        subtitle={t('courseOfferings.sessions.subtitle')}
        action={
          editable ? (
            <SectionEditAction label={t('common.add')} onClick={() => setCreateOpen(true)} />
          ) : null
        }
      />

      {offering.sessions.length === 0 ? (
        <p className="mt-3 text-sm text-[var(--text-secondary)]">
          {t('courseOfferings.sessions.empty')}
        </p>
      ) : (
        <ListContainer className="mt-3" variant="inset">
          {offering.sessions.map((session) => (
            <ListRow
              key={session.id}
              href={getOrganizationCourseSessionDetailPath(organizationId, offering.id, session.id)}
              title={t(courseSessionTypeLabelKey(session.type))}
              meta={
                <ScheduleMetaRow
                  when={formatAppointmentDateTimeRange(session.start, session.end)}
                />
              }
              actions={
                editable ? (
                  <>
                    <IconButton
                      variant="action"
                      aria-label={t('common.edit')}
                      icon={<Pencil className="h-4 w-4" aria-hidden />}
                      onClick={() => setEditingSession(session)}
                    />
                    <IconButton
                      variant="action"
                      aria-label={t('common.delete')}
                      icon={<Trash2 className="h-4 w-4" aria-hidden />}
                      onClick={() => setDeleteTarget(session)}
                    />
                  </>
                ) : undefined
              }
            />
          ))}
        </ListContainer>
      )}

      {createOpen ? (
        <CourseSessionFormModal
          isSaving={addSession.isPending}
          onClose={() => setCreateOpen(false)}
          onSave={saveCreate}
        />
      ) : null}

      {editingSession ? (
        <CourseSessionFormModal
          session={editingSession}
          isSaving={updateSession.isPending}
          onClose={() => setEditingSession(null)}
          onSave={saveEdit}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteTarget != null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void confirmDelete()}
        title={t('courseOfferings.sessions.deleteTitle')}
        message={t('courseOfferings.sessions.deleteConfirm')}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={deleteSession.isPending}
      />
    </Card>
  );
}
