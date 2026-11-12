/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { useCallback, useMemo, useState } from 'react';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type StandardContentDto,
  type StandardContentItemDto,
  type StandardDto,
  standardsApi,
} from '@/lib/api/standards';
import {
  adminTenantCatalogKeys,
  invalidateAdminTenantCatalogList,
  useQueryErrorToast,
  useStandardContentQuery,
  useStandardEnabledModules,
} from '@/lib/store';
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

  const queryClient = useQueryClient();
  const queriesEnabled = canAccessAdministration && !authLoading && idsValid && !tenantOutOfScope;
  const contentQuery = useStandardContentQuery(tenantId, standardId, queriesEnabled);
  const modulesQuery = useStandardEnabledModules(tenantId, standardId, queriesEnabled);
  useQueryErrorToast(contentQuery.error ?? modulesQuery.error, {
    messageKey: 'admin.standards.detail.loadFailed',
  });
  const standard = contentQuery.data?.standard ?? null;
  const items = useMemo(() => contentQuery.data?.items ?? [], [contentQuery.data]);
  const enabledModules = useMemo(
    () =>
      new Set<Module>([Module.CORE, ...(modulesQuery.data?.modules ?? []).map((m) => m as Module)]),
    [modulesQuery.data],
  );
  const loading = contentQuery.isLoading || modulesQuery.isLoading;
  const [busy, setBusy] = useState(false);

  const applyContent = useCallback(
    (content: StandardContentDto) => {
      queryClient.setQueryData(
        adminTenantCatalogKeys.standardContent(tenantId, standardId),
        content,
      );
    },
    [queryClient, tenantId, standardId],
  );

  const reload = useCallback(async () => {
    await Promise.all([contentQuery.refetch(), modulesQuery.refetch()]);
  }, [contentQuery, modulesQuery]);

  const updateEnabledModules = useCallback(
    async (modules: Set<Module>) => {
      try {
        const result = await standardsApi.updateEnabledModules(tenantId, standardId, {
          modules: Array.from(modules).filter((m) => m !== Module.CORE),
        });
        queryClient.setQueryData(
          adminTenantCatalogKeys.standardModules(tenantId, standardId),
          result,
        );
        // Permission lists are clipped to enabled modules — refresh after the toggle.
        await queryClient.invalidateQueries({
          queryKey: adminTenantCatalogKeys.standardContent(tenantId, standardId),
        });
        return true;
      } catch (e) {
        showError(e instanceof Error ? e.message : t('admin.standards.modules.saveFailed'));
        return false;
      }
    },
    [tenantId, standardId, showError, t, queryClient],
  );

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

  const updateStandardMeta = useCallback(
    (updated: StandardDto) => {
      queryClient.setQueryData<StandardContentDto>(
        adminTenantCatalogKeys.standardContent(tenantId, standardId),
        (prev) => (prev ? { ...prev, standard: updated } : prev),
      );
      void invalidateAdminTenantCatalogList(queryClient, 'standards', tenantId);
    },
    [queryClient, tenantId, standardId],
  );

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
