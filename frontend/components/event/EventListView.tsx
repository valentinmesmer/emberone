/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ClipboardList, Pencil, Trash2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { CreateEventModal } from '@/components/event/CreateEventModal';
import { EditEventModal } from '@/components/event/EditEventModal';
import { EventAttendanceModal } from '@/components/event/EventAttendanceModal';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow, type ListRowTone, ScheduleMetaRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { PaginatedListView, paginatedListEmptyCard } from '@/components/ui/paginated-list';
import { useTranslation } from '@/hooks/useTranslation';
import { useOrganizationMembership } from '@/hooks/useWorkspaceOrganizations';
import type { OrganizationEventListFilterOptionsContext } from '@/lib/api/listFilterOptions';
import { getEventDetailPath, getSectionForEventType } from '@/lib/department/sections';
import { eventHasCertificationGrant } from '@/lib/event/certificationGrantConfirm';
import { useEventCertificationGrants, useMyEventAttendanceForEventIds } from '@/lib/store';
import { formatAppointmentDateTimeRange } from '@/lib/utils/date';
import type { EventDto } from '@/types/event';
import { EventType } from '@/types/event';
import type { EventListState } from './useEventList';

function eventAttendanceTone(attending: boolean | undefined): ListRowTone {
  if (attending === undefined) return 'default';
  return attending ? 'success' : 'danger';
}

interface EventListViewProps {
  title: string;
  getEventLabel: (event: EventDto) => string;
  list: EventListState;
}

