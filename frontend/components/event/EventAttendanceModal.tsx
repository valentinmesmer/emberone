/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Download, Minus, Plus } from 'lucide-react';
import { startTransition, useCallback, useEffect, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Input } from '@/components/ui/input';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { SectionActions } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { eventApi } from '@/lib/api/event';
import { eventHasCertificationGrant } from '@/lib/event/certificationGrantConfirm';
import { importAppointmentYesResponsesAsEventAttendance } from '@/lib/event/importAppointmentAttendance';
import {
  invalidateAfterEventAttendanceChange,
  useAppointments,
  useEventAttendances,
  useEventCertificationGrants,
  useUsersForOrganization,
} from '@/lib/store';
import { formatAppointmentDateTimeRange } from '@/lib/utils';
import { findAppointmentsMatchingEventSchedule } from '@/lib/utils/appointmentEvent';
import { cn } from '@/lib/utils/cn';
import { eventTypeToUrlSegment } from '@/lib/utils/event';
import type { AppointmentDto } from '@/types/appointment';
import type { EventAttendanceDto, EventType } from '@/types/event';
import { EventType as EventTypeEnum } from '@/types/event';

export interface EventAttendanceModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  eventType: EventType;
  eventId: number;
  canReadAttendance: boolean;
  canWriteAttendance: boolean;
  /** When true, attendance removal can warn about linked certification records. */
  canReadCertGrant?: boolean;
  /** Used with {@link eventEnd} to find a unique matching appointment for import (non-operation only). */
  eventStart?: string;
  eventEnd?: string | null;
  /** Right after creating an event - friendlier title than "bearbeiten". */
  afterCreate?: boolean;
}

function matchesSearchMember(firstName: string, lastName: string, q: string): boolean {
  if (!q.trim()) return true;
  const n = `${firstName} ${lastName}`.toLowerCase();
  return n.includes(q.trim().toLowerCase());
}

