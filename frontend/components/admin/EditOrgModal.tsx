/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useCallback } from 'react';
import { OrganizationFormFields } from '@/components/organization/OrganizationFormFields';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import type { AdminOrganizationDetailDto } from '@/lib/api/admin';
import { formBlockers, normalizeOrganizationCode } from '@/lib/utils';
import { isValidOrganizationCode } from '@/lib/validation/organization';
import type { UpdateOrganizationRequest } from '@/types/organization';

export interface EditOrgModalProps {
  isOpen: boolean;
  onClose: () => void;
  org: AdminOrganizationDetailDto | null;
  onSave: (data: UpdateOrganizationRequest) => Promise<void>;
  isLoading?: boolean;
}

export function EditOrgModal({
  isOpen,
  onClose,
  org,
  onSave,
  isLoading = false,
}: EditOrgModalProps) {
  const t = useTranslation();

  const getInitialData = useCallback(
    () => ({ organizationCode: org?.organizationCode ?? '', name: org?.name ?? '' }),
    [org],
  );

  const { formData, setFormData, reset } = useFormModal(getInitialData, isOpen, true, [
    org?.id,
    org?.organizationCode,
    org?.name,
  ]);

  const { organizationCode, name } = formData;

  const handleClose = () => {
    reset();
    onClose();
  };

  const blockers = formBlockers([
    [t('setup.organization.code'), isValidOrganizationCode(organizationCode)],
    [t('setup.organization.name'), name.trim() !== ''],
  ]);

  const handleSave = async () => {
    if (blockers.length > 0) return;
    try {
      await onSave({
        organizationCode: normalizeOrganizationCode(organizationCode),
        name: name.trim(),
      });
      reset();
    } catch {
      /* toast from parent */
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={t('admin.orgTree.editOrgModalTitle')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={handleClose}
          onSave={() => void handleSave()}
          blockers={blockers}
          isLoading={isLoading}
          saveText={t('common.save')}
        />
      }
    >
      <OrganizationFormFields
        organizationCode={organizationCode}
        name={name}
        onOrganizationCodeChange={(v) => setFormData((p) => ({ ...p, organizationCode: v }))}
        onNameChange={(v) => setFormData((p) => ({ ...p, name: v }))}
        disabled={isLoading}
      />
    </Modal>
  );
}
