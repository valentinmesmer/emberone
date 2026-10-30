/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, RotateCcw } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { AdminTenantWorkflowDefinitionEditModal } from '@/components/admin/AdminTenantWorkflowDefinitionEditForm';
import { workflowDefinitionBadges } from '@/components/admin/workflowDefinitionBadges';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { WorkflowOpenScopeListSections } from '@/components/workflow/WorkflowOpenScopeListSections';
import { useTranslation } from '@/hooks/useTranslation';
import { adminApi } from '@/lib/api/admin';
import { workflowsApi } from '@/lib/api/workflows';
import { type FlatAdminOrganization, flattenAdminOrgTree } from '@/lib/utils/adminOrgTree';
import { getAdminTenantWorkflowDetailPath } from '@/lib/utils/adminPaths';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import type { TenantWorkflowDefinitionDto, WorkflowTypeCatalogDto } from '@/types/workflow';

interface TenantWorkflowDefinitionRow {
  catalogEntry: WorkflowTypeCatalogDto;
  tenantDefault: TenantWorkflowDefinitionDto;
}

interface AdminTenantWorkflowDefinitionManagementProps {
  tenantId: number;
  /** When true, renders only the list (no outer card/header) for embedding in a parent section. */
  embedded?: boolean;
}

export function AdminTenantWorkflowDefinitionManagement({
  tenantId,
  embedded = false,
}: AdminTenantWorkflowDefinitionManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [rows, setRows] = useState<TenantWorkflowDefinitionRow[]>([]);
  const [organizations, setOrganizations] = useState<FlatAdminOrganization[]>([]);
  const [editingTypeId, setEditingTypeId] = useState<number | null>(null);
  const [restoreTypeId, setRestoreTypeId] = useState<number | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    try {
      const [catalog, orgTree] = await Promise.all([
        workflowsApi.getWorkflowTypeCatalog(tenantId),
        adminApi.getAdminOrganizationTree(tenantId),
      ]);
      setOrganizations(flattenAdminOrgTree(orgTree.roots));

      const loadedRows = await Promise.all(
        catalog.map(async (entry) => {
          const tenantDefault = await workflowsApi.getTenantWorkflowDefinition(
            tenantId,
            entry.workflowTypeId,
          );
          return { catalogEntry: entry, tenantDefault };
        }),
      );
      setRows(loadedRows);
    } catch {
      setLoadFailed(true);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const editingRow = useMemo(
    () =>
      editingTypeId != null
        ? rows.find((r) => r.catalogEntry.workflowTypeId === editingTypeId)
        : undefined,
    [editingTypeId, rows],
  );

  const closeModal = () => setEditingTypeId(null);

  const openWorkflowDetail = (workflowTypeId: number) => {
    router.push(getAdminTenantWorkflowDetailPath(tenantId, workflowTypeId));
  };

  const handleSaved = () => {
    closeModal();
    void loadData();
  };

  const handleRestore = async () => {
    if (restoreTypeId == null) return;
    setBusy(true);
    try {
      await workflowsApi.restoreTenantWorkflowDefinition(tenantId, restoreTypeId);
      showSuccess(t('admin.workflowDefinitions.restored'));
      setRestoreTypeId(null);
      await loadData();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.workflowDefinitions.restoreFailed'));
    } finally {
      setBusy(false);
    }
  };

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : loadFailed ? (
    <p className="text-sm text-[var(--text-secondary)]">
      {t('admin.workflowDefinitions.loadFailed')}
    </p>
  ) : rows.length === 0 ? (
    <EmptyState>{t('admin.workflowDefinitions.empty')}</EmptyState>
  ) : (
    <WorkflowOpenScopeListSections
      items={rows}
      getWorkflowTypeId={(row) => row.catalogEntry.workflowTypeId}
      getItemKey={(row) => String(row.catalogEntry.workflowTypeId)}
      listLabel={t('admin.workflowDefinitions.tenantTitle')}
      renderRow={(row) => (
        <ListRow
          onClick={() => openWorkflowDetail(row.catalogEntry.workflowTypeId)}
          detailHref={getAdminTenantWorkflowDetailPath(tenantId, row.catalogEntry.workflowTypeId)}
          title={t(workflowTypeTranslationKey(row.catalogEntry.workflowTypeId))}
          badges={workflowDefinitionBadges({
            definition: row.tenantDefault,
            t,
            variant: 'tenantDefault',
          })}
          actions={
            <div className="flex items-center gap-1">
              {row.tenantDefault.customized ? (
                <IconButton
                  variant="action"
                  icon={<RotateCcw className="h-4 w-4" aria-hidden />}
                  onClick={(event) => {
                    event.stopPropagation();
                    setRestoreTypeId(row.catalogEntry.workflowTypeId);
                  }}
                  aria-label={t('admin.workflowDefinitions.restore')}
                  title={t('admin.workflowDefinitions.restore')}
                  disabled={busy}
                />
              ) : null}
              <IconButton
                variant="action"
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={(event) => {
                  event.stopPropagation();
                  setEditingTypeId(row.catalogEntry.workflowTypeId);
                }}
                aria-label={t('common.edit')}
                title={t('common.edit')}
                disabled={busy}
              />
            </div>
          }
        />
      )}
    />
  );

  return (
    <>
      {embedded ? (
        listContent
      ) : (
        <Card className="p-3 sm:p-4">
          <SectionHeader
            className="mb-3"
            title={t('admin.workflowDefinitions.tenantTitle')}
            subtitle={t('admin.workflowDefinitions.tenantSubtitle')}
          />
          {listContent}
        </Card>
      )}

      {editingRow ? (
        <AdminTenantWorkflowDefinitionEditModal
          key={`${editingRow.catalogEntry.workflowTypeId}-${editingRow.tenantDefault.id}`}
          isOpen={editingTypeId !== null}
          onClose={closeModal}
          onSaved={handleSaved}
          tenantId={tenantId}
          catalogEntry={editingRow.catalogEntry}
          tenantDefault={editingRow.tenantDefault}
          organizations={organizations}
        />
      ) : null}

      {restoreTypeId != null ? (
        <ConfirmDialog
          isOpen
          onClose={() => setRestoreTypeId(null)}
          title={t('admin.workflowDefinitions.restoreTitle')}
          message={t('admin.workflowDefinitions.restoreMessage')}
          confirmText={t('admin.workflowDefinitions.restore')}
          onConfirm={() => void handleRestore()}
          isLoading={busy}
        />
      ) : null}
    </>
  );
}
