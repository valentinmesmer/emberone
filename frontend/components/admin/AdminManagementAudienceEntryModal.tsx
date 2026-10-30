/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { Select } from '@/components/ui/select';
import { useTranslation } from '@/hooks/useTranslation';
import type {
  ManagementAudienceEntryDto,
  ManagementAudienceEntryRequest,
  ManagementAudienceKind,
  ManagementOptionDto,
} from '@/lib/api/admin';
import { managementPermissionLabel } from '@/lib/utils/managementActivation';
import { PermissionTypeEnum } from '@/types/permission';

const KINDS: ManagementAudienceKind[] = ['ORG', 'ORG_SUBTREE', 'ORG_TYPE', 'TENANT'];

/** Every permission type an audience may allow. Management access never can (no chains). */
const AUDIENCE_PERMISSION_TYPES = (Object.values(PermissionTypeEnum) as (string | number)[]).filter(
  (value): value is PermissionTypeEnum =>
    typeof value === 'number' && value !== PermissionTypeEnum.MANAGEMENT_ACCESS,
);

export function managementAudienceKindLabelKey(kind: ManagementAudienceKind): string {
  switch (kind) {
    case 'ORG':
      return 'admin.managementAudience.kindOrg';
    case 'ORG_SUBTREE':
      return 'admin.managementAudience.kindOrgSubtree';
    case 'ORG_TYPE':
      return 'admin.managementAudience.kindOrgType';
    case 'TENANT':
      return 'admin.managementAudience.kindTenant';
  }
}

interface AdminManagementAudienceEntryModalProps {
  /** The entry being edited; null when adding one. */
  entry: ManagementAudienceEntryDto | null;
  organizations: ManagementOptionDto[];
  organizationTypes: ManagementOptionDto[];
  /** The other entries of the audience; one target may only be covered once. */
  taken: ManagementAudienceEntryDto[];
  isSaving: boolean;
  onClose: () => void;
  onSave: (request: ManagementAudienceEntryRequest) => void;
}

/** Permission types, scope and maximum duration of one audience entry. */
export function AdminManagementAudienceEntryModal({
  entry,
  organizations,
  organizationTypes,
  taken,
  isSaving,
  onClose,
  onSave,
}: AdminManagementAudienceEntryModalProps) {
  const t = useTranslation();
  const [kind, setKind] = useState<ManagementAudienceKind>(entry?.kind ?? 'ORG');
  const [refId, setRefId] = useState<string>(entry?.refId == null ? '' : String(entry.refId));
  const [duration, setDuration] = useState<string>(entry ? String(entry.maxDurationMinutes) : '60');
  /** New entries start with every allowed type selected; the admin deselects what to leave out. */
  const [selected, setSelected] = useState<Set<string>>(
    () => new Set<string>((entry ? entry.permissionTypes : AUDIENCE_PERMISSION_TYPES).map(String)),
  );

  const targetOptions =
    kind === 'ORG_TYPE' ? organizationTypes : kind === 'TENANT' ? [] : organizations;
  const needsTarget = kind !== 'TENANT';
  const durationValue = Number(duration);
  const durationValid = Number.isInteger(durationValue) && durationValue >= 1;
  const targetValue = needsTarget ? Number(refId) : null;
  const targetMissing = needsTarget && refId === '';
  const duplicate = taken.some(
    (other) => other.kind === kind && (kind === 'TENANT' || other.refId === targetValue),
  );

  let error: string | null = null;
  if (selected.size === 0) error = t('admin.managementAudience.permissionsRequired');
  else if (!durationValid) error = t('admin.managementAudience.durationInvalid');
  else if (targetMissing) error = t('admin.managementAudience.targetRequired');
  else if (duplicate) error = t('admin.managementAudience.duplicate');

  const togglePermission = (permission: string) => {
    setSelected((current) => {
      const next = new Set(current);
      if (next.has(permission)) next.delete(permission);
      else next.add(permission);
      return next;
    });
  };

  const changeKind = (next: ManagementAudienceKind) => {
    setKind(next);
    setRefId('');
  };

  const submit = () => {
    onSave({
      kind,
      refId: needsTarget ? targetValue : null,
      permissionTypes: AUDIENCE_PERMISSION_TYPES.filter((permission) =>
        selected.has(String(permission)),
      ),
      maxDurationMinutes: durationValue,
    });
  };

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={
        entry ? t('admin.managementAudience.editTitle') : t('admin.managementAudience.addTitle')
      }
      footer={
        <div className="flex justify-end gap-2">
          <Button variant="outline" onClick={onClose} disabled={isSaving}>
            {t('common.cancel')}
          </Button>
          <Button onClick={submit} disabled={error != null || isSaving}>
            {t('admin.managementAudience.save')}
          </Button>
        </div>
      }
    >
      <div className="space-y-4">
        <Select
          label={t('admin.managementAudience.kindLabel')}
          value={kind}
          options={KINDS.map((value) => ({
            value,
            label: t(managementAudienceKindLabelKey(value)),
          }))}
          onChange={(event) => changeKind(event.target.value as ManagementAudienceKind)}
        />
        {needsTarget ? (
          <div className="w-full space-y-2">
            {/* Not `Select`'s `required`: that would drop the empty placeholder option. */}
            <label
              htmlFor="audience-target"
              className="block text-sm font-semibold text-[var(--text-primary)]"
            >
              {t('admin.managementAudience.targetLabel')}
              <span className="text-[var(--fire-red)] ml-1">*</span>
            </label>
            <Select
              id="audience-target"
              aria-required
              value={refId}
              options={[
                { value: '', label: t('admin.managementAudience.targetPlaceholder') },
                ...targetOptions.map((option) => ({
                  value: String(option.id),
                  label: option.name,
                })),
              ]}
              onChange={(event) => setRefId(event.target.value)}
            />
          </div>
        ) : null}
        <div>
          <div className="mb-2 flex items-center justify-between gap-2">
            <span className="text-sm font-semibold text-[var(--text-primary)]">
              {t('admin.managementAudience.permissionsLabel')}
              <span className="text-[var(--fire-red)] ml-1">*</span>
            </span>
            <Button
              variant="ghost"
              size="xs"
              className="-mr-2"
              onClick={() => setSelected(new Set(AUDIENCE_PERMISSION_TYPES.map(String)))}
            >
              {t('admin.managementAudience.selectAll')}
            </Button>
          </div>
          <StandardCatalogCheckboxList
            idPrefix="audience-permission"
            listLabel={t('admin.managementAudience.permissionsLabel')}
            maxHeightClassName="max-h-64"
            searchable
            items={AUDIENCE_PERMISSION_TYPES.map((permission) => ({
              id: String(permission),
              label: managementPermissionLabel(t, permission),
            }))}
            selected={selected}
            onToggle={togglePermission}
          />
        </div>
        <Input
          label={t('admin.managementAudience.durationLabel')}
          required
          type="number"
          min={1}
          value={duration}
          onChange={(event) => setDuration(event.target.value)}
        />
        {error ? (
          <p role="alert" className="text-sm text-[var(--status-danger-accent)]">
            {error}
          </p>
        ) : null}
      </div>
    </Modal>
  );
}
