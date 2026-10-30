/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { appointmentAttachmentApi } from '@/lib/api/appointmentAttachment';
import { appointmentAttachmentKeys, appointmentKeys } from '@/lib/store';
import { buildRecurrenceRequest } from '@/lib/utils/recurrence';
import { AudienceScope, type CreateAppointmentRequest } from '@/types/appointment';
import type { OrganizationGroupDto } from '@/types/organizationGroup';
import { AppointmentAttachments } from './AppointmentAttachments';
import type { AppointmentFormState } from './AppointmentFormFields';
import { AppointmentFormFields } from './AppointmentFormFields';
import { AppointmentPendingAttachments } from './AppointmentPendingAttachments';
import {
  buildAppointmentPayloadFields,
  getDefaultAppointmentForm,
  isAppointmentFormValid,
} from './appointmentFormUtils';

interface CreateAppointmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  organizationGroups: OrganizationGroupDto[];
  onCreate: (data: CreateAppointmentRequest) => Promise<{ id: number }>;
  isLoading?: boolean;
}

export function CreateAppointmentModal({
  isOpen,
  onClose,
  organizationId,
  organizationGroups,
  onCreate,
  isLoading,
}: CreateAppointmentModalProps) {
  const t = useTranslation();
  const { showError } = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<AppointmentFormState>(() => getDefaultAppointmentForm());
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);
  const [createdAppointmentId, setCreatedAppointmentId] = useState<number | null>(null);
  const [isUploading, setIsUploading] = useState(false);

  const isValid = isAppointmentFormValid(form, { mode: 'create' });
  const isAttachmentRetryStep = createdAppointmentId != null;

  const resetAndClose = () => {
    setForm(getDefaultAppointmentForm());
    setPendingFiles([]);
    setCreatedAppointmentId(null);
    setIsUploading(false);
    onClose();
  };

  const uploadPendingFiles = async (appointmentId: number, files: File[]) => {
    if (files.length === 0) return;
    for (const file of files) {
      await appointmentAttachmentApi.upload(organizationId, appointmentId, file);
    }
    await queryClient.invalidateQueries({
      queryKey: appointmentAttachmentKeys.list(organizationId, appointmentId),
    });
    await queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
  };

  const handleSubmit = async () => {
    if (!isValid || isAttachmentRetryStep) return;

    const payload: CreateAppointmentRequest = {
      organizationId,
      name: form.name.trim(),
      type: form.type,
      start: form.start,
      end: form.end,
      ...buildAppointmentPayloadFields(form),
      responseMode: form.responseMode,
      audienceScope: form.audienceScope,
      audienceGroupIds:
        form.audienceScope === AudienceScope.SELECTED_GROUPS ? form.audienceGroupIds : undefined,
      recurrence: form.recurrence.enabled
        ? buildRecurrenceRequest(form.recurrence, form.start)
        : undefined,
    };

    try {
      const created = await onCreate(payload);
      if (pendingFiles.length === 0) {
        resetAndClose();
        return;
      }

      setIsUploading(true);
      try {
        await uploadPendingFiles(created.id, pendingFiles);
        resetAndClose();
      } catch {
        showError(t('appointments.errors.uploadFailed'));
        setCreatedAppointmentId(created.id);
        setPendingFiles([]);
      } finally {
        setIsUploading(false);
      }
    } catch {
      // create errors are handled by the parent
    }
  };

  if (!isOpen) {
    return null;
  }

  const isBusy = isLoading || isUploading;

  return (
    <Modal
      isOpen
      onClose={resetAndClose}
      title={t('appointments.create.title')}
      size="lg"
      footer={
        isAttachmentRetryStep ? (
          <ModalFooter onCancel={resetAndClose} showSave={false} cancelText={t('common.close')} />
        ) : (
          <ModalFooter
            onCancel={resetAndClose}
            onSave={handleSubmit}
            isValid={isValid}
            isLoading={isBusy}
          />
        )
      }
    >
      <div className="space-y-4">
        {!isAttachmentRetryStep ? (
          <>
            <AppointmentFormFields
              form={form}
              onChange={setForm}
              organizationGroups={organizationGroups}
              futureStartPolicy={{ mode: 'create' }}
            />
            <AppointmentPendingAttachments files={pendingFiles} onChange={setPendingFiles} />
          </>
        ) : (
          <>
            <p className="text-sm text-[var(--text-secondary)]">
              {t('appointments.attachments.uploadRetryHint')}
            </p>
            <AppointmentAttachments
              organizationId={organizationId}
              appointmentId={createdAppointmentId}
              canManage
            />
          </>
        )}
      </div>
    </Modal>
  );
}
