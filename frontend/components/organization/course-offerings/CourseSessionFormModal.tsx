/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { DateTimePicker } from '@/components/ui/date-time-picker';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { validateRequiredFields } from '@/lib/utils';
import { courseSessionTypeLabelKey } from '@/lib/utils/courseOffering';
import { isEndStrictlyBeforeStart, syncEndLocalDateTimeWithStartChange } from '@/lib/utils/date';
import type {
  CourseSessionDto,
  CourseSessionRequest,
  CourseSessionType,
} from '@/types/courseOffering';

type SessionForm = {
  type: CourseSessionType;
  start: string;
  end: string;
  location: string;
  notes: string;
};

function emptyForm(): SessionForm {
  return { type: 'THEORY', start: '', end: '', location: '', notes: '' };
}

function formFromSession(session: CourseSessionDto): SessionForm {
  return {
    type: session.type,
    start: session.start,
    end: session.end,
    location: session.location ?? '',
    notes: session.notes ?? '',
  };
}

export interface CourseSessionFormModalProps {
  session?: CourseSessionDto | null;
  isSaving: boolean;
  onClose: () => void;
  onSave: (payload: CourseSessionRequest) => Promise<void>;
}

export function CourseSessionFormModal({
  session,
  isSaving,
  onClose,
  onSave,
}: CourseSessionFormModalProps) {
  const t = useTranslation();
  const { showError } = useToast();
  const [form, setForm] = useState<SessionForm>(() =>
    session ? formFromSession(session) : emptyForm(),
  );

  const scheduleError =
    form.start && form.end && isEndStrictlyBeforeStart(form.start, form.end)
      ? t('common.validation.endNotBeforeStart')
      : undefined;

  const isValid = form.start !== '' && form.end !== '' && !scheduleError;

  const save = async () => {
    if (
      !validateRequiredFields(
        { start: form.start, end: form.end },
        t,
        showError,
        'common.validation.fillRequired',
      )
    ) {
      return;
    }
    if (!isValid) return;
    await onSave({
      type: form.type,
      start: form.start,
      end: form.end,
      location: form.location.trim() || null,
      notes: form.notes.trim() || null,
    });
  };

  const typeOptions = (['THEORY', 'PRACTICE', 'EXAM'] as const).map((type) => ({
    value: type,
    label: t(courseSessionTypeLabelKey(type)),
  }));

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        session
          ? t('courseOfferings.sessions.editTitle')
          : t('courseOfferings.sessions.createTitle')
      }
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void save()}
          isValid={isValid}
          isLoading={isSaving}
        />
      }
    >
      <div className="space-y-4">
        <Select
          label={t('courseOfferings.sessions.type')}
          value={form.type}
          onChange={(e) =>
            setForm((prev) => ({ ...prev, type: e.target.value as CourseSessionType }))
          }
          options={typeOptions}
          required
        />
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
          <DateTimePicker
            label={t('courseOfferings.sessions.start')}
            name="sessionStart"
            value={form.start}
            onChange={(e) => {
              const start = e.target.value;
              setForm((prev) => ({
                ...prev,
                start,
                end: syncEndLocalDateTimeWithStartChange(prev.start, start, prev.end),
              }));
            }}
            required
          />
          <DateTimePicker
            label={t('courseOfferings.sessions.end')}
            name="sessionEnd"
            value={form.end}
            onChange={(e) => setForm((prev) => ({ ...prev, end: e.target.value }))}
            required
            error={scheduleError}
          />
        </div>
        <Input
          label={t('courseOfferings.sessions.location')}
          value={form.location}
          onChange={(e) => setForm((prev) => ({ ...prev, location: e.target.value }))}
        />
        <Textarea
          label={t('courseOfferings.sessions.notes')}
          value={form.notes}
          onChange={(e) => setForm((prev) => ({ ...prev, notes: e.target.value }))}
        />
      </div>
    </Modal>
  );
}
