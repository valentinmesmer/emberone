/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import { StandardFunctionGrantTargets } from '@/components/admin/standards/StandardFunctionGrantTargets';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  filterFunctionGrantsToEligibleOrgGroups,
  grantsByFunctionConfigKey,
} from '@/lib/admin/standardFunctionGrants';
import { standardsApi } from '@/lib/api/standards';
import { type TenantFunctionDto, tenantFunctionsApi } from '@/lib/api/tenantFunctions';

interface Props {
  state: StandardContentState;
  embedded?: boolean;
}

const SAVE_DEBOUNCE_MS = 400;

function setsEqual(a: Set<string>, b: Set<string>): boolean {
  if (a.size !== b.size) return false;
  for (const key of a) {
    if (!b.has(key)) return false;
  }
  return true;
}

export function StandardFunctionSelectionManagement({ state, embedded = false }: Props) {
  const t = useTranslation();
  const { showError } = useToast();
  const { tenantId, standardId, items, editable, applyContent } = state;

  const [loading, setLoading] = useState(true);
  const [catalog, setCatalog] = useState<TenantFunctionDto[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const selectedRef = useRef(selected);
  const lastSavedRef = useRef<Set<string>>(new Set());
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    selectedRef.current = selected;
  }, [selected]);

  const selectedFromStandard = useMemo(
    () =>
      new Set(items.filter((item) => item.domainKey === 'functions').map((item) => item.configKey)),
    [items],
  );

  const orgGroupTitleByKey = useMemo(() => {
    const map = new Map<string, string>();
    for (const group of items.filter((item) => item.domainKey === 'organization-groups')) {
      map.set(group.configKey, group.title);
    }
    return map;
  }, [items]);

  const grantsByFunction = useMemo(() => grantsByFunctionConfigKey(items), [items]);

  const loadCatalog = useCallback(async () => {
    setLoading(true);
    try {
      const definitions = await tenantFunctionsApi.list(tenantId);
      setCatalog(definitions.filter((definition) => !definition.archived));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.functions.loadCatalogFailed'));
      setCatalog([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId, showError, t]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadCatalog();
    });
  }, [loadCatalog]);

  useEffect(() => {
    if (setsEqual(selectedFromStandard, lastSavedRef.current)) return;
    const next = new Set(selectedFromStandard);
    setSelected(next);
    lastSavedRef.current = next;
  }, [selectedFromStandard]);

  const flushSave = useCallback(async () => {
    const toSave = selectedRef.current;
    if (setsEqual(toSave, lastSavedRef.current)) return;
    try {
      const content = await standardsApi.updateFunctionSelections(tenantId, standardId, {
        functionConfigKeys: [...toSave],
      });
      applyContent(content);
      lastSavedRef.current = new Set(toSave);
    } catch (e) {
      setSelected(new Set(lastSavedRef.current));
      showError(e instanceof Error ? e.message : t('admin.standards.errors.saveFailed'));
    }
  }, [tenantId, standardId, showError, t, applyContent]);

  const scheduleSave = useCallback(() => {
    if (saveTimeoutRef.current) clearTimeout(saveTimeoutRef.current);
    saveTimeoutRef.current = setTimeout(() => {
      saveTimeoutRef.current = null;
      void flushSave();
    }, SAVE_DEBOUNCE_MS);
  }, [flushSave]);

  useEffect(() => {
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
        void flushSave();
      }
    };
  }, [flushSave]);

  const toggle = (configKey: string) => {
    if (!editable) return;
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(configKey)) next.delete(configKey);
      else next.add(configKey);
      return next;
    });
    scheduleSave();
  };

  const content = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : catalog.length === 0 ? (
    <EmptyStateCard>{t('admin.standards.functions.emptyCatalog')}</EmptyStateCard>
  ) : (
    <StandardCatalogCheckboxList
      listLabel={t('admin.standards.functions.title')}
      idPrefix="function-selection"
      items={catalog.map((definition) => ({
        id: definition.configKey,
        label: definition.name,
      }))}
      selected={selected}
      onToggle={toggle}
      disabled={!editable}
      renderTrailing={(item, isSelected) => (
        <StandardFunctionGrantTargets
          grants={filterFunctionGrantsToEligibleOrgGroups(
            items,
            grantsByFunction.get(item.id) ?? [],
          )}
          orgGroupTitleByKey={orgGroupTitleByKey}
          show={isSelected}
        />
      )}
    />
  );

  return (
    <OrganizationManageSection
      embedded={embedded}
      title={t('admin.standards.functions.title')}
      subtitle={t('admin.standards.sections.functionsHint')}
    >
      {content}
    </OrganizationManageSection>
  );
}
