/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { CalendarPlus, Pencil, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { AppointmentMarkAllReadButton } from '@/components/appointment/AppointmentMarkAllReadButton';
import { CreateAppointmentModal } from '@/components/appointment/CreateAppointmentModal';
import { EditAppointmentModal } from '@/components/appointment/EditAppointmentModal';
import { SeriesEditDialog } from '@/components/appointment/SeriesEditDialog';
import { CreateEventModal } from '@/components/event/CreateEventModal';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow, ScheduleMetaRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { SectionActions } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useOrganizationMembership } from '@/hooks/useWorkspaceOrganizations';
import { getAppointmentDetailPath } from '@/lib/department/sections';
import { importAppointmentYesResponsesAsEventAttendance } from '@/lib/event/importAppointmentAttendance';
import {
  AppointmentListPeriodParam,
  appointmentListDefinition,
  managedAppointmentListDefinition,
} from '@/lib/list/appointmentList';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  appointmentKeys,
  eventAttendanceKeys,
  eventKeys,
  useAppointmentAudienceGroups,
  useAppointmentList,
  useCreateAppointment,
  useDeleteAppointment,
  useMyAppointmentResponses,
  useOrganizationById,
  useUpdateAppointment,
  useUrlSyncedAppointmentListState,
} from '@/lib/store';
import { createEvent, getWritePermissionForEventType } from '@/lib/utils';
import { eventTypeFromAppointmentType, hasAppointmentStarted } from '@/lib/utils/appointmentEvent';
import { getAppointmentListTone } from '@/lib/utils/appointmentResponse';
import { formatAppointmentDateTimeRange, formatToLocalDateTime } from '@/lib/utils/date';
import {
  type AppointmentDto,
  AppointmentResponseMode,
  type SeriesEditScope,
} from '@/types/appointment';
import type {
  CreateEventPayload,
  EventDto,
  EventModalInitialValues,
  EventType,
} from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';

interface AppointmentListViewProps {
  organizationId: number;
}

export function AppointmentListView({ organizationId }: AppointmentListViewProps) {
  const membership = useOrganizationMembership(organizationId);
  if (membership.isPending) {
    return <LoadingState variant="embedded" />;
  }
  return <AppointmentListContent organizationId={organizationId} isMember={membership.isMember} />;
}

interface AppointmentListContentProps extends AppointmentListViewProps {
  isMember: boolean;
}

