/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useMemo } from 'react';
import { OrganizationFormFields } from '@/components/organization/OrganizationFormFields';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import type { OrganizationTypeDto } from '@/lib/api/organizationTypes';
import { normalizeOrganizationCode, validateRequiredFields } from '@/lib/utils';
import { isValidOrganizationCode } from '@/lib/validation/organization';
import type { CreateChildOrganizationRequest } from '@/types/organization';

export interface CreateChildOrgModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: CreateChildOrganizationRequest) => Promise<void>;
  isLoading?: boolean;
  organizationTypes: OrganizationTypeDto[];
}

export function CreateChildOrgModal({
  isOpen,
  onClose,
  onCreate,
  isLoading = false,
  organizationTypes,
}: CreateChildOrgModalProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const activeTypes = useMemo(
    () => organizationTypes.filter((type) => type.active),
    [organizationTypes],
  );

  const getInitialData = useCallback(
    () => ({ organizationCode: '', name: '', organizationTypeId: activeTypes[0]?.id ?? 0 }),
    [activeTypes],
  );

  const { formData, setFormData, reset } = useFormModal(getInitialData, isOpen, true);
  const { organizationCode, name, organizationTypeId } = formData;

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
      await onCreate({
        organizationCode: normalizeOrganizationCode(organizationCode),
        name: name.trim(),
        organizationTypeId,
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
      title={t('admin.orgTree.createChildModalTitle')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={handleClose}
          onSave={() => void handleSave()}
          isValid={
            isValidOrganizationCode(organizationCode) &&
            name.trim().length > 0 &&
            organizationTypeId > 0
          }
          isLoading={isLoading}
          saveText={t('common.save')}
        />
      }
    >
      <p className="mb-4 text-sm text-[var(--text-secondary)]">
        {t('admin.orgTree.createChildModalDescription')}
      </p>
      <OrganizationFormFields
        organizationCode={organizationCode}
        name={name}
        onOrganizationCodeChange={(v) => setFormData((p) => ({ ...p, organizationCode: v }))}
        onNameChange={(v) => setFormData((p) => ({ ...p, name: v }))}
        showType
        type={organizationTypeId}
        onTypeChange={(v) => setFormData((p) => ({ ...p, organizationTypeId: v }))}
        typeOptions={activeTypes.map((type) => ({ value: type.id, label: type.name }))}
        disabled={isLoading}
      />
    </Modal>
  );
}
