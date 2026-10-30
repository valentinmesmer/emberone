/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { StandardContentItemCheckboxPicker } from '@/components/admin/standards/StandardContentItemCheckboxPicker';
import { useTranslation } from '@/hooks/useTranslation';
import type { StandardContentItemDto } from '@/lib/api/standards';

interface StandardPermissionGroupMappingPickerProps {
  permissionGroups: StandardContentItemDto[];
  selected: Set<string>;
  onChange: (next: Set<string>) => void;
  disabled?: boolean;
}

export function StandardPermissionGroupMappingPicker({
  permissionGroups,
  selected,
  onChange,
  disabled = false,
}: StandardPermissionGroupMappingPickerProps) {
  const t = useTranslation();

  return (
    <StandardContentItemCheckboxPicker
      items={permissionGroups}
      selected={selected}
      onChange={onChange}
      disabled={disabled}
      title={t('organizationManagement.organizationGroups.permissionGroups')}
      emptyMessage={t('organizationManagement.organizationGroups.noPermissionGroups')}
    />
  );
}
