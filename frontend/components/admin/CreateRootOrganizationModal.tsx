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
import type { SetupOrganizationRequest } from '@/lib/api/admin';
import type { OrganizationTypeDto } from '@/lib/api/organizationTypes';
import { normalizeOrganizationCode, validateRequiredFields } from '@/lib/utils';
import { isValidOrganizationCode } from '@/lib/validation/organization';

export interface CreateRootOrganizationModalProps {
  isOpen: boolean;
  onClose: () => void;
  onCreate: (data: SetupOrganizationRequest) => Promise<void>;
  isLoading?: boolean;
  organizationTypes: OrganizationTypeDto[];
}

export function CreateRootOrganizationModal({
  isOpen,
  onClose,
  onCreate,
  isLoading = false,
  organizationTypes,
}: CreateRootOrganizationModalProps) {
  const t = useTranslation();
  const { showError } = useToast();

  const activeTypes = useMemo(
    () => organizationTypes.filter((type) => type.active),
    [organizationTypes],
  );

  const getInitialData = useCallback(
    () => ({
      organizationCode: '',
      name: '',
      organizationTypeId: activeTypes[0]?.id ?? 0,
    }),
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
    const data: SetupOrganizationRequest = {
      organizationCode: normalizeOrganizationCode(organizationCode),
      name: name.trim(),
      organizationTypeId,
    };
    try {
      await onCreate(data);
      reset();
      onClose();
    } catch {
      /* toast from parent */
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title={t('admin.rootOrg.createModalTitle')}
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
        {t('admin.rootOrg.createModalDescription')}
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
      />
    </Modal>
  );
}
