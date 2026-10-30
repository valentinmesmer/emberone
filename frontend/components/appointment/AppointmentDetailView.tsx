/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { CalendarPlus, Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { AppointmentDetailPanel } from '@/components/appointment/AppointmentDetailPanel';
import { EditAppointmentModal } from '@/components/appointment/EditAppointmentModal';
import { SeriesEditDialog } from '@/components/appointment/SeriesEditDialog';
import { CreateEventModal } from '@/components/event/CreateEventModal';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useAuth } from '@/lib/auth/AuthContext';
import { DEPARTMENT_SECTION, getDepartmentSectionPath } from '@/lib/department/sections';
import { importAppointmentYesResponsesAsEventAttendance } from '@/lib/event/importAppointmentAttendance';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { buildAppointmentTopic, useRealtimeTopic } from '@/lib/realtime';
import {
  appointmentKeys,
  eventAttendanceKeys,
  eventKeys,
  useAppointment,
  useAppointmentAudienceGroups,
  useDeleteAppointment,
  useMarkAppointmentRead,
  useOrganizationById,
  useUpdateAppointment,
} from '@/lib/store';
import { createEvent, getWritePermissionForEventType } from '@/lib/utils';
import { eventTypeFromAppointmentType, hasAppointmentStarted } from '@/lib/utils/appointmentEvent';
import { formatToLocalDateTime } from '@/lib/utils/date';
import {
  type AppointmentDto,
  AppointmentResponseMode,
  type SeriesEditScope,
} from '@/types/appointment';
import type { CreateEventPayload, EventDto, EventModalInitialValues } from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';

interface AppointmentDetailViewProps {
  organizationId: number;
  appointmentId: number;
}

