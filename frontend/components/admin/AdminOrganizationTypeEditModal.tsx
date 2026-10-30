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
import { SwitchField } from '@/components/ui/switch';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { type OrganizationTypeDto, organizationTypesApi } from '@/lib/api/organizationTypes';

interface AdminOrganizationTypeEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  tenantId: number;
  /** When null, creates a new organization type. */
  organizationType: OrganizationTypeDto | null;
}

export function AdminOrganizationTypeEditModal({
  isOpen,
  onClose,
  onSaved,
  tenantId,
  organizationType,
}: AdminOrganizationTypeEditModalProps) {
  if (!isOpen) return null;
  return (
    <AdminOrganizationTypeEditModalBody
      key={organizationType?.id ?? 'create'}
      onClose={onClose}
      onSaved={onSaved}
      tenantId={tenantId}
      organizationType={organizationType}
    />
  );
}

function AdminOrganizationTypeEditModalBody({
  onClose,
  onSaved,
  tenantId,
  organizationType,
}: Omit<AdminOrganizationTypeEditModalProps, 'isOpen'>) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();

  const isCreate = organizationType == null;
  const [name, setName] = useState(() => organizationType?.name ?? '');
  const [active, setActive] = useState(() => organizationType?.active ?? true);
  const [busy, setBusy] = useState(false);

  async function handleSave() {
    const trimmed = name.trim();
    if (!trimmed) return;
    setBusy(true);
    try {
      if (isCreate) {
        await organizationTypesApi.create(tenantId, { name: trimmed });
        showSuccess(t('admin.organizationTypes.created'));
      } else {
        await organizationTypesApi.update(tenantId, organizationType.id, {
          name: trimmed,
          active,
        });
        showSuccess(t('admin.organizationTypes.updated'));
      }
      onSaved();
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : isCreate
            ? t('admin.organizationTypes.errors.createFailed')
            : t('admin.organizationTypes.errors.updateFailed'),
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        isCreate ? t('admin.organizationTypes.createTitle') : t('admin.organizationTypes.editTitle')
      }
      size="sm"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isValid={name.trim().length > 0}
          isLoading={busy}
          saveText={t('common.save')}
        />
      }
    >
      <div className="space-y-4">
        <Input
          label={t('admin.organizationTypes.nameLabel')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder={t('admin.organizationTypes.namePlaceholder')}
          autoFocus
          required
        />
        {!isCreate ? (
          <SwitchField
            label={t('admin.organizationTypes.activeLabel')}
            description={t('admin.organizationTypes.activeHint')}
            checked={active}
            onCheckedChange={setActive}
          />
        ) : null}
      </div>
    </Modal>
  );
}
