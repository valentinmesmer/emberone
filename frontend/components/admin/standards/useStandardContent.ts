/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useState } from 'react';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type StandardContentDto,
  type StandardContentItemDto,
  type StandardDto,
  standardsApi,
} from '@/lib/api/standards';
import { Module } from '@/types/permission';

export interface StandardContentState {
  tenantId: number;
  standardId: number;
  idsValid: boolean;
  tenantOutOfScope: boolean;
  authLoading: boolean;
  canAccessAdministration: boolean;
  standard: StandardDto | null;
  items: StandardContentItemDto[];
  loading: boolean;
  busy: boolean;
  editable: boolean;
  /** Modules enabled for this standard (always includes CORE). Permission groups may only grant
   * permissions of an enabled module. */
  enabledModules: Set<Module>;
  updateEnabledModules: (modules: Set<Module>) => Promise<boolean>;
  reload: () => Promise<void>;
  /** Updates standard + items from an API response without toggling page-level loading. */
  applyContent: (content: StandardContentDto) => void;
  runMutation: (
    action: () => Promise<StandardContentDto>,
    successKey: string,
    errorKey: string,
  ) => Promise<boolean>;
  updateStandardMeta: (updated: StandardDto) => void;
}

export function useStandardContent(tenantId: number, standardId: number): StandardContentState {
  const {
    canAccessAdministration,
    isGlobalAdmin,
    tenantId: authTenantId,
    isLoading: authLoading,
  } = useAuth();
  const t = useTranslation();
  const { showSuccess, showError } = useToast();

  const idsValid =
    Number.isFinite(tenantId) && tenantId > 0 && Number.isFinite(standardId) && standardId > 0;
  const tenantOutOfScope =
    !isGlobalAdmin && idsValid && (authTenantId == null || tenantId !== authTenantId);

  const [standard, setStandard] = useState<StandardDto | null>(null);
  const [items, setItems] = useState<StandardContentItemDto[]>([]);
  const [enabledModules, setEnabledModules] = useState<Set<Module>>(new Set([Module.CORE]));
  const [loading, setLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  const applyContent = useCallback((content: StandardContentDto) => {
    setStandard(content.standard);
    setItems(content.items);
  }, []);

  const reload = useCallback(async () => {
    if (!idsValid) return;
    setLoading(true);
    try {
      const [content, modules] = await Promise.all([
        standardsApi.getContent(tenantId, standardId),
        standardsApi.getEnabledModules(tenantId, standardId),
      ]);
      applyContent(content);
      setEnabledModules(new Set([Module.CORE, ...modules.modules.map((m) => m as Module)]));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.detail.loadFailed'));
    } finally {
      setLoading(false);
    }
  }, [idsValid, tenantId, standardId, showError, t, applyContent]);

  const updateEnabledModules = useCallback(
    async (modules: Set<Module>) => {
      try {
        const result = await standardsApi.updateEnabledModules(tenantId, standardId, {
          modules: Array.from(modules).filter((m) => m !== Module.CORE),
        });
        setEnabledModules(new Set([Module.CORE, ...result.modules.map((m) => m as Module)]));
        return true;
      } catch (e) {
        showError(e instanceof Error ? e.message : t('admin.standards.modules.saveFailed'));
        return false;
      }
    },
    [tenantId, standardId, showError, t],
  );

  useEffect(() => {
    if (!canAccessAdministration || authLoading || !idsValid || tenantOutOfScope) {
      return;
    }
    const id = window.setTimeout(() => void reload(), 0);
    return () => window.clearTimeout(id);
  }, [canAccessAdministration, authLoading, idsValid, tenantOutOfScope, reload]);

  const runMutation = useCallback(
    async (action: () => Promise<StandardContentDto>, successKey: string, errorKey: string) => {
      setBusy(true);
      try {
        applyContent(await action());
        if (successKey) {
          showSuccess(t(successKey));
        }
        return true;
      } catch (e) {
        showError(e instanceof Error ? e.message : t(errorKey));
        return false;
      } finally {
        setBusy(false);
      }
    },
    [applyContent, showError, showSuccess, t],
  );

  const updateStandardMeta = useCallback((updated: StandardDto) => {
    setStandard(updated);
  }, []);

  return {
    tenantId,
    standardId,
    idsValid,
    tenantOutOfScope,
    authLoading,
    canAccessAdministration,
    standard,
    items,
    loading,
    busy,
    editable: standard != null && !standard.system,
    enabledModules,
    updateEnabledModules,
    reload,
    applyContent,
    runMutation,
    updateStandardMeta,
  };
}
