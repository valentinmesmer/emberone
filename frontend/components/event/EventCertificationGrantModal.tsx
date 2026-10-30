/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Checkbox } from '@/components/ui/checkbox';
import { LoadingState } from '@/components/ui/loading';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationDefinitionDto } from '@/lib/api/certifications';
import type { CertGrantRecipientMode } from '@/lib/api/eventCertificationGrants';
import {
  useCreateEventCertificationGrant,
  useEventAttendances,
  useEventCertificationGrantDetail,
  useUpdateEventCertificationGrant,
} from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import type { EventType } from '@/types/event';

export interface EventCertificationGrantModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  eventType: EventType;
  eventId: number;
  /** Available definitions for create. Ignored when editing. */
  eventDefinitions: CertificationDefinitionDto[];
  grantId?: number | null;
}

export function EventCertificationGrantModal({
  isOpen,
  onClose,
  organizationId,
  eventType,
  eventId,
  eventDefinitions,
  grantId = null,
}: EventCertificationGrantModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const isEdit = grantId != null && grantId > 0;

  const { data: grantDetail, isLoading: grantLoading } = useEventCertificationGrantDetail(
    organizationId,
    eventType,
    eventId,
    grantId,
    { enabled: isOpen && isEdit },
  );
  const { data: attendances = [] } = useEventAttendances(organizationId, eventType, eventId, {
    enabled: isOpen,
  });
  const createGrant = useCreateEventCertificationGrant(organizationId, eventType, eventId);
  const updateGrant = useUpdateEventCertificationGrant(organizationId, eventType, eventId);
  const saving = createGrant.isPending || updateGrant.isPending;

  const [definitionId, setDefinitionId] = useState<number | ''>('');
  const [recipientMode, setRecipientMode] = useState<CertGrantRecipientMode>('ALL_ATTENDEES');
  const [notes, setNotes] = useState('');
  const [selectedUserIds, setSelectedUserIds] = useState<Set<number>>(new Set());
  const [prevIsOpen, setPrevIsOpen] = useState(isOpen);
  const [prevGrantId, setPrevGrantId] = useState(grantId);
  const [hydrated, setHydrated] = useState(false);

  if (isOpen !== prevIsOpen || grantId !== prevGrantId) {
    setPrevIsOpen(isOpen);
    setPrevGrantId(grantId);
    setHydrated(false);
  }

  // Create: hydrate even when the modal mounts already open (conditional render).
  if (isOpen && !isEdit && !hydrated) {
    const first = eventDefinitions[0];
    setDefinitionId(first ? first.id : '');
    setRecipientMode('ALL_ATTENDEES');
    setNotes('');
    setSelectedUserIds(new Set());
    setHydrated(true);
  }

  if (isOpen && isEdit && grantDetail && !hydrated) {
    setRecipientMode(grantDetail.grant.recipientMode);
    setNotes(grantDetail.grant.notes ?? '');
    const specificIds = grantDetail.recipients.filter((r) => r.inScope).map((r) => r.userId);
    setSelectedUserIds(new Set(specificIds));
    setHydrated(true);
  }

  const toggleUser = (userId: number) => {
    setSelectedUserIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) {
        next.delete(userId);
      } else {
        next.add(userId);
      }
      return next;
    });
  };

  const canSave =
    hydrated &&
    (isEdit || typeof definitionId === 'number') &&
    (recipientMode !== 'SPECIFIC' || selectedUserIds.size > 0) &&
    !saving;

  const handleSave = async () => {
    if (!canSave) return;
    const payload = {
      recipientMode,
      notes: notes.trim() || undefined,
      recipientUserIds: recipientMode === 'SPECIFIC' ? Array.from(selectedUserIds) : undefined,
    };
    try {
      if (isEdit && grantId != null) {
        await updateGrant.mutateAsync({ grantId, data: payload });
      } else if (typeof definitionId === 'number') {
        await createGrant.mutateAsync({
          tenantCertDefinitionId: definitionId,
          ...payload,
        });
      } else {
        return;
      }
      showSuccess(t('events.certificationGrant.saveSuccess'));
      onClose();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('events.certificationGrant.saveError'));
    }
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        isEdit
          ? t('events.certificationGrant.editTitle')
          : t('events.certificationGrant.createTitle')
      }
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={canSave}
          isLoading={saving}
          showSave
        />
      }
    >
      {isEdit && (grantLoading || !hydrated) ? (
        <LoadingState variant="embedded" size="sm" />
      ) : (
        <div className="space-y-4">
          {isEdit ? (
            <div>
              <p className="mb-1 text-sm font-medium text-[var(--text-secondary)]">
                {t('events.certificationGrant.definitionLabel')}
              </p>
              <p className="text-sm font-semibold text-[var(--text-primary)]">
                {grantDetail?.grant.certDefinitionName ??
                  `#${grantDetail?.grant.tenantCertDefinitionId ?? ''}`}
              </p>
            </div>
          ) : (
            <Select
              label={t('events.certificationGrant.definitionLabel')}
              value={typeof definitionId === 'number' ? String(definitionId) : ''}
              options={eventDefinitions.map((def) => ({
                value: String(def.id),
                label: def.name,
              }))}
              onChange={(e) =>
                setDefinitionId(e.target.value ? Number.parseInt(e.target.value, 10) : '')
              }
              required
            />
          )}

          <Select
            label={t('events.certificationGrant.recipientModeLabel')}
            value={recipientMode}
            options={[
              { value: 'ALL_ATTENDEES', label: t('events.certificationGrant.allAttendees') },
              { value: 'SPECIFIC', label: t('events.certificationGrant.specificAttendees') },
            ]}
            onChange={(e) => setRecipientMode(e.target.value as CertGrantRecipientMode)}
            required
          />

          {recipientMode === 'SPECIFIC' ? (
            <div className="space-y-2">
              <p className="text-sm font-semibold text-[var(--text-primary)]">
                {t('events.certificationGrant.selectRecipients')}
                <span className="text-[var(--fire-red)] ml-1">*</span>
              </p>
              {attendances.length === 0 ? (
                <p className="text-xs text-[var(--text-secondary)]">
                  {t('events.certificationGrant.noAttendees')}
                </p>
              ) : (
                <div className="max-h-[min(20rem,40vh)] space-y-0.5 overflow-y-auto rounded-lg border border-[var(--border-color)] p-2">
                  {attendances.map((row) => (
                    <Checkbox
                      key={row.userId}
                      id={`cert-grant-recipient-${row.userId}`}
                      checked={selectedUserIds.has(row.userId)}
                      onCheckedChange={() => toggleUser(row.userId)}
                      label={`${row.lastName}, ${row.firstName}`}
                      className={cn('rounded-md px-1 py-0.5 hover:bg-[var(--hover-bg)]/50')}
                    />
                  ))}
                </div>
              )}
            </div>
          ) : null}

          <Textarea
            label={t('events.certificationGrant.notesLabel')}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>
      )}
    </Modal>
  );
}