function AppointmentListContent({ organizationId, isMember }: AppointmentListContentProps) {
  const t = useTranslation();
  const queryClient = useQueryClient();
  const { hasPermission } = usePermissions();
  const { showError, showSuccess } = useToast();

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

  const listDefinition = isMember ? appointmentListDefinition : managedAppointmentListDefinition;
  const listState = useUrlSyncedAppointmentListState(listDefinition);
  const organizationAppointmentFilterOptions = useMemo(
    () => ({
      scope: 'organizationAppointments' as const,
      organizationId,
    }),
    [organizationId],
  );
  const emptyListMessage = useMemo(() => {
    const periods = Array.isArray(listState.filters.period)
      ? listState.filters.period.filter((value): value is string => typeof value === 'string')
      : [];
    if (periods.length === 1 && periods[0] === AppointmentListPeriodParam.PAST) {
      return t('appointments.emptyPast');
    }
    return t('appointments.emptyUpcoming');
  }, [listState.filters.period, t]);
  const {
    data: appointments,
    isPending,
    isError,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
    refetch,
  } = useAppointmentList({ organizationId, listQuery: listState.listQuery });
  const appointmentIds = useMemo(() => appointments.map((a) => a.id), [appointments]);
  const { data: myResponses = [] } = useMyAppointmentResponses(isMember ? appointmentIds : []);
  const myResponseByAppointmentId = useMemo(
    () => new Map(myResponses.map((r) => [r.appointmentId, r])),
    [myResponses],
  );
  const createMutation = useCreateAppointment(organizationId);
  const updateMutation = useUpdateAppointment(organizationId);
  const deleteMutation = useDeleteAppointment(organizationId);

  const [createOpen, setCreateOpen] = useState(false);
  const [editTarget, setEditTarget] = useState<AppointmentDto | null>(null);
  const [seriesDialog, setSeriesDialog] = useState<{
    mode: 'edit' | 'delete';
    appointment: AppointmentDto;
  } | null>(null);
  const [pendingScope, setPendingScope] = useState<SeriesEditScope | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<{
    appointment: AppointmentDto;
    scope?: SeriesEditScope;
  } | null>(null);
  const [promoteFor, setPromoteFor] = useState<AppointmentDto | null>(null);
  const [importAfterCreate, setImportAfterCreate] = useState<{
    event: EventDto;
    appointmentId: number;
    eventType: EventType;
  } | null>(null);
  const [isCreatingEvent, setIsCreatingEvent] = useState(false);
  const [importingResponses, setImportingResponses] = useState(false);

  if (orgLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!organization || !canRead) {
    return <p className="text-[var(--text-secondary)]">{t('appointments.errors.noAccess')}</p>;
  }

  const handleDeleteConfirm = async () => {
    if (!deleteTarget) return;
    const { appointment, scope } = deleteTarget;
    try {
      await deleteMutation.mutateAsync({ appointmentId: appointment.id, scope });
      showSuccess(t('appointments.success.deleted'));
      setDeleteTarget(null);
      refetch();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('appointments.errors.deleteFailed'));
    }
  };

  const handleEditSave = async (
    data: Parameters<typeof updateMutation.mutateAsync>[0]['data'],
    scope?: SeriesEditScope,
  ) => {
    if (!editTarget) return;
    try {
      await updateMutation.mutateAsync({
        appointmentId: editTarget.id,
        data,
        scope,
      });
      showSuccess(t('appointments.success.updated'));
      setEditTarget(null);
      refetch();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('appointments.errors.updateFailed'));
    }
  };

  const handlePromoteCreate = async (data: CreateEventPayload) => {
    const src = promoteFor;
    if (!src) {
      throw new Error('no appointment');
    }
    const eventType = eventTypeFromAppointmentType(src.type);
    try {
      setIsCreatingEvent(true);
      const created = await createEvent(eventType, data);
      showSuccess(t('events.success.created'));
      await queryClient.invalidateQueries({ queryKey: eventKeys.all });
      await queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
      refetch();
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
        importAfterCreate.eventType,
        importAfterCreate.event.id,
        importAfterCreate.appointmentId,
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
    <>
      <PageHeader
        title={t('appointments.title')}
        description={organization.name}
        size="compact"
        action={
          <SectionActions layout="inline">
            {isMember ? (
              <AppointmentMarkAllReadButton
                organizationId={organizationId}
                variant="outline"
                size="xs"
                fullWidth="responsive"
              />
            ) : null}
            {canWrite ? (
              <Button
                variant="primary"
                size="xs"
                onClick={() => setCreateOpen(true)}
                fullWidth="responsive"
              >
                {t('common.add')}
              </Button>
            ) : null}
          </SectionActions>
        }
      />

      <PaginatedListView
        definition={listDefinition}
        state={listState}
        organizationAppointmentFilterOptions={organizationAppointmentFilterOptions}
        totalItems={appointments.length}
        isPending={isPending}
        error={
          isError ? (
            <ErrorState
              message={t('appointments.errors.listLoadFailed')}
              className="mt-1 sm:mt-0"
            />
          ) : undefined
        }
        emptyState={paginatedListEmptyCard(emptyListMessage)}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage()}
        className="mt-1 w-full sm:mt-0"
      >
        <ListContainer label={t('appointments.title')}>
          {appointments.map((appointment) => {
            const promotePermission = hasPermission(
              organizationId,
              getWritePermissionForEventType(eventTypeFromAppointmentType(appointment.type)),
            );
            const promoteAllowed = canWrite && promotePermission && !appointment.archived;
            const promoteStartOk = hasAppointmentStarted(appointment.start);
            const tone = isMember
              ? getAppointmentListTone(appointment, myResponseByAppointmentId.get(appointment.id))
              : 'default';
            return (
              <ListRow
                key={appointment.id}
                href={getAppointmentDetailPath(organizationId, appointment.id)}
                title={appointment.name}
                tone={tone}
                accent={tone !== 'default'}
                ariaLabel={
                  appointment.unread
                    ? `${appointment.name} (${t('appointments.unread')})`
                    : undefined
                }
                meta={
                  <ScheduleMetaRow
                    when={formatAppointmentDateTimeRange(appointment.start, appointment.end)}
                  />
                }
                actions={
                  promoteAllowed || canWrite ? (
                    <>
                      {promoteAllowed ? (
                        <Button
                          type="button"
                          variant="secondary"
                          size="xs"
                          className="shrink-0 px-2"
                          disabled={!promoteStartOk}
                          title={
                            promoteStartOk
                              ? t('appointments.promoteToEvent')
                              : t('appointments.promoteToEventAfterStart')
                          }
                          onClick={() => promoteStartOk && setPromoteFor(appointment)}
                        >
                          <CalendarPlus className="h-3.5 w-3.5" aria-hidden />
                          <span className="sr-only">{t('appointments.promoteToEvent')}</span>
                        </Button>
                      ) : null}
                      {canWrite && !appointment.archived ? (
                        <>
                          <IconButton
                            variant="action"
                            icon={<Pencil className="h-4 w-4" aria-hidden />}
                            aria-label={t('common.edit')}
                            onClick={() => {
                              if (appointment.seriesId) {
                                setSeriesDialog({ mode: 'edit', appointment });
                              } else {
                                setEditTarget(appointment);
                              }
                            }}
                          />
                          <IconButton
                            variant="action"
                            icon={<Trash2 className="h-4 w-4" aria-hidden />}
                            aria-label={t('common.delete')}
                            onClick={() => {
                              if (appointment.seriesId) {
                                setSeriesDialog({ mode: 'delete', appointment });
                              } else {
                                setDeleteTarget({ appointment });
                              }
                            }}
                          />
                        </>
                      ) : null}
                      {canWrite && appointment.archived ? (
                        <IconButton
                          variant="action"
                          icon={<Trash2 className="h-4 w-4" aria-hidden />}
                          aria-label={t('common.delete')}
                          onClick={() => {
                            if (appointment.seriesId) {
                              setSeriesDialog({ mode: 'delete', appointment });
                            } else {
                              setDeleteTarget({ appointment });
                            }
                          }}
                        />
                      ) : null}
                    </>
                  ) : undefined
                }
              />
            );
          })}
        </ListContainer>
      </PaginatedListView>

      <CreateAppointmentModal
        isOpen={createOpen}
        onClose={() => setCreateOpen(false)}
        organizationId={organization.id}
        organizationGroups={audienceGroups}
        isLoading={createMutation.isPending}
        onCreate={async (data) => {
          try {
            const created = await createMutation.mutateAsync(data);
            showSuccess(t('appointments.success.created'));
            refetch();
            return created;
          } catch (e) {
            showError(e instanceof Error ? e.message : t('appointments.errors.createFailed'));
            throw e;
          }
        }}
      />

      {editTarget && (
        <EditAppointmentModal
          isOpen={!!editTarget}
          onClose={() => {
            setEditTarget(null);
            setPendingScope(null);
          }}
          organizationId={organization.id}
          appointment={editTarget}
          organizationGroups={audienceGroups}
          scope={pendingScope ?? undefined}
          canManageAttachments={canWrite}
          isLoading={updateMutation.isPending}
          onSave={handleEditSave}
        />
      )}

      {seriesDialog && (
        <SeriesEditDialog
          isOpen
          mode={seriesDialog.mode}
          onClose={() => {
            setSeriesDialog(null);
            setPendingScope(null);
          }}
          onConfirm={(scope) => {
            const { appointment, mode } = seriesDialog;
            setSeriesDialog(null);
            if (mode === 'edit') {
              setPendingScope(scope);
              setEditTarget(appointment);
            } else {
              setDeleteTarget({ appointment, scope });
            }
          }}
        />
      )}

      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDeleteConfirm}
        title={t('appointments.confirm.deleteTitle')}
        message={t('appointments.confirm.delete')}
        confirmText={t('common.delete')}
        isLoading={deleteMutation.isPending}
      />

      {promoteFor ? (
        <CreateEventModal
          isOpen
          onClose={() => setPromoteFor(null)}
          onCreate={handlePromoteCreate}
          onCreated={(created) => {
            const src = promoteFor;
            setPromoteFor(null);
            if (src && canWriteAttendance && src.responseMode !== AppointmentResponseMode.NONE) {
              setImportAfterCreate({
                event: created,
                appointmentId: src.id,
                eventType: eventTypeFromAppointmentType(src.type),
              });
            }
          }}
          eventType={eventTypeFromAppointmentType(promoteFor.type)}
          organizationId={organizationId}
          isLoading={isCreatingEvent}
          initialValues={
            {
              start: formatToLocalDateTime(promoteFor.start),
              end: formatToLocalDateTime(promoteFor.end),
              location: promoteFor.location ?? '',
              description: promoteFor.description ?? '',
              topic: promoteFor.name,
            } satisfies EventModalInitialValues
          }
          instanceKey={`from-apt-${promoteFor.id}`}
          promoteFromAppointmentId={promoteFor.id}
        />
      ) : null}

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
    </>
  );
}
