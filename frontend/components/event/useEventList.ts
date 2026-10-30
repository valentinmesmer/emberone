/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useState } from 'react';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  eventKeys,
  eventListDefinitionForType,
  useOperationEvents,
  useOrganizationById,
  userKeys,
  useServiceEvents,
  useUrlSyncedOperationEventListState,
  useUrlSyncedServiceEventListState,
} from '@/lib/store';
import {
  createEvent,
  deleteEvent,
  getReadPermissionForEventType,
  getWritePermissionForEventType,
  updateEvent,
} from '@/lib/utils';
import type { CreateEventPayload, EventDto, UpdateEventPayload } from '@/types/event';
import { EventType } from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';

export function useEventList(organizationId: number, eventType: EventType) {
  const t = useTranslation();
  const queryClient = useQueryClient();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const { data: organization, isLoading: orgLoading } = useOrganizationById(organizationId);

  const listDefinition = useMemo(() => eventListDefinitionForType(eventType), [eventType]);
  const serviceListState = useUrlSyncedServiceEventListState();
  const operationListState = useUrlSyncedOperationEventListState();
  const listState = eventType === EventType.SERVICE ? serviceListState : operationListState;

  const canRead = hasPermission(organizationId, getReadPermissionForEventType(eventType));
  const canWrite = hasPermission(organizationId, getWritePermissionForEventType(eventType));
  const canReadAttendance = hasPermission(organizationId, PermissionTypeEnum.EVENT_ATTENDANCE_READ);
  const canWriteAttendance = hasPermission(
    organizationId,
    PermissionTypeEnum.EVENT_ATTENDANCE_WRITE,
  );
  const canReadCertGrant =
    canReadAttendance && hasPermission(organizationId, PermissionTypeEnum.CERTIFICATION_READ);

  const serviceQuery = useServiceEvents(eventType === EventType.SERVICE ? organizationId : null, {
    enabled: canRead,
    listQuery: listState.listQuery,
  });
  const operationQuery = useOperationEvents(
    eventType === EventType.OPERATION ? organizationId : null,
    { enabled: canRead, listQuery: listState.listQuery },
  );

  const activeQuery = useMemo(() => {
    switch (eventType) {
      case EventType.SERVICE:
        return serviceQuery;
      case EventType.OPERATION:
        return operationQuery;
      default:
        return null;
    }
  }, [eventType, serviceQuery, operationQuery]);

  useEffect(() => {
    if (activeQuery?.error) {
      showError(
        activeQuery.error instanceof Error
          ? activeQuery.error.message
          : t('events.errors.loadFailed'),
      );
    }
  }, [activeQuery?.error, showError, t]);

  const events: EventDto[] = activeQuery?.data ?? [];
  const eventsLoading = activeQuery?.isPending ?? false;
  const eventsError = activeQuery?.isError ?? false;

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isCreating, setIsCreating] = useState(false);
  const [editingEvent, setEditingEvent] = useState<EventDto | null>(null);
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<EventDto | null>(null);

  const openDeleteConfirm = (event: EventDto) => setDeleteTarget(event);
  const closeDeleteConfirm = () => setDeleteTarget(null);

  const confirmDeleteEvent = async () => {
    if (!deleteTarget) return;
    try {
      setIsDeleting(true);
      await deleteEvent(eventType, organizationId, deleteTarget.id);
      showSuccess(t('events.success.deleted'));
      await queryClient.invalidateQueries({ queryKey: eventKeys.all });
      if (eventType === EventType.OPERATION) {
        await queryClient.invalidateQueries({ queryKey: userKeys.operationAlarmAttendanceStats() });
      }
      setDeleteTarget(null);
    } catch {
      showError(t('events.errors.deleteFailed'));
    } finally {
      setIsDeleting(false);
    }
  };

  const openEditModal = (event: EventDto) => {
    setEditingEvent(event);
    setIsEditModalOpen(true);
  };

  const closeEditModal = () => {
    setIsEditModalOpen(false);
    setEditingEvent(null);
  };

  const handleCreateEvent = async (data: CreateEventPayload): Promise<EventDto> => {
    try {
      setIsCreating(true);
      const created = await createEvent(eventType, data);
      showSuccess(t('events.success.created'));
      await queryClient.invalidateQueries({ queryKey: eventKeys.all });
      if (eventType === EventType.OPERATION) {
        await queryClient.invalidateQueries({ queryKey: userKeys.operationAlarmAttendanceStats() });
      }
      return created;
    } catch {
      showError(t('events.errors.createFailed'));
      throw new Error('create failed');
    } finally {
      setIsCreating(false);
    }
  };

  const handleUpdateEvent = async (data: UpdateEventPayload) => {
    if (!editingEvent) return;
    try {
      setIsUpdating(true);
      await updateEvent(eventType, organizationId, editingEvent.id, data);
      showSuccess(t('events.success.updated'));
      await queryClient.invalidateQueries({ queryKey: eventKeys.all });
      closeEditModal();
    } catch {
      showError(t('events.errors.updateFailed'));
    } finally {
      setIsUpdating(false);
    }
  };

  return {
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
    hasNextPage: activeQuery?.hasNextPage ?? false,
    isFetchingNextPage: activeQuery?.isFetchingNextPage ?? false,
    fetchNextPage: activeQuery?.fetchNextPage,
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
  };
}

export type EventListState = ReturnType<typeof useEventList>;
