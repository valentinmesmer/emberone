/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo } from 'react';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { ListContainer } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import {
  FUNCTION_GRANT_MEMBERSHIP_TYPES,
  readOrgGroupFunctionGrantsFromItems,
} from '@/lib/admin/standardFunctionGrants';
import type { StandardContentItemDto } from '@/lib/api/standards';

interface Props {
  items: StandardContentItemDto[];
  organizationGroupConfigKey: string;
}

export function StandardOrgGroupFunctionGrantSummary({ items, organizationGroupConfigKey }: Props) {
  const t = useTranslation();

  const functionTitleByKey = useMemo(() => {
    const map = new Map<string, string>();
    for (const item of items.filter((row) => row.domainKey === 'functions')) {
      map.set(item.configKey, item.title);
    }
    return map;
  }, [items]);

  const slots = useMemo(
    () => readOrgGroupFunctionGrantsFromItems(items, organizationGroupConfigKey),
    [items, organizationGroupConfigKey],
  );

  const hasAnyGrant = FUNCTION_GRANT_MEMBERSHIP_TYPES.some((slot) => slots[slot]?.trim());

  return (
    <OrganizationManageSection embedded title={t('admin.standards.functions.grantsTitle')}>
      {!hasAnyGrant ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('admin.standards.functions.grantsSummaryEmpty')}
        </p>
      ) : (
        <ListContainer variant="inset" label={t('admin.standards.functions.grantsTitle')}>
          {FUNCTION_GRANT_MEMBERSHIP_TYPES.map((slot) => {
            const functionKey = slots[slot]?.trim() ?? '';
            const functionName = functionKey
              ? (functionTitleByKey.get(functionKey) ?? functionKey)
              : t('admin.standards.functions.noGrant');
            const roleLabel = t(`admin.standards.functions.slots.${slot}`);
            return (
              <li
                key={slot}
                className="flex items-center justify-between gap-3 px-3 py-2.5 text-sm"
              >
                <span className="text-[var(--text-secondary)]">{roleLabel}</span>
                <span
                  className={
                    functionKey
                      ? 'min-w-0 truncate text-right font-medium text-[var(--text-primary)]'
                      : 'text-right text-[var(--text-secondary)]'
                  }
                >
                  {functionName}
                </span>
              </li>
            );
          })}
        </ListContainer>
      )}
    </OrganizationManageSection>
  );
}
