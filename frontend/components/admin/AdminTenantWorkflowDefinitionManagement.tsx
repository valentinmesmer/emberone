/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useMemo } from 'react';
import { workflowDefinitionBadges } from '@/components/admin/workflowDefinitionBadges';
import { Card } from '@/components/ui/card';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyState } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { WorkflowOpenScopeListSections } from '@/components/workflow/WorkflowOpenScopeListSections';
import { useTranslation } from '@/hooks/useTranslation';
import { useAdminTenantWorkflowDefinitions, useAdminWorkflowTypeCatalog } from '@/lib/store';
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
  const catalogQuery = useAdminWorkflowTypeCatalog(tenantId);
  const definitionsQuery = useAdminTenantWorkflowDefinitions(tenantId);
  const loading = catalogQuery.isPending || definitionsQuery.isPending;
  const loadFailed = catalogQuery.isError || definitionsQuery.isError;
  const rows = useMemo<TenantWorkflowDefinitionRow[]>(() => {
    const definitionByType = new Map(
      (definitionsQuery.data ?? []).map((definition) => [definition.workflowTypeId, definition]),
    );
    return (catalogQuery.data ?? []).flatMap((catalogEntry) => {
      const tenantDefault = definitionByType.get(catalogEntry.workflowTypeId);
      return tenantDefault ? [{ catalogEntry, tenantDefault }] : [];
    });
  }, [catalogQuery.data, definitionsQuery.data]);

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
          href={getAdminTenantWorkflowDetailPath(tenantId, row.catalogEntry.workflowTypeId)}
          title={t(workflowTypeTranslationKey(row.catalogEntry.workflowTypeId))}
          badges={workflowDefinitionBadges({
            definition: row.tenantDefault,
            t,
            variant: 'tenantDefault',
          })}
        />
      )}
    />
  );

  return embedded ? (
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
  );
}
