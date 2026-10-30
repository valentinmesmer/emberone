/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { formatToLocalDateTime, getCurrentDateTime, getEndDateTime } from '@/lib/utils/date';
import type {
  CreateEventPayload,
  EventDto,
  EventFormState,
  EventModalInitialValues,
  EventType,
} from '@/types/event';
import { EventType as EventTypeEnum } from '@/types/event';
import { EventFormFields } from './EventFormFields';
import { isEventFormValid, validateEventFormForSubmit } from './eventFormUtils';

export interface CreateEventModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateEventPayload) => Promise<EventDto>;
  /** Called after a successful create (modal is closed first). */
  onCreated?: (event: EventDto) => void;
  eventType: EventType;
  organizationId: number;
  isLoading?: boolean;
  /** Pre-filled fields (e.g. when creating an event from a matching appointment). */
  initialValues?: EventModalInitialValues;
  /** Include in the remount key when switching preset without changing org/type. */
  instanceKey?: string;
  /** When creating from a calendar appointment: archives that row server-side after success. */
  promoteFromAppointmentId?: number;
}

function getInitialFormData(eventType: EventType, organizationId: number): EventFormState {
  const currentStart = getCurrentDateTime();
  const currentEnd = getEndDateTime();

  const base = {
    start: currentStart,
    end: currentEnd,
    organizationId,
    location: '',
    description: '',
    leaderId: undefined as number | undefined,
  };

  if (eventType === EventTypeEnum.SERVICE) {
    return { ...base, topic: '' } as EventFormState;
  }

  if (eventType === EventTypeEnum.OPERATION) {
    return {
      ...base,
      number: '',
      keyword: '',
      tag: '',
      specialSignal: false,
    } as EventFormState;
  }

  return base;
}

function mergeInitialForm(
  eventType: EventType,
  organizationId: number,
  initial?: EventModalInitialValues,
): EventFormState {
  const base = getInitialFormData(eventType, organizationId);
  if (!initial) return base;
  return { ...base, ...initial } as EventFormState;
}

interface CreateEventModalFormProps {
  onClose: () => void;
  onCreate: (data: CreateEventPayload) => Promise<EventDto>;
  onCreated?: (event: EventDto) => void;
  eventType: EventType;
  organizationId: number;
  isLoading: boolean;
  initialValues?: EventModalInitialValues;
  promoteFromAppointmentId?: number;
}

function CreateEventModalForm({
  onClose,
  onCreate,
  onCreated,
  eventType,
  organizationId,
  isLoading,
  initialValues,
  promoteFromAppointmentId,
}: CreateEventModalFormProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const [formData, setFormData] = useState<EventFormState>(() =>
    mergeInitialForm(eventType, organizationId, initialValues),
  );

  const handleCreate = async () => {
    const validation = validateEventFormForSubmit(formData, eventType);
    if (validation === 'required') {
      showError(t('common.validation.fillRequired'));
      return;
    }
    if (validation === 'endBeforeStart') {
      showError(t('common.validation.endNotBeforeStart'));
      return;
    }

    const resolvedLocation = formData.location.trim();

    const apiData: Record<string, unknown> = {
      start: formatToLocalDateTime(formData.start),
      organizationId,
    };

    if (formData.end) {
      apiData.end = formatToLocalDateTime(formData.end);
    }
    if (resolvedLocation) apiData.location = resolvedLocation;
    if (formData.description) apiData.description = formData.description;
    if (formData.leaderId && formData.leaderId !== '0' && formData.leaderId !== 0) {
      apiData.leaderId =
        typeof formData.leaderId === 'string' ? parseInt(formData.leaderId, 10) : formData.leaderId;
    }

    if (eventType === EventTypeEnum.SERVICE) {
      if (formData.topic) apiData.topic = formData.topic;
    } else if (eventType === EventTypeEnum.OPERATION) {
      if (formData.number) apiData.number = formData.number;
      if (formData.keyword) apiData.keyword = formData.keyword;
      if (formData.tag) apiData.tag = formData.tag;
      apiData.specialSignal = formData.specialSignal === 'true' || formData.specialSignal === true;
    }

    if (promoteFromAppointmentId != null && eventType !== EventTypeEnum.OPERATION) {
      apiData.promoteFromAppointmentId = promoteFromAppointmentId;
    }

    try {
      const created = await onCreate(apiData as unknown as CreateEventPayload);
      onCreated?.(created);
      onClose();
    } catch (error) {
      console.error('Error creating event:', error);
    }
  };

  const getTitle = () => {
    if (eventType === EventTypeEnum.SERVICE) {
      return t('events.create.service');
    }
    if (eventType === EventTypeEnum.OPERATION) {
      return t('events.create.operation');
    }
    return t('events.create.title');
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={getTitle()}
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleCreate}
          isValid={isEventFormValid(formData, eventType)}
          isLoading={isLoading}
        />
      }
    >
      <div className="space-y-4">
        <EventFormFields
          eventType={eventType}
          organizationId={organizationId}
          formData={formData}
          setFormData={setFormData}
          isLoading={isLoading}
          lockScheduleFields={promoteFromAppointmentId != null}
        />
      </div>
    </Modal>
  );
}

export function CreateEventModal({
  isOpen,
  onClose,
  onCreate,
  onCreated,
  eventType,
  organizationId,
  isLoading = false,
  initialValues,
  instanceKey,
  promoteFromAppointmentId,
}: CreateEventModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <CreateEventModalForm
      key={`${eventType}-${organizationId}-${instanceKey ?? 'default'}`}
      onClose={onClose}
      onCreate={onCreate}
      onCreated={onCreated}
      eventType={eventType}
      organizationId={organizationId}
      isLoading={isLoading}
      initialValues={initialValues}
      promoteFromAppointmentId={promoteFromAppointmentId}
    />
  );
}
