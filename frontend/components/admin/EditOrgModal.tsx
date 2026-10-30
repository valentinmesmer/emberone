/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback } from 'react';
import { OrganizationFormFields } from '@/components/organization/OrganizationFormFields';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import type { AdminOrganizationDetailDto } from '@/lib/api/admin';
import { normalizeOrganizationCode, validateRequiredFields } from '@/lib/utils';
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
  const { showError } = useToast();

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

  const handleSave = async () => {
    if (
      !validateRequiredFields(
        { organizationCode, name },
        t,
        showError,
        'common.validation.fillRequired',
      )
    ) {
      return;
    }
    if (!isValidOrganizationCode(organizationCode)) {
      showError(t('common.validation.invalidOrganizationCode'));
      return;
    }
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
          isValid={isValidOrganizationCode(organizationCode) && name.trim().length > 0}
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
