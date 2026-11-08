/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useCallback, useMemo } from 'react';
import { OrganizationFormFields } from '@/components/organization/OrganizationFormFields';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useFormModal } from '@/hooks/useFormModal';
import { useTranslation } from '@/hooks/useTranslation';
import type { OrganizationTypeDto } from '@/lib/api/organizationTypes';
import { formBlockers, normalizeOrganizationCode } from '@/lib/utils';
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

  const blockers = formBlockers([
    [t('setup.organization.code'), isValidOrganizationCode(organizationCode)],
    [t('setup.organization.name'), name.trim() !== ''],
    [t('setup.organization.type'), organizationTypeId > 0],
  ]);

  const handleSave = async () => {
    if (blockers.length > 0) return;
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
          blockers={blockers}
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
