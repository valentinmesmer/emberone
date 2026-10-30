/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  useMemberMainOrganizationChangeTargets,
  useOpenOrganizationUserMainOrganizationChangeWorkflow,
} from '@/lib/store';
import { resolveWorkflowErrorMessage } from '@/lib/utils/workflowErrors';
export interface OrganizationUserMainOrganizationChangeModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationId: number;
  userId: number;
  onWorkflowOpened: (workflowId: number) => void;
}

export function OrganizationUserMainOrganizationChangeModal({
  isOpen,
  onClose,
  organizationId,
  userId,
  onWorkflowOpened,
}: OrganizationUserMainOrganizationChangeModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const openWorkflow = useOpenOrganizationUserMainOrganizationChangeWorkflow(
    organizationId,
    userId,
  );

  const { data: targetOrganizations = [], isLoading: targetsLoading } =
    useMemberMainOrganizationChangeTargets(organizationId, userId, isOpen);

  const organizationOptions = useMemo(
    () => [
      { value: '', label: t('userManagement.mainOrganizationChange.targetPlaceholder') },
      ...targetOrganizations.map((org) => ({
        value: String(org.id),
        label: org.name,
      })),
    ],
    [targetOrganizations, t],
  );

  const [newMainOrganizationId, setNewMainOrganizationId] = useState('');
  const [reason, setReason] = useState('');
  const [prevOpen, setPrevOpen] = useState(isOpen);

  if (isOpen !== prevOpen) {
    setPrevOpen(isOpen);
    if (isOpen) {
      setNewMainOrganizationId('');
      setReason('');
    }
  }

  const canSubmit =
    newMainOrganizationId.trim().length > 0 && !targetsLoading && !openWorkflow.isPending;

  const handleSubmit = async () => {
    if (!canSubmit) return;
    try {
      const workflow = await openWorkflow.mutateAsync({
        newMainOrganizationId: Number(newMainOrganizationId),
        reason: reason.trim() || undefined,
      });
      showSuccess(t('userManagement.mainOrganizationChange.workflowOpened'));
      onClose();
      onWorkflowOpened(workflow.id);
    } catch (err) {
      showError(
        resolveWorkflowErrorMessage(err, t, t('userManagement.mainOrganizationChange.failed')),
      );
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('userManagement.mainOrganizationChange.modalTitle')}
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isValid={canSubmit}
          isLoading={openWorkflow.isPending}
          saveText={t('userManagement.mainOrganizationChange.submit')}
        />
      }
    >
      <div className="space-y-4">
        <p className="text-sm text-[var(--text-secondary)]">
          {t('userManagement.mainOrganizationChange.introMainContext')}
        </p>
        <Select
          id="org-user-main-change-target"
          label={t('userManagement.mainOrganizationChange.targetLabel')}
          value={newMainOrganizationId}
          onChange={(e) => setNewMainOrganizationId(e.target.value)}
          options={organizationOptions}
          disabled={targetsLoading || openWorkflow.isPending}
          required
        />
        <Textarea
          label={t('userManagement.mainOrganizationChange.reasonLabel')}
          hint={t('userManagement.mainOrganizationChange.reasonOptional')}
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          disabled={openWorkflow.isPending}
          rows={3}
        />
      </div>
    </Modal>
  );
}
