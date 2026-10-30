/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { DestructiveTwoStepConfirmModal } from '@/components/ui/DestructiveTwoStepConfirmModal';
import { useTranslation } from '@/hooks/useTranslation';

export interface AdminDeactivateOrganizationModalProps {
  isOpen: boolean;
  onClose: () => void;
  organizationName: string;
  isLoading?: boolean;
  onConfirm: () => void | Promise<void>;
}

export function AdminDeactivateOrganizationModal({
  isOpen,
  onClose,
  organizationName,
  isLoading = false,
  onConfirm,
}: AdminDeactivateOrganizationModalProps) {
  const t = useTranslation();

  return (
    <DestructiveTwoStepConfirmModal
      isOpen={isOpen}
      onClose={onClose}
      title={t('admin.orgDeactivate.modalTitle')}
      message={t('admin.orgDeactivate.modalMessage')}
      detail={t('admin.orgDeactivate.modalDetail')}
      confirmationLabel={t('admin.orgDeactivate.confirmationLabel', { name: organizationName })}
      expectedConfirmation={organizationName}
      confirmMessage={t('admin.orgDeactivate.confirmMessage', { name: organizationName })}
      confirmButtonText={t('admin.orgDeactivate.confirmButton')}
      isLoading={isLoading}
      onConfirm={onConfirm}
    />
  );
}