export function EventAttendanceModal({
  isOpen,
  onClose,
  organizationId,
  eventType,
  eventId,
  canReadAttendance,
  canWriteAttendance,
  canReadCertGrant = false,
  eventStart,
  eventEnd,
  afterCreate = false,
}: EventAttendanceModalProps) {
  const t = useTranslation();
  const queryClient = useQueryClient();
  const { showError, showSuccess } = useToast();
  const category = eventTypeToUrlSegment(eventType);

  const [search, setSearch] = useState('');
  const [pendingUserIds, setPendingUserIds] = useState<Set<number>>(() => new Set());
  const [bulkWorking, setBulkWorking] = useState(false);
  const [importAptConfirm, setImportAptConfirm] = useState<AppointmentDto | null>(null);
  const [aptImportWorking, setAptImportWorking] = useState(false);
  const [removeConfirm, setRemoveConfirm] = useState<
    { kind: 'single'; userId: number } | { kind: 'bulk'; attendanceIds: number[] } | null
  >(null);
  const [removeConfirmWorking, setRemoveConfirmWorking] = useState(false);

  const { data: certGrants = [] } = useEventCertificationGrants(
    organizationId,
    eventType,
    eventId,
    {
      enabled: isOpen && canReadCertGrant,
    },
  );
  const removalAffectsCertGrants = eventHasCertificationGrant(certGrants);

  const { data: rows = [], isLoading } = useEventAttendances(organizationId, eventType, eventId, {
    enabled: isOpen && (canReadAttendance || canWriteAttendance),
  });
  const { data: users = [], isLoading: usersLoading } = useUsersForOrganization(
    isOpen ? organizationId : null,
  );

  const allowAppointmentImport = eventType !== EventTypeEnum.OPERATION && Boolean(eventStart);

  const { data: appointments = [], isLoading: appointmentsLoading } = useAppointments(
    isOpen && allowAppointmentImport && canWriteAttendance ? organizationId : null,
  );

  const matchingAppointments = useMemo(() => {
    if (!allowAppointmentImport || !eventStart) return [];
    return findAppointmentsMatchingEventSchedule(appointments, {
      eventType,
      start: eventStart,
      end: eventEnd,
    });
  }, [allowAppointmentImport, appointments, eventEnd, eventStart, eventType]);

  const singleAppointmentMatch = matchingAppointments.length === 1 ? matchingAppointments[0] : null;

  useEffect(() => {
    if (!isOpen) return;
    startTransition(() => {
      setSearch('');
      setPendingUserIds(new Set());
      setImportAptConfirm(null);
      setRemoveConfirm(null);
    });
  }, [isOpen]);

  const attendanceByUserId = useMemo(() => {
    const m = new Map<number, EventAttendanceDto>();
    for (const r of rows) {
      m.set(r.userId, r);
    }
    return m;
  }, [rows]);

  const filteredMembers = useMemo(() => {
    return users.filter((u) => matchesSearchMember(u.firstName, u.lastName, search));
  }, [users, search]);

  const presentCount = rows.length;
  const memberCount = users.length;

  const invalidateList = useCallback(() => {
    invalidateAfterEventAttendanceChange(queryClient, organizationId, eventType, eventId);
  }, [queryClient, organizationId, eventType, eventId]);

  const setPending = useCallback((userId: number, on: boolean) => {
    setPendingUserIds((prev) => {
      const next = new Set(prev);
      if (on) next.add(userId);
      else next.delete(userId);
      return next;
    });
  }, []);

  const deleteAttendanceRows = async (attendanceIds: number[]) => {
    for (const id of attendanceIds) {
      await eventApi.deleteEventAttendance(organizationId, category, eventId, id);
    }
    await invalidateList();
  };

  const runConfirmedAttendanceRemoval = async () => {
    if (!removeConfirm || !canWriteAttendance) return;
    setRemoveConfirmWorking(true);
    const singleUserId = removeConfirm.kind === 'single' ? removeConfirm.userId : null;
    if (singleUserId != null) {
      setPending(singleUserId, true);
    }
    try {
      if (removeConfirm.kind === 'single') {
        const row = attendanceByUserId.get(removeConfirm.userId);
        if (row) {
          await deleteAttendanceRows([row.id]);
        }
      } else {
        await deleteAttendanceRows(removeConfirm.attendanceIds);
        showSuccess(
          t('events.attendance.bulkRemoved', { count: removeConfirm.attendanceIds.length }),
        );
      }
      setRemoveConfirm(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('events.attendance.removeFailed'));
      await invalidateList();
    } finally {
      if (singleUserId != null) {
        setPending(singleUserId, false);
      }
      setRemoveConfirmWorking(false);
    }
  };

  const requestAttendanceRemoval = (
    payload:
      | { kind: 'single'; userId: number }
      | {
          kind: 'bulk';
          attendanceIds: number[];
        },
  ) => {
    if (!canWriteAttendance || bulkWorking) return;
    if (payload.kind === 'bulk' && payload.attendanceIds.length === 0) return;
    if (removalAffectsCertGrants) {
      setRemoveConfirm(payload);
      return;
    }
    void (async () => {
      if (payload.kind === 'single') {
        setPending(payload.userId, true);
        try {
          const row = attendanceByUserId.get(payload.userId);
          if (row) {
            await deleteAttendanceRows([row.id]);
          }
        } catch (e) {
          showError(e instanceof Error ? e.message : t('events.attendance.removeFailed'));
          await invalidateList();
        } finally {
          setPending(payload.userId, false);
        }
      } else {
        setBulkWorking(true);
        try {
          await deleteAttendanceRows(payload.attendanceIds);
          showSuccess(t('events.attendance.bulkRemoved', { count: payload.attendanceIds.length }));
        } catch (e) {
          showError(e instanceof Error ? e.message : t('events.attendance.removeFailed'));
          await invalidateList();
        } finally {
          setBulkWorking(false);
        }
      }
    })();
  };

  const handleTogglePresence = async (userId: number, nextPresent: boolean) => {
    if (!canWriteAttendance || pendingUserIds.has(userId) || bulkWorking || removeConfirmWorking) {
      return;
    }
    if (nextPresent) {
      setPending(userId, true);
      try {
        await eventApi.createEventAttendance(organizationId, category, eventId, { userId });
        await invalidateList();
      } catch (e) {
        showError(e instanceof Error ? e.message : t('events.attendance.addFailed'));
        await invalidateList();
      } finally {
        setPending(userId, false);
      }
      return;
    }
    requestAttendanceRemoval({ kind: 'single', userId });
  };

  const handleMarkFilteredPresent = async () => {
    if (!canWriteAttendance || bulkWorking) return;
    const toAdd = filteredMembers.filter((u) => !attendanceByUserId.has(u.id)).map((u) => u.id);
    if (toAdd.length === 0) return;
    setBulkWorking(true);
    try {
      for (const userId of toAdd) {
        await eventApi.createEventAttendance(organizationId, category, eventId, { userId });
      }
      await invalidateList();
      showSuccess(t('events.attendance.bulkAdded', { count: toAdd.length }));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('events.attendance.addFailed'));
      await invalidateList();
    } finally {
      setBulkWorking(false);
    }
  };

  const handleMarkFilteredAbsent = () => {
    if (!canWriteAttendance || bulkWorking || removeConfirmWorking) return;
    const toRemove = filteredMembers.flatMap((u) => {
      const attendance = attendanceByUserId.get(u.id);
      return attendance ? [attendance] : [];
    });
    if (toRemove.length === 0) return;
    requestAttendanceRemoval({
      kind: 'bulk',
      attendanceIds: toRemove.map((row) => row.id),
    });
  };

  const runImportFromAppointment = async () => {
    if (!importAptConfirm || !canWriteAttendance) return;
    try {
      setAptImportWorking(true);
      const n = await importAppointmentYesResponsesAsEventAttendance(
        organizationId,
        eventType,
        eventId,
        importAptConfirm.id,
      );
      if (n > 0) {
        showSuccess(t('events.attendance.importFromAppointmentSuccess', { count: n }));
      } else {
        showSuccess(t('events.attendance.importFromAppointmentNone'));
      }
      setImportAptConfirm(null);
      await invalidateList();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('events.attendance.addFailed'));
      await invalidateList();
    } finally {
      setAptImportWorking(false);
    }
  };

  if (!isOpen) return null;

  const appointmentsBusy = allowAppointmentImport && appointmentsLoading;
  const busy =
    bulkWorking ||
    removeConfirmWorking ||
    isLoading ||
    usersLoading ||
    appointmentsBusy ||
    aptImportWorking;
  const listScrollClass =
    'max-h-[min(32rem,52vh)] overflow-y-auto rounded-lg border border-[var(--border-color)] p-2';

  const filteredToAddCount = filteredMembers.filter((u) => !attendanceByUserId.has(u.id)).length;
  const filteredToRemoveCount = filteredMembers.filter((u) => attendanceByUserId.has(u.id)).length;

  return (
    <>
      <Modal
        isOpen={isOpen}
        onClose={onClose}
        title={afterCreate ? t('events.attendance.record') : t('events.attendance.edit')}
        size="2xl"
        className="flex flex-col sm:max-h-[min(92vh,900px)]"
        footer={
          <ModalFooter
            onCancel={onClose}
            isLoading={busy}
            showSave={false}
            cancelText={t('common.close')}
          />
        }
      >
        {!canWriteAttendance ? (
          <div className="space-y-3">
            <p className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
              {t('events.attendance.readOnlyBadge')}
            </p>
            <p className="text-sm font-medium text-[var(--text-primary)]">
              {t('events.attendance.listSummary', { present: presentCount, total: memberCount })}
            </p>
            <Input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t('events.attendance.searchMembers')}
              aria-label={t('events.attendance.searchMembers')}
              disabled={busy}
              autoComplete="off"
              className="rounded-lg px-3 py-2 text-sm"
            />
            {isLoading || usersLoading ? (
              <LoadingState variant="embedded" size="sm" />
            ) : users.length === 0 ? (
              <EmptyState>{t('events.attendance.noMembers')}</EmptyState>
            ) : (
              <div className={cn(listScrollClass, 'space-y-0.5')}>
                {filteredMembers.length === 0 ? (
                  <EmptyState centered className="px-2 py-6">
                    {t('events.attendance.noSearchHits')}
                  </EmptyState>
                ) : (
                  filteredMembers.map((u) => {
                    const present = attendanceByUserId.has(u.id);
                    return (
                      <Checkbox
                        key={u.id}
                        id={`att-ro-${u.id}`}
                        checked={present}
                        onCheckedChange={() => {}}
                        label={`${u.lastName}, ${u.firstName}`}
                        disabled
                        className="rounded-md px-1 py-0.5 opacity-90"
                      />
                    );
                  })
                )}
              </div>
            )}
          </div>
        ) : (
          <div className="flex min-h-0 flex-col gap-3">
            <p className="text-sm font-medium text-[var(--text-primary)]">
              {t('events.attendance.listSummary', { present: presentCount, total: memberCount })}
            </p>
            <Input
              type="search"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder={t('events.attendance.searchMembers')}
              aria-label={t('events.attendance.searchMembers')}
              disabled={busy}
              autoComplete="off"
              className="rounded-lg px-3 py-2 text-sm"
            />
            <SectionActions>
              <Button
                type="button"
                size="xs"
                variant="secondary"
                fullWidth="responsive"
                disabled={busy || filteredToAddCount === 0}
                onClick={() => void handleMarkFilteredPresent()}
                icon={<Plus className="h-3.5 w-3.5 shrink-0" aria-hidden />}
              >
                {t('common.addCount', { count: filteredToAddCount })}
              </Button>
              <Button
                type="button"
                size="xs"
                variant="outline"
                fullWidth="responsive"
                className="border-red-500/60 text-red-600 hover:bg-red-500/10"
                disabled={busy || filteredToRemoveCount === 0}
                onClick={() => void handleMarkFilteredAbsent()}
                icon={<Minus className="h-3.5 w-3.5 shrink-0" aria-hidden />}
              >
                {t('common.removeCount', { count: filteredToRemoveCount })}
              </Button>
              {allowAppointmentImport ? (
                <Button
                  type="button"
                  size="xs"
                  variant="outline"
                  fullWidth="responsive"
                  disabled={busy || !singleAppointmentMatch}
                  title={
                    singleAppointmentMatch
                      ? undefined
                      : t('events.fromAppointment.importFromAppointmentHint')
                  }
                  onClick={() => {
                    if (singleAppointmentMatch) setImportAptConfirm(singleAppointmentMatch);
                  }}
                  icon={<Download className="h-3.5 w-3.5 shrink-0" aria-hidden />}
                >
                  {t('events.fromAppointment.importFromAppointment')}
                </Button>
              ) : null}
            </SectionActions>
            {isLoading || usersLoading ? (
              <LoadingState variant="embedded" size="sm" />
            ) : users.length === 0 ? (
              <EmptyState>{t('events.attendance.noMembers')}</EmptyState>
            ) : (
              <div className={cn(listScrollClass, 'space-y-0.5')}>
                {filteredMembers.length === 0 ? (
                  <EmptyState centered className="px-2 py-6">
                    {t('events.attendance.noSearchHits')}
                  </EmptyState>
                ) : (
                  filteredMembers.map((u) => {
                    const present = attendanceByUserId.has(u.id);
                    const rowBusy = pendingUserIds.has(u.id);
                    return (
                      <Checkbox
                        key={u.id}
                        id={`att-member-${u.id}`}
                        checked={present}
                        onCheckedChange={(c) => void handleTogglePresence(u.id, c)}
                        label={`${u.lastName}, ${u.firstName}`}
                        disabled={busy || rowBusy}
                        className={cn(
                          'rounded-md px-1 py-0.5 hover:bg-[var(--hover-bg)]/50',
                          rowBusy && 'opacity-60',
                        )}
                      />
                    );
                  })
                )}
              </div>
            )}
          </div>
        )}
      </Modal>

      <ConfirmDialog
        variant="warning"
        isOpen={removeConfirm != null}
        onClose={() => !removeConfirmWorking && setRemoveConfirm(null)}
        onConfirm={() => void runConfirmedAttendanceRemoval()}
        title={
          removeConfirm?.kind === 'bulk'
            ? t('events.confirm.removeAttendanceBulkTitle')
            : t('events.confirm.removeAttendanceTitle')
        }
        message={
          removeConfirm?.kind === 'bulk'
            ? t('events.confirm.removeAttendanceBulkMessage', {
                count: removeConfirm.attendanceIds.length,
              })
            : (() => {
                const user =
                  removeConfirm?.kind === 'single'
                    ? users.find((u) => u.id === removeConfirm.userId)
                    : undefined;
                if (!user) {
                  return t('events.confirm.removeAttendanceMessageAnonymous');
                }
                return t('events.confirm.removeAttendanceMessage', {
                  name: `${user.lastName}, ${user.firstName}`,
                });
              })()
        }
        detail={
          removalAffectsCertGrants ? t('events.confirm.removeAttendanceCertDetail') : undefined
        }
        confirmText={t('common.confirm')}
        isLoading={removeConfirmWorking}
      />

      <ConfirmDialog
        variant="info"
        isOpen={!!importAptConfirm}
        onClose={() => !aptImportWorking && setImportAptConfirm(null)}
        onConfirm={() => void runImportFromAppointment()}
        title={t('events.fromAppointment.confirmMatchingTitle')}
        message={
          importAptConfirm
            ? t('events.fromAppointment.confirmMatchingMessage', {
                name: importAptConfirm.name,
                when: formatAppointmentDateTimeRange(importAptConfirm.start, importAptConfirm.end),
              })
            : ''
        }
        confirmText={t('events.fromAppointment.importResponsesConfirm')}
        cancelText={t('common.cancel')}
        isLoading={aptImportWorking}
      />
    </>
  );
}
