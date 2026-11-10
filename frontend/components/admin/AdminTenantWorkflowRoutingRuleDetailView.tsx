/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { AdminTenantWorkflowRoutingRuleModal } from '@/components/admin/AdminTenantWorkflowRoutingRuleModal';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { adminApi } from '@/lib/api/admin';
import { workflowsApi } from '@/lib/api/workflows';
import { type FlatAdminOrganization, flattenAdminOrgTree } from '@/lib/utils/adminOrgTree';
import { getAdminTenantWorkflowDetailPath } from '@/lib/utils/adminPaths';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import {
  ROUTING_DIMENSIONS,
  type RoutingSubjectOption,
  routingDimensionLabels,
} from '@/lib/workflows/routingDimensions';
import type { TenantWorkflowDefinitionDto, WorkflowRoutingRuleDto } from '@/types/workflow';

interface AdminTenantWorkflowRoutingRuleDetailViewProps {
  tenantId: number;
  workflowTypeId: number;
  ruleId: number;
}

export function AdminTenantWorkflowRoutingRuleDetailView({
  tenantId,
  workflowTypeId,
  ruleId,
}: AdminTenantWorkflowRoutingRuleDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [rule, setRule] = useState<WorkflowRoutingRuleDto | null>(null);
  const [definition, setDefinition] = useState<TenantWorkflowDefinitionDto | null>(null);
  const [dimensionLabels, setDimensionLabels] = useState<{
    singular: string;
    plural: string;
  } | null>(null);
  const [subjectOptions, setSubjectOptions] = useState<RoutingSubjectOption[]>([]);
  const [organizations, setOrganizations] = useState<FlatAdminOrganization[]>([]);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    try {
      const [catalog, orgTree, loadedRule, loadedDefinition] = await Promise.all([
        workflowsApi.getWorkflowTypeCatalog(tenantId),
        adminApi.getAdminOrganizationTree(tenantId),
        workflowsApi.getTenantWorkflowRoutingRule(tenantId, workflowTypeId, ruleId),
        workflowsApi.getTenantWorkflowDefinition(tenantId, workflowTypeId),
      ]);
      const dimension =
        catalog.find((item) => item.workflowTypeId === workflowTypeId)?.routingDimension ?? null;
      if (!dimension) {
        throw new Error('Workflow type has no routing dimension');
      }
      setSubjectOptions(await ROUTING_DIMENSIONS[dimension].loadOptions(tenantId));
      setDimensionLabels(routingDimensionLabels(dimension, t));
      setRule(loadedRule);
      setDefinition(loadedDefinition);
      setOrganizations(flattenAdminOrgTree(orgTree.roots));
    } catch {
      setLoadFailed(true);
      setRule(null);
      setDefinition(null);
    } finally {
      setLoading(false);
    }
  }, [tenantId, workflowTypeId, ruleId, t]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const orgNameById = useMemo(
    () => new Map(organizations.map((org) => [org.id, org.name])),
    [organizations],
  );
  const subjectLabelById = useMemo(
    () => new Map(subjectOptions.map((option) => [option.id, option.label])),
    [subjectOptions],
  );

  const handleDelete = async () => {
    setBusy(true);
    try {
      await workflowsApi.deleteTenantWorkflowRoutingRule(tenantId, workflowTypeId, ruleId);
      showSuccess(t('admin.workflowDefinitions.routing.deleted'));
      router.push(getAdminTenantWorkflowDetailPath(tenantId, workflowTypeId));
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.workflowDefinitions.routing.errors.deleteFailed'),
      );
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !rule || !definition || !dimensionLabels) {
    return <NoticeCard>{t('admin.workflowDefinitions.routing.detail.loadFailed')}</NoticeCard>;
  }

  const subjectNames = rule.subjectIds.map((id) => subjectLabelById.get(id) ?? `#${id}`).join(', ');
  const orgNames =
    rule.staticApproverOrganizationIds.length > 0
      ? rule.staticApproverOrganizationIds.map((id) => orgNameById.get(id) ?? `#${id}`).join(', ')
      : t('admin.workflowDefinitions.tenantDefaultNoStaticOrgs');

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          className="mb-2"
          title={subjectNames}
          subtitle={t(workflowTypeTranslationKey(workflowTypeId))}
          actions={
            <StandardDetailAction
              label={t('common.edit')}
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={() => setEditOpen(true)}
              disabled={busy}
            />
          }
        />

        <div className="space-y-4 border-t border-[var(--border-color)] pt-4">
          <FieldDisplay
            label={t('admin.workflowDefinitions.routing.detail.appliesTo')}
            value={subjectNames}
          />
          <FieldDisplay label={t('admin.workflowDefinitions.staticOrgsTitle')} value={orgNames} />
        </div>
      </Card>

      <ActionsSection>
        <ListRow
          leading={<ActionRowIcon icon={Trash2} />}
          title={t('admin.workflowDefinitions.routing.deleteTitle')}
          subtitle={t('admin.workflowDefinitions.routing.deleteHint')}
          onClick={busy ? undefined : () => setDeleteOpen(true)}
        />
      </ActionsSection>

      {editOpen ? (
        <AdminTenantWorkflowRoutingRuleModal
          isOpen
          onClose={() => setEditOpen(false)}
          onSaved={() => {
            setEditOpen(false);
            void loadData();
          }}
          tenantId={tenantId}
          workflowTypeId={workflowTypeId}
          rule={rule}
          rules={definition.routingRules}
          subjectOptions={subjectOptions}
          dimensionLabel={dimensionLabels.plural}
          organizations={organizations}
        />
      ) : null}

      {deleteOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteOpen(false)}
          title={t('admin.workflowDefinitions.routing.deleteTitle')}
          message={t('admin.workflowDefinitions.routing.deleteConfirm', {
            plural: dimensionLabels.plural,
          })}
          confirmText={t('common.delete')}
          onConfirm={() => void handleDelete()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