export function EventListView({ title, getEventLabel, list }: EventListViewProps) {
  const t = useTranslation();

  const {
    organization,
    orgLoading,
    canRead,
    canWrite,
    canReadAttendance,
    canWriteAttendance,
    canReadCertGrant,
    events,
    eventsLoading,
    eventsError,
    listDefinition,
    listState,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
    eventType,
    organizationId,
    isCreateModalOpen,
    setIsCreateModalOpen,
    isCreating,
    editingEvent,
    isEditModalOpen,
    openEditModal,
    closeEditModal,
    isUpdating,
    isDeleting,
    deleteTarget,
    openDeleteConfirm,
    closeDeleteConfirm,
    confirmDeleteEvent,
    handleCreateEvent,
    handleUpdateEvent,
  } = list;

  const [attendanceModalEvent, setAttendanceModalEvent] = useState<EventDto | null>(null);
  const [attendanceAfterCreate, setAttendanceAfterCreate] = useState(false);

  const { data: deleteTargetCertGrants = [] } = useEventCertificationGrants(
    organizationId,
    eventType,
    deleteTarget?.id ?? 0,
    { enabled: Boolean(deleteTarget && canReadCertGrant) },
  );
  const deleteAffectsCertGrants = eventHasCertificationGrant(deleteTargetCertGrants);

  const departmentSection = useMemo(() => getSectionForEventType(eventType), [eventType]);

  const eventCategory: OrganizationEventListFilterOptionsContext['category'] =
    eventType === EventType.SERVICE ? 'service' : 'operation';
  const organizationEventFilterOptions = useMemo(
    () => ({
      scope: 'organizationEvents' as const,
      organizationId,
      category: eventCategory,
    }),
    [organizationId, eventCategory],
  );

  const eventIds = useMemo(() => events.map((e) => e.id), [events]);
  // Own attendance exists only for members; managers viewing another org get no self request.
  const { isMember } = useOrganizationMembership(organizationId);
  const myAttendanceByEventId = useMyEventAttendanceForEventIds(
    organizationId,
    eventType,
    eventIds,
    {
      enabled: canRead && isMember,
    },
  );

  if (orgLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!organization) {
    return (
      <ErrorState message={t('organization.errors.notFound')} className="mt-4 sm:mt-5 md:mt-6" />
    );
  }

  if (!canRead) {
    return <p className="text-[var(--text-secondary)]">{t('organization.manage.noPermission')}</p>;
  }

  return (
    <>
      <PageHeader
        title={title}
        description={organization.name}
        size="compact"
        action={
          canWrite ? (
            <Button
              variant="primary"
              size="xs"
              onClick={() => setIsCreateModalOpen(true)}
              fullWidth="responsive"
            >
              {t('common.add')}
            </Button>
          ) : null
        }
      />

      <PaginatedListView
        definition={listDefinition}
        state={listState}
        organizationEventFilterOptions={organizationEventFilterOptions}
        totalItems={events.length}
        isPending={eventsLoading}
        error={
          eventsError ? (
            <ErrorState message={t('events.errors.loadFailed')} className="mt-1 sm:mt-0" />
          ) : undefined
        }
        emptyState={paginatedListEmptyCard(t('events.noEvents'))}
        hasNextPage={!!hasNextPage}
        isFetchingNextPage={isFetchingNextPage}
        onLoadMore={() => void fetchNextPage?.()}
        className="mt-1 w-full sm:mt-0"
      >
        <ListContainer label={title}>
          {events.map((event) => (
            <ListRow
              key={event.id}
              href={getEventDetailPath(organizationId, departmentSection, event.id)}
              title={getEventLabel(event)}
              tone={eventAttendanceTone(myAttendanceByEventId.get(event.id))}
              meta={
                <ScheduleMetaRow when={formatAppointmentDateTimeRange(event.start, event.end)} />
              }
              actions={
                canWriteAttendance || canWrite ? (
                  <>
                    {canWriteAttendance ? (
                      <IconButton
                        variant="action"
                        icon={<ClipboardList className="h-4 w-4" aria-hidden />}
                        aria-label={t('events.attendance.edit')}
                        onClick={() => {
                          setAttendanceAfterCreate(false);
                          setAttendanceModalEvent(event);
                        }}
                      />
                    ) : null}
                    {canWrite ? (
                      <>
                        <IconButton
                          variant="action"
                          icon={<Pencil className="h-4 w-4" aria-hidden />}
                          aria-label={t('common.edit')}
                          onClick={() => openEditModal(event)}
                          disabled={isUpdating || isDeleting}
                        />
                        <IconButton
                          variant="action"
                          icon={<Trash2 className="h-4 w-4" aria-hidden />}
                          aria-label={t('common.delete')}
                          onClick={() => openDeleteConfirm(event)}
                          disabled={isUpdating || isDeleting}
                        />
                      </>
                    ) : null}
                  </>
                ) : undefined
              }
            />
          ))}
        </ListContainer>
      </PaginatedListView>

      <CreateEventModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onCreate={handleCreateEvent}
        onCreated={(created) => {
          if (canWriteAttendance) {
            setAttendanceAfterCreate(true);
            setAttendanceModalEvent(created);
          }
        }}
        eventType={eventType}
        organizationId={organizationId}
        isLoading={isCreating}
      />

      {editingEvent && (
        <EditEventModal
          isOpen={isEditModalOpen}
          onClose={closeEditModal}
          onUpdate={handleUpdateEvent}
          event={editingEvent}
          eventType={eventType}
          organizationId={organizationId}
          isLoading={isUpdating}
        />
      )}

      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={closeDeleteConfirm}
        onConfirm={() => void confirmDeleteEvent()}
        title={t('events.confirm.deleteTitle')}
        message={t('events.confirm.delete')}
        detail={deleteAffectsCertGrants ? t('events.confirm.deleteCertDetail') : undefined}
        confirmText={t('common.delete')}
        isLoading={isDeleting}
      />

      {attendanceModalEvent && canWriteAttendance ? (
        <EventAttendanceModal
          isOpen
          afterCreate={attendanceAfterCreate}
          onClose={() => {
            setAttendanceModalEvent(null);
            setAttendanceAfterCreate(false);
          }}
          organizationId={organizationId}
          eventType={eventType}
          eventId={attendanceModalEvent.id}
          canReadAttendance={canReadAttendance}
          canWriteAttendance={canWriteAttendance}
          canReadCertGrant={canReadCertGrant}
          eventStart={attendanceModalEvent.start}
          eventEnd={attendanceModalEvent.end ?? null}
        />
      ) : null}
    </>
  );
}
