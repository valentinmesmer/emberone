/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { standardsApi } from '@/lib/api/standards';
import { type TenantRankDto, tenantRanksApi } from '@/lib/api/tenantRanks';

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

export function StandardRankSelectionManagement({ state, embedded = false }: Props) {
  const t = useTranslation();
  const { showError } = useToast();
  const { tenantId, standardId, items, editable } = state;

  const [loading, setLoading] = useState(true);
  const [catalog, setCatalog] = useState<TenantRankDto[]>([]);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const selectedRef = useRef(selected);
  const lastSavedRef = useRef<Set<string>>(new Set());
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    selectedRef.current = selected;
  }, [selected]);

  const selectedFromStandard = useMemo(
    () => new Set(items.filter((item) => item.domainKey === 'ranks').map((item) => item.configKey)),
    [items],
  );

  const loadCatalog = useCallback(async () => {
    setLoading(true);
    try {
      const response = await tenantRanksApi.list(tenantId);
      setCatalog(response.filter((rank) => !rank.archived));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.ranks.loadCatalogFailed'));
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
      await standardsApi.updateRankSelections(tenantId, standardId, {
        rankConfigKeys: [...toSave],
      });
      lastSavedRef.current = new Set(toSave);
    } catch (e) {
      setSelected(new Set(lastSavedRef.current));
      showError(e instanceof Error ? e.message : t('admin.standards.errors.saveFailed'));
    }
  }, [tenantId, standardId, showError, t]);

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
    <EmptyStateCard>{t('admin.standards.ranks.emptyCatalog')}</EmptyStateCard>
  ) : (
    <StandardCatalogCheckboxList
      listLabel={t('admin.standards.ranks.title')}
      idPrefix="rank-selection"
      items={catalog.map((rank) => ({ id: rank.configKey, label: rank.name }))}
      selected={selected}
      onToggle={toggle}
      disabled={!editable}
    />
  );

  return (
    <OrganizationManageSection
      embedded={embedded}
      title={t('admin.standards.ranks.title')}
      subtitle={t('admin.standards.sections.ranksHint')}
    >
      {content}
    </OrganizationManageSection>
  );
}
