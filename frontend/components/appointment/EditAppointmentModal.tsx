/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { buildRecurrenceRequest } from '@/lib/utils/recurrence';
import {
  type AppointmentDto,
  AudienceScope,
  SeriesEditScope,
  type UpdateAppointmentRequest,
} from '@/types/appointment';
import type { OrganizationGroupDto } from '@/types/organizationGroup';
import { AppointmentAttachments } from './AppointmentAttachments';
import type { AppointmentFormState } from './AppointmentFormFields';
import { AppointmentFormFields } from './AppointmentFormFields';
import {
  buildAppointmentPayloadFields,
  formFromAppointment,
  isAppointmentFormValid,
} from './appointmentFormUtils';

interface EditAppointmentModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  appointment: AppointmentDto;
  organizationGroups: OrganizationGroupDto[];
  onSave: (data: UpdateAppointmentRequest, scope?: SeriesEditScope) => Promise<void>;
  scope?: SeriesEditScope;
  canManageAttachments?: boolean;
  isLoading?: boolean;
}

export function EditAppointmentModal({ isOpen, appointment, ...props }: EditAppointmentModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <EditAppointmentModalBody
      key={appointment.id}
      isOpen={isOpen}
      appointment={appointment}
      {...props}
    />
  );
}

function EditAppointmentModalBody({
  isOpen,
  onClose,
  organizationId,
  appointment,
  organizationGroups,
  onSave,
  scope,
  canManageAttachments = false,
  isLoading,
}: EditAppointmentModalProps) {
  const t = useTranslation();
  const [form, setForm] = useState<AppointmentFormState>(() => formFromAppointment(appointment));
  const [initialOccurrenceStart] = useState(() => appointment.start);

  const showSeriesRecurrence = !!appointment.seriesId && scope === SeriesEditScope.ENTIRE_SERIES;

  const isValid = isAppointmentFormValid(form, {
    mode: 'edit',
    originalStart: initialOccurrenceStart,
  });

  const handleSubmit = async () => {
    if (!isValid) return;

    const payload: UpdateAppointmentRequest = {
      name: form.name.trim(),
      type: form.type,
      start: form.start,
      end: form.end,
      ...buildAppointmentPayloadFields(form),
      responseMode: form.responseMode,
      audienceScope: form.audienceScope,
      audienceGroupIds:
        form.audienceScope === AudienceScope.SELECTED_GROUPS ? form.audienceGroupIds : undefined,
      recurrence:
        showSeriesRecurrence && form.recurrence.enabled
          ? buildRecurrenceRequest(form.recurrence, form.start)
          : undefined,
    };
    await onSave(payload, scope);
    onClose();
  };

  if (!isOpen) {
    return null;
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('appointments.edit.title')}
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleSubmit}
          isValid={isValid}
          isLoading={isLoading}
        />
      }
    >
      <div className="space-y-4">
        <AppointmentFormFields
          form={form}
          onChange={setForm}
          organizationGroups={organizationGroups}
          showRecurrence={showSeriesRecurrence}
          futureStartPolicy={{ mode: 'edit', originalStart: initialOccurrenceStart }}
        />
        <AppointmentAttachments
          organizationId={organizationId}
          appointmentId={appointment.id}
          canManage={canManageAttachments}
        />
      </div>
    </Modal>
  );
}
