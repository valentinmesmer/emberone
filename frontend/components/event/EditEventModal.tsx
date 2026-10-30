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
import { formatToLocalDateTime } from '@/lib/utils/date';
import type {
  EventDto,
  EventFormState,
  EventType,
  OperationEventDto,
  ServiceEventDto,
  UpdateEventPayload,
} from '@/types/event';
import { EventType as EventTypeEnum } from '@/types/event';
import { EventFormFields } from './EventFormFields';
import { isEventFormValid, validateEventFormForSubmit } from './eventFormUtils';

export interface EditEventModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpdate: (data: UpdateEventPayload) => Promise<void>;
  event: EventDto;
  eventType: EventType;
  organizationId: number;
  isLoading?: boolean;
}

function getInitialFormData(event: EventDto, eventType: EventType): EventFormState {
  const base = {
    start: event.start || '',
    end: event.end || '',
    location: event.location?.trim() ?? '',
    description: event.description || '',
    leaderId: event.leaderId || undefined,
  };

  if (eventType === EventTypeEnum.SERVICE) {
    const serviceEvent = event as ServiceEventDto;
    return { ...base, topic: serviceEvent.topic || '' } as EventFormState;
  }

  if (eventType === EventTypeEnum.OPERATION) {
    const operationEvent = event as OperationEventDto;
    return {
      ...base,
      number: operationEvent.number || '',
      keyword: operationEvent.keyword || '',
      tag: operationEvent.tag || '',
      specialSignal: operationEvent.specialSignal || false,
    } as EventFormState;
  }

  return base;
}

interface EditEventModalFormProps {
  onClose: () => void;
  onUpdate: (data: UpdateEventPayload) => Promise<void>;
  event: EventDto;
  eventType: EventType;
  organizationId: number;
  isLoading: boolean;
}

function EditEventModalForm({
  onClose,
  onUpdate,
  event,
  eventType,
  organizationId,
  isLoading,
}: EditEventModalFormProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const [formData, setFormData] = useState<EventFormState>(() =>
    getInitialFormData(event, eventType),
  );

  const handleUpdate = async () => {
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

    try {
      await onUpdate(apiData as unknown as UpdateEventPayload);
      onClose();
    } catch (error) {
      console.error('Error updating event:', error);
    }
  };

  const getTitle = () => {
    if (eventType === EventTypeEnum.SERVICE) {
      return t('events.edit.service');
    }
    if (eventType === EventTypeEnum.OPERATION) {
      return t('events.edit.operation');
    }
    return t('events.edit.title');
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
          onSave={handleUpdate}
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
        />
      </div>
    </Modal>
  );
}

export function EditEventModal({
  isOpen,
  onClose,
  onUpdate,
  event,
  eventType,
  organizationId,
  isLoading = false,
}: EditEventModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <EditEventModalForm
      key={event.id}
      onClose={onClose}
      onUpdate={onUpdate}
      event={event}
      eventType={eventType}
      organizationId={organizationId}
      isLoading={isLoading}
    />
  );
}
