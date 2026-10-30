/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil, Trash2 } from 'lucide-react';
import { usePathname, useRouter, useSearchParams } from 'next/navigation';
import { startTransition, useEffect, useState } from 'react';
import { EditEventModal } from '@/components/event/EditEventModal';
import { EventAttendanceModal } from '@/components/event/EventAttendanceModal';
import { EventAttendanceSection } from '@/components/event/EventAttendanceSection';
import { EventCertificationGrantSection } from '@/components/event/EventCertificationGrantSection';
import { EventDetailPanel } from '@/components/event/EventDetailPanel';
import { getOperationEventDetailTitle, getServiceEventLabel } from '@/components/event/eventLabels';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useAuth } from '@/lib/auth/AuthContext';
import {
  type DepartmentEventSection,
  getDepartmentSectionPath,
  getEventTypeForDepartmentEventSection,
} from '@/lib/department/sections';
import { eventHasCertificationGrant } from '@/lib/event/certificationGrantConfirm';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { buildEventTopic, useRealtimeTopic } from '@/lib/realtime';
import {
  eventKeys,
  useEventCertificationGrants,
  useOrganizationById,
  useOrganizationEvent,
  useUsersForOrganization,
} from '@/lib/store';
import {
  deleteEvent,
  getReadPermissionForEventType,
  getWritePermissionForEventType,
  updateEvent,
} from '@/lib/utils';
import type { EventDto, UpdateEventPayload } from '@/types/event';
import { EventType } from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';

function getEventTitle(event: EventDto, eventType: EventType): string {
  if (eventType === EventType.SERVICE) {
    return getServiceEventLabel(event);
  }
  if (eventType === EventType.OPERATION) {
    return getOperationEventDetailTitle(event);
  }
  return '-';
}

interface EventDetailViewProps {
  organizationId: number;
  departmentSection: DepartmentEventSection;
  eventId: number;
}

export function EventDetailView({
  organizationId,
  departmentSection,
  eventId,
}: EventDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();
  const { tenantId } = useAuth();

  const eventType = getEventTypeForDepartmentEventSection(departmentSection);
  const eventTopic = tenantId != null ? buildEventTopic(tenantId, eventType, eventId) : null;
  useRealtimeTopic(eventTopic);
  const listPath = getDepartmentSectionPath(organizationId, departmentSection);

  const canRead = hasPermission(organizationId, getReadPermissionForEventType(eventType));
  const canWrite = hasPermission(organizationId, getWritePermissionForEventType(eventType));
  const canReadAttendance = hasPermission(organizationId, PermissionTypeEnum.EVENT_ATTENDANCE_READ);
  const canWriteAttendance = hasPermission(
    organizationId,
    PermissionTypeEnum.EVENT_ATTENDANCE_WRITE,
  );
  const canReadCertGrant =
    canReadAttendance && hasPermission(organizationId, PermissionTypeEnum.CERTIFICATION_READ);
  const canWriteCertGrant =
    canReadAttendance && hasPermission(organizationId, PermissionTypeEnum.CERTIFICATION_WRITE);

  const { data: organization, isLoading: orgLoading } = useOrganizationById(organizationId);
  const { data: users = [] } = useUsersForOrganization(organizationId);

  const {
    data: event,
    isLoading: eventLoading,
    isError: eventError,
  } = useOrganizationEvent(eventType, organizationId, eventId, { enabled: canRead });

  const { data: certGrants = [] } = useEventCertificationGrants(
    organizationId,
    eventType,
    eventId,
    {
      enabled: canReadCertGrant && event != null,
    },
  );
  const deleteAffectsCertGrants = eventHasCertificationGrant(certGrants);

  const [editOpen, setEditOpen] = useState(false);
  const [attendanceOpen, setAttendanceOpen] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);

  useEffect(() => {
    if (searchParams.get('attendance') === '1' && canWriteAttendance) {
      startTransition(() => {
        setAttendanceOpen(true);
        router.replace(pathname, { scroll: false });
      });
    }
  }, [searchParams, pathname, router, canWriteAttendance]);

  useEffect(() => {
    if (!canWriteAttendance) {
      startTransition(() => {
        setAttendanceOpen(false);
      });
    }
  }, [canWriteAttendance]);

  const handleUpdateEvent = async (data: UpdateEventPayload) => {
    if (!event) return;
    try {
      setIsUpdating(true);
      await updateEvent(eventType, organizationId, event.id, data);
      showSuccess(t('events.success.updated'));
      setEditOpen(false);
      await queryClient.invalidateQueries({ queryKey: eventKeys.all });
    } catch {
      showError(t('events.errors.updateFailed'));
    } finally {
      setIsUpdating(false);
    }
  };

  const handleDeleteConfirm = async () => {
    if (!event) return;
    try {
      setIsDeleting(true);
      await deleteEvent(eventType, organizationId, event.id);
      showSuccess(t('events.success.deleted'));
      router.push(listPath);
    } catch {
      showError(t('events.errors.deleteFailed'));
    } finally {
      setIsDeleting(false);
      setDeleteConfirmOpen(false);
    }
  };

  if (orgLoading || eventLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!organization || !canRead) {
    return <p className="text-[var(--text-secondary)]">{t('organization.manage.noPermission')}</p>;
  }

  if (eventError || !event) {
    return (
      <div className="w-full">
        <EmptyStateCard>
          {eventError ? t('events.errors.loadFailed') : t('events.detail.notFound')}
        </EmptyStateCard>
      </div>
    );
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-2"
          title={getEventTitle(event, eventType)}
          actions={
            canWrite ? (
              <>
                <DetailCardAction
                  label={t('common.edit')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setEditOpen(true)}
                />
                <DetailCardAction
                  label={t('common.delete')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => setDeleteConfirmOpen(true)}
                />
              </>
            ) : undefined
          }
        />

        <EventDetailPanel event={event} eventType={eventType} users={users} />
      </Card>

      {canReadAttendance ? (
        <EventAttendanceSection
          organizationId={organizationId}
          eventType={eventType}
          eventId={event.id}
          canWriteAttendance={canWriteAttendance}
          onManage={() => setAttendanceOpen(true)}
        />
      ) : null}

      {canReadCertGrant ? (
        <EventCertificationGrantSection
          organizationId={organizationId}
          eventType={eventType}
          eventId={event.id}
          canRead={canReadCertGrant}
          canWrite={canWriteCertGrant}
        />
      ) : null}

      <EditEventModal
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onUpdate={handleUpdateEvent}
        event={event}
        eventType={eventType}
        organizationId={organizationId}
        isLoading={isUpdating}
      />

      {canWriteAttendance ? (
        <EventAttendanceModal
          isOpen={attendanceOpen}
          onClose={() => setAttendanceOpen(false)}
          organizationId={organizationId}
          eventType={eventType}
          eventId={event.id}
          canReadAttendance={canReadAttendance}
          canWriteAttendance={canWriteAttendance}
          canReadCertGrant={canReadCertGrant}
          eventStart={event.start}
          eventEnd={event.end}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        onConfirm={() => void handleDeleteConfirm()}
        title={t('events.confirm.deleteTitle')}
        message={t('events.confirm.delete')}
        detail={deleteAffectsCertGrants ? t('events.confirm.deleteCertDetail') : undefined}
        confirmText={t('common.delete')}
        isLoading={isDeleting}
      />
    </div>
  );
}
