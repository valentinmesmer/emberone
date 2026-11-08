/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { formBlockers } from '@/lib/utils';

export interface TenantNameEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  isLoading: boolean;
  initialName: string;
  onSave: (name: string) => Promise<void>;
}

export function TenantNameEditModal({
  isOpen,
  onClose,
  isLoading,
  initialName,
  onSave,
}: TenantNameEditModalProps) {
  const t = useTranslation();
  const [name, setName] = useState(initialName);

  const blockers = formBlockers([[t('admin.tenantForm.name'), name.trim() !== '']]);

  const handleSave = async () => {
    if (blockers.length > 0) return;
    const trimmed = name.trim();
    await onSave(trimmed);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('admin.tenantForm.editNameModalTitle')}
      size="sm"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          blockers={blockers}
          isLoading={isLoading}
        />
      }
    >
      <Input
        label={t('admin.tenantForm.name')}
        value={name}
        onChange={(e) => setName(e.target.value)}
        required
        autoFocus
      />
    </Modal>
  );
}
