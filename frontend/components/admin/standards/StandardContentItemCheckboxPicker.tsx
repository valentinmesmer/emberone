/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import { Button } from '@/components/ui/button';
import { useTranslation } from '@/hooks/useTranslation';
import type { StandardContentItemDto } from '@/lib/api/standards';

export interface StandardContentItemCheckboxPickerProps {
  items: StandardContentItemDto[];
  selected: Set<string>;
  onChange: (next: Set<string>) => void;
  title: string;
  emptyMessage: string;
  disabled?: boolean;
  getItemSubtitle?: (item: StandardContentItemDto) => string | null | undefined;
}

export function StandardContentItemCheckboxPicker({
  items,
  selected,
  onChange,
  title,
  emptyMessage,
  disabled = false,
  getItemSubtitle,
}: StandardContentItemCheckboxPickerProps) {
  const t = useTranslation();
  const allSelected = items.length > 0 && selected.size === items.length;

  const toggle = (configKey: string) => {
    const next = new Set(selected);
    if (next.has(configKey)) next.delete(configKey);
    else next.add(configKey);
    onChange(next);
  };

  const toggleAll = () => {
    if (allSelected) {
      onChange(new Set());
    } else {
      onChange(new Set(items.map((item) => item.configKey)));
    }
  };

  if (items.length === 0) {
    return <p className="py-4 text-center text-sm text-[var(--text-secondary)]">{emptyMessage}</p>;
  }

  return (
    <div>
      <div className="mb-3 flex items-center justify-between gap-3">
        <div className="flex min-w-0 items-center gap-2">
          <span className="text-sm font-semibold text-[var(--text-secondary)]">{title}</span>
          <span className="shrink-0 text-xs text-[var(--text-secondary)]">
            ({selected.size} / {items.length})
          </span>
        </div>
        <Button
          variant="ghost"
          onClick={toggleAll}
          className="shrink-0 text-sm"
          disabled={disabled}
        >
          {allSelected
            ? t('organizationManagement.organizationGroups.deselectAll')
            : t('organizationManagement.organizationGroups.selectAll')}
        </Button>
      </div>
      <StandardCatalogCheckboxList
        listLabel={title}
        idPrefix="standard-item"
        maxHeightClassName="max-h-64"
        items={items.map((item) => ({
          id: item.configKey,
          label: item.title,
          subtitle: getItemSubtitle?.(item) ?? item.attributes?.description,
        }))}
        selected={selected}
        onToggle={toggle}
        disabled={disabled}
      />
    </div>
  );
}
