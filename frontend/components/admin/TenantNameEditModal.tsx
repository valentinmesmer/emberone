/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';

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

  const handleSave = async () => {
    const trimmed = name.trim();
    if (!trimmed) return;
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
          isValid={name.trim().length > 0}
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
