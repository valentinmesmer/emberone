/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { Dispatch, SetStateAction } from 'react';
import { OrganizationMemberSelectField } from '@/components/organization/OrganizationMemberSelectField';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import { isEndStrictlyBeforeStart, shiftEndWithStartDateChange } from '@/lib/utils/date';
import type { EventFormState, EventType } from '@/types/event';
import { EventType as EventTypeEnum } from '@/types/event';

interface EventFormFieldsProps {
  eventType: EventType;
  organizationId: number;
  formData: EventFormState;
  setFormData: Dispatch<SetStateAction<EventFormState>>;
  isLoading: boolean;
  /** When true, start/end cannot be changed (e.g. event created from a fixed appointment slot). */
  lockScheduleFields?: boolean;
}

function parseLeaderId(leaderId: EventFormState['leaderId']): number | undefined {
  if (leaderId == null || leaderId === '' || leaderId === '0' || leaderId === 0) {
    return undefined;
  }
  return typeof leaderId === 'string' ? parseInt(leaderId, 10) : leaderId;
}

export function EventFormFields({
  eventType,
  organizationId,
  formData,
  setFormData,
  isLoading,
  lockScheduleFields = false,
}: EventFormFieldsProps) {
  const t = useTranslation();

  const scheduleRangeError =
    formData.start && formData.end && isEndStrictlyBeforeStart(formData.start, formData.end)
      ? t('common.validation.endNotBeforeStart')
      : undefined;

  const renderStartEndPickers = () => (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <DateTimePicker
        label={t('events.fields.start')}
        name="start"
        value={formData.start}
        onChange={(e) => {
          const start = e.target.value;
          setFormData((prev: EventFormState) => ({
            ...prev,
            start,
            ...(prev.end ? { end: shiftEndWithStartDateChange(prev.start, start, prev.end) } : {}),
          }));
        }}
        required
        disabled={isLoading || lockScheduleFields}
      />
      <DateTimePicker
        label={t('events.fields.end')}
        name="end"
        value={formData.end}
        onChange={(e) => setFormData((prev: EventFormState) => ({ ...prev, end: e.target.value }))}
        required
        disabled={isLoading || lockScheduleFields}
        error={scheduleRangeError}
      />
    </div>
  );

  const renderLocationField = () => (
    <Input
      label={t('events.fields.location')}
      name="location"
      value={formData.location}
      onChange={(e) =>
        setFormData((prev: EventFormState) => ({ ...prev, location: e.target.value }))
      }
      placeholder={t('events.fields.locationPlaceholder')}
      required
      disabled={isLoading}
    />
  );

  const renderLeaderField = () => (
    <OrganizationMemberSelectField
      organizationId={organizationId}
      label={t('events.fields.leader')}
      name="leaderId"
      value={parseLeaderId(formData.leaderId)}
      onChange={(leaderId) =>
        setFormData((prev: EventFormState) => ({
          ...prev,
          leaderId,
        }))
      }
      required
      disabled={isLoading}
    />
  );

  const renderDescriptionField = (labelKey: string) => (
    <Textarea
      label={t(labelKey)}
      name="description"
      value={formData.description || ''}
      onChange={(e) =>
        setFormData((prev: EventFormState) => ({ ...prev, description: e.target.value }))
      }
      disabled={isLoading}
      placeholder={t(labelKey)}
      rows={4}
      className="min-h-[110px]"
    />
  );

  const renderServiceFields = () => (
    <>
      {renderStartEndPickers()}
      <Input
        label={t('events.fields.topic')}
        name="topic"
        value={formData.topic || ''}
        onChange={(e) =>
          setFormData((prev: EventFormState) => ({ ...prev, topic: e.target.value }))
        }
        required
        placeholder={t('events.hints.topic')}
        disabled={isLoading}
      />
      {renderLocationField()}
      {renderLeaderField()}
      {renderDescriptionField('events.fields.description')}
    </>
  );

  if (eventType === EventTypeEnum.SERVICE) {
    return <>{renderServiceFields()}</>;
  }

  if (eventType === EventTypeEnum.OPERATION) {
    return (
      <>
        {renderStartEndPickers()}
        <Input
          label={t('events.fields.number')}
          name="number"
          value={formData.number || ''}
          onChange={(e) =>
            setFormData((prev: EventFormState) => ({ ...prev, number: e.target.value }))
          }
          required
          placeholder={t('events.hints.number')}
          disabled={isLoading}
        />
        <Input
          label={t('events.fields.keyword')}
          name="keyword"
          value={formData.keyword || ''}
          onChange={(e) =>
            setFormData((prev: EventFormState) => ({ ...prev, keyword: e.target.value }))
          }
          required
          placeholder={t('events.hints.keyword')}
          disabled={isLoading}
        />
        <Input
          label={t('events.fields.tag')}
          name="tag"
          value={formData.tag || ''}
          onChange={(e) =>
            setFormData((prev: EventFormState) => ({ ...prev, tag: e.target.value }))
          }
          required
          placeholder={t('events.hints.tag')}
          disabled={isLoading}
        />
        <Select
          label={t('events.fields.specialSignal')}
          name="specialSignal"
          value={formData.specialSignal ? 'true' : 'false'}
          onChange={(e) =>
            setFormData((prev: EventFormState) => ({
              ...prev,
              specialSignal: e.target.value === 'true',
            }))
          }
          options={[
            { value: 'false', label: t('common.no') },
            { value: 'true', label: t('common.yes') },
          ]}
          required
          disabled={isLoading}
        />
        {renderLocationField()}
        {renderLeaderField()}
        {renderDescriptionField('events.fields.message')}
      </>
    );
  }

  return null;
}
