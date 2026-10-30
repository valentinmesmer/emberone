/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus, X } from 'lucide-react';
import { useMemo, useState } from 'react';
import { IconButton } from '@/components/ui/icon-button';
import { Select } from '@/components/ui/select';
import { SwitchField } from '@/components/ui/switch';
import { useTranslation } from '@/hooks/useTranslation';
import { compareText } from '@/lib/i18n/locale';
import type { FlatAdminOrganization } from '@/lib/utils/adminOrgTree';

export interface WorkflowDefinitionFormFieldsProps {
  organizations: FlatAdminOrganization[];
  staticApproversExclusive: boolean;
  onStaticApproversExclusiveChange: (exclusive: boolean) => void;
  staticOrgIds: Set<number>;
  onStaticOrgIdsChange: (ids: Set<number>) => void;
}

export function WorkflowDefinitionFormFields({
  organizations,
  staticApproversExclusive,
  onStaticApproversExclusiveChange,
  staticOrgIds,
  onStaticOrgIdsChange,
}: WorkflowDefinitionFormFieldsProps) {
  const t = useTranslation();
  const [pickerOpen, setPickerOpen] = useState(false);
  const [pickOrgId, setPickOrgId] = useState('');

  const orgById = useMemo(() => new Map(organizations.map((o) => [o.id, o])), [organizations]);

  const selectedOrgs = useMemo(
    () =>
      [...staticOrgIds]
        .map((id) => orgById.get(id))
        .filter((o): o is FlatAdminOrganization => o != null)
        .sort((a, b) => compareText(a.name, b.name)),
    [staticOrgIds, orgById],
  );

  const availableOrgs = useMemo(
    () =>
      organizations
        .filter((o) => !staticOrgIds.has(o.id))
        .sort((a, b) => compareText(a.name, b.name)),
    [organizations, staticOrgIds],
  );

  const removeOrg = (orgId: number) => {
    const next = new Set(staticOrgIds);
    next.delete(orgId);
    onStaticOrgIdsChange(next);
    if (next.size === 0 && staticApproversExclusive) {
      onStaticApproversExclusiveChange(false);
    }
  };

  const addOrg = (orgId: number) => {
    const next = new Set(staticOrgIds);
    next.add(orgId);
    onStaticOrgIdsChange(next);
    setPickOrgId('');
    setPickerOpen(false);
  };

  const handlePickOrg = (value: string) => {
    setPickOrgId(value);
    const id = Number.parseInt(value, 10);
    if (Number.isFinite(id) && id > 0) {
      addOrg(id);
    }
  };

  const openPicker = () => {
    if (availableOrgs.length === 0) return;
    setPickerOpen(true);
    setPickOrgId('');
  };

  return (
    <div className="space-y-3">
      <div className="space-y-1.5">
        <div className="flex items-center justify-between gap-2">
          <p className="text-sm font-medium text-[var(--text-primary)]">
            {t('admin.workflowDefinitions.staticOrgsTitle')}
          </p>
          <IconButton
            variant="action"
            size="sm"
            icon={<Plus className="h-4 w-4" aria-hidden />}
            aria-label={t('admin.workflowDefinitions.addStaticOrg')}
            title={t('admin.workflowDefinitions.addStaticOrg')}
            onClick={openPicker}
            disabled={availableOrgs.length === 0}
          />
        </div>

        {selectedOrgs.length === 0 && !pickerOpen ? (
          <p className="text-xs text-[var(--text-secondary)]">
            {t('admin.workflowDefinitions.noStaticOrgs')}
          </p>
        ) : (
          <ul className="divide-y divide-[var(--border-color)] overflow-hidden rounded-lg border border-[var(--border-color)]">
            {selectedOrgs.map((org) => (
              <li key={org.id} className="flex items-center gap-2 px-3 py-2">
                <span className="min-w-0 flex-1 truncate text-sm text-[var(--text-primary)]">
                  {org.name}
                </span>
                <IconButton
                  variant="ghost"
                  size="sm"
                  icon={<X className="h-4 w-4" aria-hidden />}
                  aria-label={t('admin.workflowDefinitions.removeStaticOrg', { name: org.name })}
                  title={t('admin.workflowDefinitions.removeStaticOrg', { name: org.name })}
                  onClick={() => removeOrg(org.id)}
                />
              </li>
            ))}
          </ul>
        )}

        {pickerOpen && availableOrgs.length > 0 ? (
          <Select
            id="wf-static-org-picker"
            name="wfStaticOrgPicker"
            label={t('admin.workflowDefinitions.selectStaticOrg')}
            options={[
              { value: '', label: t('admin.workflowDefinitions.selectStaticOrgPlaceholder') },
              ...availableOrgs.map((o) => ({ value: String(o.id), label: o.name })),
            ]}
            value={pickOrgId}
            onChange={(e) => handlePickOrg(e.target.value)}
          />
        ) : null}
      </div>

      {staticOrgIds.size > 0 ? (
        <SwitchField
          label={t('admin.workflowDefinitions.staticExclusive')}
          description={t('admin.workflowDefinitions.staticExclusiveHint')}
          checked={staticApproversExclusive}
          onCheckedChange={onStaticApproversExclusiveChange}
        />
      ) : null}
    </div>
  );
}
