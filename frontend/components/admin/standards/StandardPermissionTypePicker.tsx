/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import {
  ALL_PERMISSION_TYPES,
  standardPermissionLabel,
} from '@/components/admin/standards/standardContentUtils';
import { Badge } from '@/components/ui/badge';
import { useTranslation } from '@/hooks/useTranslation';
import { type Module, PERMISSION_MODULE_MAP, PermissionTypeEnum } from '@/types/permission';

interface StandardPermissionTypePickerProps {
  selected: Set<string>;
  onChange: (next: Set<string>) => void;
  disabled?: boolean;
  /** Modules enabled for the standard being edited; permissions of other modules are locked. */
  enabledModules: Set<Module>;
}

export function StandardPermissionTypePicker({
  selected,
  onChange,
  disabled = false,
  enabledModules,
}: StandardPermissionTypePickerProps) {
  const t = useTranslation();
  const lockedHint = t('admin.standards.modules.permissionLockedHint');

  const listItems = ALL_PERMISSION_TYPES.map((perm) => {
    const permissionId = PermissionTypeEnum[perm as keyof typeof PermissionTypeEnum];
    const permissionModule = PERMISSION_MODULE_MAP[permissionId];
    const moduleEnabled = enabledModules.has(permissionModule);
    const isSelected = selected.has(perm);
    const isLocked = !moduleEnabled && !isSelected;

    return {
      id: perm,
      label: standardPermissionLabel(t, perm),
      disabled: isLocked,
      title: isLocked ? lockedHint : undefined,
      showModuleDisabledBadge: !moduleEnabled && isSelected,
    };
  });

  const toggle = (perm: string) => {
    const meta = listItems.find((entry) => entry.id === perm);
    if (disabled || meta?.disabled) return;

    const next = new Set(selected);
    if (next.has(perm)) next.delete(perm);
    else next.add(perm);
    onChange(next);
  };

  return (
    <StandardCatalogCheckboxList
      idPrefix="standard-permission"
      maxHeightClassName="max-h-64"
      searchable
      items={listItems.map(({ id, label, disabled: itemDisabled, title }) => ({
        id,
        label,
        disabled: itemDisabled,
        title,
      }))}
      selected={selected}
      onToggle={toggle}
      disabled={disabled}
      renderTrailing={(item) => {
        const meta = listItems.find((entry) => entry.id === item.id);
        if (!meta?.showModuleDisabledBadge) return null;
        return (
          <Badge tone="warning" className="shrink-0">
            {t('admin.standards.modules.moduleDisabledBadge')}
          </Badge>
        );
      }}
    />
  );
}
