/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil } from 'lucide-react';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { AdminTenantWorkflowDefinitionEditModal } from '@/components/admin/AdminTenantWorkflowDefinitionEditForm';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { workflowDefinitionBadges } from '@/components/admin/workflowDefinitionBadges';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { adminApi } from '@/lib/api/admin';
import { workflowsApi } from '@/lib/api/workflows';
import { type FlatAdminOrganization, flattenAdminOrgTree } from '@/lib/utils/adminOrgTree';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import type { TenantWorkflowDefinitionDto, WorkflowTypeCatalogDto } from '@/types/workflow';

interface AdminTenantWorkflowDefinitionDetailViewProps {
  tenantId: number;
  workflowTypeId: number;
}

export function AdminTenantWorkflowDefinitionDetailView({
  tenantId,
  workflowTypeId,
}: AdminTenantWorkflowDefinitionDetailViewProps) {
  const t = useTranslation();
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [catalogEntry, setCatalogEntry] = useState<WorkflowTypeCatalogDto | null>(null);
  const [tenantDefault, setTenantDefault] = useState<TenantWorkflowDefinitionDto | null>(null);
  const [organizations, setOrganizations] = useState<FlatAdminOrganization[]>([]);
  const [isEditOpen, setIsEditOpen] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    try {
      const [catalog, orgTree] = await Promise.all([
        workflowsApi.getWorkflowTypeCatalog(tenantId),
        adminApi.getAdminOrganizationTree(tenantId),
      ]);
      const entry = catalog.find((item) => item.workflowTypeId === workflowTypeId) ?? null;
      if (!entry) {
        setCatalogEntry(null);
        setTenantDefault(null);
        return;
      }

      const defaultDefinition = await workflowsApi.getTenantWorkflowDefinition(
        tenantId,
        workflowTypeId,
      );

      setCatalogEntry(entry);
      setTenantDefault(defaultDefinition);
      setOrganizations(flattenAdminOrgTree(orgTree.roots));
    } catch {
      setLoadFailed(true);
      setCatalogEntry(null);
      setTenantDefault(null);
    } finally {
      setLoading(false);
    }
  }, [tenantId, workflowTypeId]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const orgNameById = useMemo(() => {
    const map = new Map<number, string>();
    organizations.forEach((org) => {
      map.set(org.id, org.name);
    });
    return map;
  }, [organizations]);

  const handleSaved = () => {
    setIsEditOpen(false);
    void loadData();
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed) {
    return <NoticeCard>{t('admin.workflowDefinitions.loadFailed')}</NoticeCard>;
  }

  if (!catalogEntry || !tenantDefault) {
    return <NoticeCard>{t('admin.errors.tenantLoadFailed')}</NoticeCard>;
  }

  const staticOrgNames =
    tenantDefault.staticApproverOrganizationIds.length > 0
      ? tenantDefault.staticApproverOrganizationIds
          .map((id) => orgNameById.get(id) ?? `#${id}`)
          .join(', ')
      : t('admin.workflowDefinitions.tenantDefaultNoStaticOrgs');

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          title={t(workflowTypeTranslationKey(workflowTypeId))}
          badges={workflowDefinitionBadges({
            definition: tenantDefault,
            t,
            variant: 'tenantDefault',
          })}
          actions={
            <StandardDetailAction
              label={t('common.edit')}
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={() => setIsEditOpen(true)}
            />
          }
        />

        <div className="space-y-4 border-t border-[var(--border-color)] pt-4">
          <FieldDisplay
            label={t('admin.workflowDefinitions.staticOrgsTitle')}
            value={staticOrgNames}
          />
          <FieldDisplay
            label={t('admin.workflowDefinitions.staticExclusive')}
            value={
              tenantDefault.staticApproversExclusive &&
              tenantDefault.staticApproverOrganizationIds.length > 0
                ? t('common.yes')
                : t('common.no')
            }
          />
        </div>
      </Card>

      <AdminTenantWorkflowDefinitionEditModal
        key={`${catalogEntry.workflowTypeId}-${tenantDefault.id}`}
        isOpen={isEditOpen}
        onClose={() => setIsEditOpen(false)}
        onSaved={handleSaved}
        tenantId={tenantId}
        catalogEntry={catalogEntry}
        tenantDefault={tenantDefault}
        organizations={organizations}
      />
    </div>
  );
}
