/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { CourseOfferingTemplateList } from '@/components/organization/course-offerings/CourseOfferingTemplateList';
import { Input } from '@/components/ui/input';
import { ListContainer, ListRow } from '@/components/ui/list';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useInstantiateCourseOfferingTemplate } from '@/lib/store';
import type { CourseOfferingDto } from '@/types/courseOffering';

interface Props {
  organizationId: number;
  onClose: () => void;
  onCreateNew: () => void;
  onCreated: (offering: CourseOfferingDto) => void;
}

type Step = 'choose' | 'template' | 'date';

/** Entry point of "Hinzufügen": a new offering from scratch or a draft from a template. */
export function AddCourseOfferingModal({ organizationId, onClose, onCreateNew, onCreated }: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const instantiate = useInstantiateCourseOfferingTemplate(organizationId);
  const [step, setStep] = useState<Step>('choose');
  const [template, setTemplate] = useState<CourseOfferingDto | null>(null);
  const [firstSessionDate, setFirstSessionDate] = useState('');

  const handleInstantiate = async () => {
    if (!template) return;
    try {
      const created = await instantiate.mutateAsync({
        templateId: template.id,
        firstSessionDate,
      });
      showSuccess(t('courseOfferings.templates.instantiated'));
      onCreated(created);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('courseOfferings.templates.instantiateFailed'));
    }
  };

  const title =
    step === 'choose'
      ? t('courseOfferings.templates.addTitle')
      : step === 'template'
        ? t('courseOfferings.templates.pickTemplate')
        : t('courseOfferings.templates.addFromTemplate');

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={title}
      onBack={
        step === 'choose' ? undefined : () => setStep(step === 'date' ? 'template' : 'choose')
      }
      footer={
        step === 'date' ? (
          <ModalFooter
            onCancel={onClose}
            onSave={() => void handleInstantiate()}
            isValid={firstSessionDate !== ''}
            isLoading={instantiate.isPending}
          />
        ) : undefined
      }
    >
      {step === 'choose' ? (
        <ListContainer variant="inset">
          <ListRow
            title={t('courseOfferings.templates.addNew')}
            subtitle={t('courseOfferings.templates.addNewHint')}
            onClick={onCreateNew}
          />
          <ListRow
            title={t('courseOfferings.templates.addFromTemplate')}
            subtitle={t('courseOfferings.templates.addFromTemplateHint')}
            onClick={() => setStep('template')}
          />
        </ListContainer>
      ) : null}
      {step === 'template' ? (
        <CourseOfferingTemplateList
          organizationId={organizationId}
          onSelect={(selected) => {
            setTemplate(selected);
            setStep('date');
          }}
        />
      ) : null}
      {step === 'date' ? (
        <div className="space-y-4">
          <p className="text-sm text-[var(--text-secondary)]">
            {template?.templateName} · {template?.trainingCourseTypeName}
          </p>
          <Input
            label={t('courseOfferings.templates.firstSessionDate')}
            type="date"
            value={firstSessionDate}
            onChange={(e) => setFirstSessionDate(e.target.value)}
            required
          />
        </div>
      ) : null}
    </Modal>
  );
}
