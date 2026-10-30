/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import { useActivateManagement } from '@/lib/store';
import { managementPermissionLabel } from '@/lib/utils/managementActivation';
import type { ManagementTargetOrganizationDto } from '@/types/management';

/** Same bounds as the backend (`ManagementActivationService`). */
const REASON_MIN_LENGTH = 1;
const REASON_MAX_LENGTH = 500;

interface ActivateManagementDialogProps {
  target: ManagementTargetOrganizationDto;
  onClose: () => void;
}

/**
 * Permissions, duration and reason of one activation. Nothing is preselected: the person picks the
 * permissions they need, up to what the target carries. Opens the target on success.
 */
export function ActivateManagementDialog({ target, onClose }: ActivateManagementDialogProps) {
  const t = useTranslation();
  const router = useRouter();
  const activate = useActivateManagement();

  const [selected, setSelected] = useState<Set<string>>(() => new Set());
  const [durationMinutes, setDurationMinutes] = useState(String(target.maxDurationMinutes));
  const [reason, setReason] = useState('');

  const duration = Number(durationMinutes);
  const durationValid =
    Number.isInteger(duration) && duration >= 1 && duration <= target.maxDurationMinutes;
  const reasonLength = reason.trim().length;
  const reasonValid = reasonLength >= REASON_MIN_LENGTH && reasonLength <= REASON_MAX_LENGTH;
  const permissionsValid = selected.size > 0;
  const canSubmit = permissionsValid && durationValid && reasonValid && !activate.isPending;

  const togglePermission = (permission: string) => {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(permission)) next.delete(permission);
      else next.add(permission);
      return next;
    });
  };

  const submit = async () => {
    const organizationId = target.organizationId;
    await activate.mutateAsync({
      managingOrganizationId: target.managingOrganizationId,
      targetOrganizationId: organizationId,
      durationMinutes: duration,
      permissionTypes: target.permissionTypes.filter((permission) =>
        selected.has(String(permission)),
      ),
      reason: reason.trim(),
    });
    onClose();
    router.push(`/organizations/${organizationId}`);
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('management.dialog.title', { name: target.organizationName })}
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose}>
            {t('common.cancel')}
          </Button>
          <Button onClick={() => void submit()} disabled={!canSubmit}>
            {t('management.dialog.submit')}
          </Button>
        </div>
      }
    >
      <div className="space-y-5">
        <div>
          <div className="flex items-center justify-between gap-2">
            <span className="text-sm font-semibold text-[var(--text-primary)]">
              {t('management.dialog.permissionsLabel')}
              <span className="text-[var(--fire-red)] ml-1">*</span>
            </span>
            <Button
              variant="ghost"
              size="xs"
              className="-mr-2"
              onClick={() => setSelected(new Set(target.permissionTypes.map(String)))}
            >
              {t('management.dialog.selectAll')}
            </Button>
          </div>
          <p className="mb-3 mt-1 text-xs text-[var(--text-secondary)]">
            {t('management.dialog.permissionsHint')}
          </p>
          <StandardCatalogCheckboxList
            idPrefix="management-permission"
            listLabel={t('management.dialog.permissionsLabel')}
            maxHeightClassName="max-h-64"
            searchable
            items={target.permissionTypes.map((permission) => ({
              id: String(permission),
              label: managementPermissionLabel(t, permission),
            }))}
            selected={selected}
            onToggle={togglePermission}
          />
        </div>
        <div>
          <Input
            id="management-duration"
            label={t('management.dialog.duration', { max: target.maxDurationMinutes })}
            required
            type="number"
            min={1}
            max={target.maxDurationMinutes}
            value={durationMinutes}
            onChange={(event) => setDurationMinutes(event.target.value)}
          />
        </div>
        <div>
          <Textarea
            id="management-reason"
            label={t('management.dialog.reason')}
            required
            rows={3}
            maxLength={REASON_MAX_LENGTH}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
          />
          <p className="mt-1 text-xs text-[var(--text-secondary)]">
            {t('management.dialog.reasonHint', { max: REASON_MAX_LENGTH })}
          </p>
        </div>
        {activate.isError ? (
          <p role="alert" className="text-sm text-[var(--status-danger-accent)]">
            {activate.error instanceof Error
              ? activate.error.message
              : t('management.dialog.error')}
          </p>
        ) : null}
      </div>
    </Modal>
  );
}
