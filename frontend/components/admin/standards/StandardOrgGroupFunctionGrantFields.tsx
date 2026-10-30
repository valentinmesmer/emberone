/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useMemo, useState } from 'react';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  FUNCTION_GRANT_MEMBERSHIP_TYPES,
  type OrgGroupFunctionGrantBySlot,
} from '@/lib/admin/standardFunctionGrants';
import type { FunctionGrantMembershipType, StandardContentItemDto } from '@/lib/api/standards';
import { type TenantFunctionDto, tenantFunctionsApi } from '@/lib/api/tenantFunctions';

interface Props {
  tenantId: number;
  items: StandardContentItemDto[];
  valueBySlot: OrgGroupFunctionGrantBySlot;
  onSlotChange: (slot: FunctionGrantMembershipType, functionConfigKey: string) => void;
  disabled?: boolean;
}

export function StandardOrgGroupFunctionGrantFields({
  tenantId,
  items,
  valueBySlot,
  onSlotChange,
  disabled = false,
}: Props) {
  const t = useTranslation();
  const { showError } = useToast();

  const [loading, setLoading] = useState(true);
  const [functions, setFunctions] = useState<TenantFunctionDto[]>([]);

  const selectedFunctionKeys = useMemo(
    () =>
      new Set(items.filter((item) => item.domainKey === 'functions').map((item) => item.configKey)),
    [items],
  );

  const loadFunctions = useCallback(async () => {
    setLoading(true);
    try {
      const catalog = (await tenantFunctionsApi.list(tenantId)).filter(
        (definition) => !definition.archived && definition.active,
      );
      setFunctions(
        selectedFunctionKeys.size === 0
          ? catalog
          : catalog.filter((definition) => selectedFunctionKeys.has(definition.configKey)),
      );
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.functions.loadCatalogFailed'));
      setFunctions([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId, selectedFunctionKeys, showError, t]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadFunctions();
    });
  }, [loadFunctions]);

  const options = [
    { value: '', label: t('admin.standards.functions.noGrant') },
    ...functions.map((fn) => ({ value: fn.configKey, label: fn.name })),
  ];

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (functions.length === 0) {
    return <EmptyStateCard>{t('admin.standards.functions.emptyCatalog')}</EmptyStateCard>;
  }

  return (
    <div>
      <p className="mb-3 text-sm font-semibold text-[var(--text-secondary)]">
        {t('admin.standards.functions.grantsTitle')}
      </p>
      <div className="space-y-3 rounded-lg border border-[var(--border-color)] p-3">
        {FUNCTION_GRANT_MEMBERSHIP_TYPES.map((slot) => {
          const roleLabel = t(`admin.standards.functions.slots.${slot}`);
          return (
            <Select
              key={slot}
              label={roleLabel}
              value={valueBySlot[slot] ?? ''}
              onChange={(e) => onSlotChange(slot, e.target.value)}
              options={options}
              disabled={disabled}
              size="sm"
            />
          );
        })}
      </div>
    </div>
  );
}
