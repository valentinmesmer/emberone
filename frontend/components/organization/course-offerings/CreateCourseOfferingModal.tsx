/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useState } from 'react';
import { Input } from '@/components/ui/input';
import { LoadingState } from '@/components/ui/loading';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useOfferableCourseTypes } from '@/lib/store';
import { validateRequiredFields } from '@/lib/utils';
import type { CreateCourseOfferingRequest } from '@/types/courseOffering';

interface Props {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  onSubmit: (data: CreateCourseOfferingRequest) => Promise<void>;
  isLoading?: boolean;
}

export function CreateCourseOfferingModal({
  isOpen,
  onClose,
  organizationId,
  onSubmit,
  isLoading = false,
}: Props) {
  const t = useTranslation();
  const { showError } = useToast();
  const { data: types = [], isLoading: typesLoading } = useOfferableCourseTypes(
    isOpen ? organizationId : null,
    isOpen,
  );

  const [trainingCourseTypeId, setTrainingCourseTypeId] = useState('');
  const [content, setContent] = useState('');
  const [notes, setNotes] = useState('');
  const [maxParticipants, setMaxParticipants] = useState('20');

  useEffect(() => {
    if (!isOpen) return;
    setTrainingCourseTypeId('');
    setContent('');
    setNotes('');
    setMaxParticipants('20');
  }, [isOpen]);

  const maxNum = parseInt(maxParticipants, 10);
  const isValid = trainingCourseTypeId !== '' && Number.isFinite(maxNum) && maxNum >= 1;

  const maxParticipantsError =
    maxParticipants.trim() && (!Number.isFinite(maxNum) || maxNum < 1)
      ? t('common.validation.positiveInteger')
      : undefined;

  const handleSave = async () => {
    if (
      !validateRequiredFields(
        {
          trainingCourseTypeId,
          maxParticipants,
        },
        t,
        showError,
        'common.validation.fillRequired',
      )
    ) {
      return;
    }
    if (!isValid) return;
    await onSubmit({
      trainingCourseTypeId: parseInt(trainingCourseTypeId, 10),
      content: content.trim() || null,
      notes: notes.trim() || null,
      maxParticipants: maxNum,
      hasExam: false,
      requiresHomeOrgApproval: false,
    });
  };

  if (!isOpen) return null;

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('courseOfferings.create.draftTitle')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={isValid}
          isLoading={isLoading}
        />
      }
    >
      {typesLoading ? (
        <LoadingState variant="embedded" size="sm" />
      ) : (
        <div className="space-y-4">
          {types.length === 0 ? (
            <p className="text-sm text-[var(--text-secondary)]">
              {t('courseOfferings.create.noOfferableTypes')}
            </p>
          ) : (
            <Select
              label={t('courseOfferings.fields.trainingCourseType')}
              value={trainingCourseTypeId}
              onChange={(e) => setTrainingCourseTypeId(e.target.value)}
              options={types.map((type) => ({
                value: String(type.id),
                label: type.name,
              }))}
              required
            />
          )}
          <Input
            label={t('courseOfferings.fields.maxParticipants')}
            type="number"
            min={1}
            value={maxParticipants}
            onChange={(e) => setMaxParticipants(e.target.value)}
            required
            error={maxParticipantsError}
          />
          <Textarea
            label={t('courseOfferings.fields.content')}
            value={content}
            onChange={(e) => setContent(e.target.value)}
          />
          <Textarea
            label={t('courseOfferings.fields.notes')}
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
          />
        </div>
      )}
    </Modal>
  );
}