export function AppointmentDetailView({
  organizationId,
  appointmentId,
}: AppointmentDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { tenantId } = useAuth();
  const appointmentTopic = tenantId != null ? buildAppointmentTopic(tenantId, appointmentId) : null;
  useRealtimeTopic(appointmentTopic);
  const queryClient = useQueryClient();
  const { hasPermission } = usePermissions();
  const { showError, showSuccess } = useToast();

  const listPath = getDepartmentSectionPath(organizationId, DEPARTMENT_SECTION.APPOINTMENTS);

  const { data: organization, isLoading: orgLoading } = useOrganizationById(organizationId);

  const canRead = hasPermission(organizationId, PermissionTypeEnum.APPOINTMENT_READ);
  const canWrite = hasPermission(organizationId, PermissionTypeEnum.APPOINTMENT_WRITE);
  const canWriteAttendance = hasPermission(
    organizationId,
    PermissionTypeEnum.EVENT_ATTENDANCE_WRITE,
  );

  const { data: audienceGroups = [] } = useAppointmentAudienceGroups(
    canWrite ? organizationId : null,
  );

  const {
    data: appointment,
    isLoading: appointmentLoading,
    isError: appointmentError,
    refetch,
  } = useAppointment(organizationId, appointmentId);

  const updateMutation = useUpdateAppointment(organizationId);
  const deleteMutation = useDeleteAppointment(organizationId);
  const markRead = useMarkAppointmentRead();

  useEffect(() => {
    if (!appointment?.unread) {
      return;
    }
    markRead.mutate({ organizationId, appointmentId: appointment.id });
    // Only mark when the appointment first loads as unread
  }, [appointment?.id, appointment?.unread, organizationId, markRead.mutate]);

  const [editOpen, setEditOpen] = useState(false);
  const [seriesDialog, setSeriesDialog] = useState<'edit' | 'delete' | null>(null);
  const [pendingScope, setPendingScope] = useState<SeriesEditScope | null>(null);
  const [deleteScope, setDeleteScope] = useState<SeriesEditScope | undefined>(undefined);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [createEventOpen, setCreateEventOpen] = useState(false);
  const [importAfterCreate, setImportAfterCreate] = useState<EventDto | null>(null);
  const [importingResponses, setImportingResponses] = useState(false);
  const [isCreatingEvent, setIsCreatingEvent] = useState(false);

  if (orgLoading || appointmentLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!organization || !canRead) {
    return <p className="text-[var(--text-secondary)]">{t('appointments.errors.noAccess')}</p>;
  }

  if (!appointment) {
    return (
      <div className="w-full">
        <EmptyStateCard>
          {appointmentError
            ? t('appointments.errors.loadFailed')
            : t('appointments.errors.notFound')}
        </EmptyStateCard>
      </div>
    );
  }

  const targetEventType = eventTypeFromAppointmentType(appointment.type);
  const isArchived = appointment.archived === true;
  const canPromoteEventFromAppointmentPermission = hasPermission(
    organizationId,
    getWritePermissionForEventType(targetEventType),
  );
  const promoteEventAllowed = canWrite && canPromoteEventFromAppointmentPermission && !isArchived;
  const promoteStartOk = hasAppointmentStarted(appointment.start);

  const handleEditSave = async (
    data: Parameters<typeof updateMutation.mutateAsync>[0]['data'],
    scope?: SeriesEditScope,
  ) => {
    try {
      await updateMutation.mutateAsync({
        appointmentId: appointment.id,
        data,
        scope,
      });
      showSuccess(t('appointments.success.updated'));
      setEditOpen(false);
      setPendingScope(null);
      refetch();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('appointments.errors.updateFailed'));
    }
  };

  const handleDeleteConfirm = async () => {
    try {
      await deleteMutation.mutateAsync({ appointmentId: appointment.id, scope: deleteScope });
      showSuccess(t('appointments.success.deleted'));
      router.push(listPath);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('appointments.errors.deleteFailed'));
    }
  };

  const openEdit = (target: AppointmentDto) => {
    if (target.seriesId) {
      setSeriesDialog('edit');
    } else {
      setEditOpen(true);
    }
  };

  const openDelete = (target: AppointmentDto) => {
    if (target.seriesId) {
      setSeriesDialog('delete');
    } else {
      setDeleteScope(undefined);
      setDeleteConfirmOpen(true);
    }
  };

  const eventInitialValues: EventModalInitialValues = {
    start: formatToLocalDateTime(appointment.start),
    end: formatToLocalDateTime(appointment.end),
    location: appointment.location ?? '',
    description: appointment.description ?? '',
    topic: appointment.name,
  };

  const handleCreateAppointmentEvent = async (data: CreateEventPayload) => {
    try {
      setIsCreatingEvent(true);
      const created = await createEvent(targetEventType, data);
      showSuccess(t('events.success.created'));
      await queryClient.invalidateQueries({ queryKey: eventKeys.all });
      await queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
      void refetch();
      return created;
    } catch {
      showError(t('events.errors.createFailed'));
      throw new Error('create failed');
    } finally {
      setIsCreatingEvent(false);
    }
  };

  const handleImportAfterCreate = async () => {
    if (!importAfterCreate) return;
    try {
      setImportingResponses(true);
      const n = await importAppointmentYesResponsesAsEventAttendance(
        organizationId,
        targetEventType,
        importAfterCreate.id,
        appointment.id,
      );
      if (n > 0) {
        showSuccess(t('events.attendance.importFromAppointmentSuccess', { count: n }));
      } else {
        showSuccess(t('events.attendance.importFromAppointmentNone'));
      }
      await queryClient.invalidateQueries({ queryKey: eventAttendanceKeys.all });
    } catch (e) {
      showError(e instanceof Error ? e.message : t('events.attendance.addFailed'));
    } finally {
      setImportingResponses(false);
      setImportAfterCreate(null);
    }
  };

  return (
    <div className="w-full">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          title={appointment.name}
          subtitle={
            isArchived ? (
              <p className="font-medium uppercase tracking-wide">
                {t('appointments.archived.badge')}
              </p>
            ) : undefined
          }
          actions={
            canWrite || promoteEventAllowed ? (
              <>
                {promoteEventAllowed ? (
                  <Button
                    type="button"
                    size="xs"
                    variant="secondary"
                    disabled={!promoteStartOk}
                    title={
                      promoteStartOk
                        ? t('appointments.promoteToEvent')
                        : t('appointments.promoteToEventAfterStart')
                    }
                    onClick={() => promoteStartOk && setCreateEventOpen(true)}
                  >
                    <CalendarPlus className="h-3.5 w-3.5 sm:mr-1" aria-hidden />
                    <span className="hidden sm:inline">{t('appointments.promoteToEvent')}</span>
                  </Button>
                ) : null}
                {canWrite && !isArchived ? (
                  <>
                    <DetailCardAction
                      label={t('common.edit')}
                      icon={<Pencil className="h-4 w-4" aria-hidden />}
                      onClick={() => openEdit(appointment)}
                    />
                    <DetailCardAction
                      label={t('common.delete')}
                      icon={<Trash2 className="h-4 w-4" aria-hidden />}
                      onClick={() => openDelete(appointment)}
                    />
                  </>
                ) : null}
                {canWrite && isArchived ? (
                  <DetailCardAction
                    label={t('common.delete')}
                    icon={<Trash2 className="h-4 w-4" aria-hidden />}
                    onClick={() => openDelete(appointment)}
                  />
                ) : null}
              </>
            ) : undefined
          }
        />

        <AppointmentDetailPanel
          key={appointment.id}
          organizationId={organization.id}
          appointment={appointment}
          canWrite={canWrite && !isArchived}
        />
      </Card>

      <EditAppointmentModal
        isOpen={editOpen}
        onClose={() => {
          setEditOpen(false);
          setPendingScope(null);
        }}
        organizationId={organization.id}
        appointment={appointment}
        organizationGroups={audienceGroups}
        scope={pendingScope ?? undefined}
        canManageAttachments={canWrite}
        isLoading={updateMutation.isPending}
        onSave={handleEditSave}
      />

      {seriesDialog && (
        <SeriesEditDialog
          isOpen
          mode={seriesDialog}
          onClose={() => {
            setSeriesDialog(null);
            setPendingScope(null);
          }}
          onConfirm={(scope) => {
            const mode = seriesDialog;
            setSeriesDialog(null);
            if (mode === 'edit') {
              setPendingScope(scope);
              setEditOpen(true);
            } else {
              setDeleteScope(scope);
              setDeleteConfirmOpen(true);
            }
          }}
        />
      )}

      <ConfirmDialog
        isOpen={deleteConfirmOpen}
        onClose={() => {
          setDeleteConfirmOpen(false);
          setDeleteScope(undefined);
        }}
        onConfirm={handleDeleteConfirm}
        title={t('appointments.confirm.deleteTitle')}
        message={t('appointments.confirm.delete')}
        confirmText={t('common.delete')}
        isLoading={deleteMutation.isPending}
      />

      <CreateEventModal
        isOpen={createEventOpen}
        onClose={() => setCreateEventOpen(false)}
        onCreate={handleCreateAppointmentEvent}
        onCreated={(created) => {
          if (canWriteAttendance && appointment.responseMode !== AppointmentResponseMode.NONE) {
            setImportAfterCreate(created);
          }
        }}
        eventType={targetEventType}
        organizationId={organizationId}
        isLoading={isCreatingEvent}
        initialValues={eventInitialValues}
        instanceKey={`from-apt-${appointment.id}`}
        promoteFromAppointmentId={appointment.id}
      />

      <ConfirmDialog
        variant="info"
        isOpen={!!importAfterCreate}
        onClose={() => setImportAfterCreate(null)}
        onConfirm={() => void handleImportAfterCreate()}
        title={t('events.fromAppointment.importResponsesTitle')}
        message={t('events.fromAppointment.importResponsesMessage')}
        confirmText={t('events.fromAppointment.importResponsesConfirm')}
        cancelText={t('events.fromAppointment.importResponsesSkip')}
        isLoading={importingResponses}
      />
    </div>
  );
}
