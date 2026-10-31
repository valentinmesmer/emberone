/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { DestructiveTwoStepConfirmModal } from '@/components/ui/DestructiveTwoStepConfirmModal';
import { useTranslation } from '@/hooks/useTranslation';

export interface TenantPurgeModalProps {
  isOpen: boolean;
  onClose: () => void;
  tenantName: string;
  isLoading?: boolean;
  onConfirm: () => void | Promise<void>;
}

export function TenantPurgeModal({
  isOpen,
  onClose,
  tenantName,
  isLoading = false,
  onConfirm,
}: TenantPurgeModalProps) {
  const t = useTranslation();

  return (
    <DestructiveTwoStepConfirmModal
      isOpen={isOpen}
      onClose={onClose}
      title={t('admin.tenantPurge.modalTitle')}
      message={t('admin.tenantPurge.modalMessage')}
      detail={t('admin.tenantPurge.modalDetail')}
      confirmationLabel={t('admin.tenantPurge.confirmationLabel', { name: tenantName })}
      expectedConfirmation={tenantName}
      confirmMessage={t('admin.tenantPurge.confirmMessage', { name: tenantName })}
      confirmButtonText={t('admin.tenantPurge.confirmButton')}
      isLoading={isLoading}
      onConfirm={onConfirm}
    />
  );
}
