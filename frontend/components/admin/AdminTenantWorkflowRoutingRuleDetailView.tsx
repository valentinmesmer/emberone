/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
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
import { workflowsApi } from '@/lib/api/workflows';
import {
  markAdminTenantCatalogStale,
  useAdminOrganizationTree,
  useAdminTenantWorkflowDefinitions,
  useAdminTenantWorkflowRoutingRule,
  useAdminWorkflowTypeCatalog,
} from '@/lib/store';
import { type FlatAdminOrganization, flattenAdminOrgTree } from '@/lib/utils/adminOrgTree';
import { getAdminTenantWorkflowDetailPath } from '@/lib/utils/adminPaths';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import {
  routingDimensionLabels,
  useRoutingSubjectOptions,
} from '@/lib/workflows/routingDimensions';

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
  const queryClient = useQueryClient();
  const { showError, showSuccess } = useToast();
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  const orgTreeQuery = useAdminOrganizationTree(tenantId);
  const organizations: FlatAdminOrganization[] = useMemo(
    () => flattenAdminOrgTree(orgTreeQuery.data?.roots ?? []),
    [orgTreeQuery.data],
  );
  const catalogQuery = useAdminWorkflowTypeCatalog(tenantId);
  const definitionsQuery = useAdminTenantWorkflowDefinitions(tenantId);
  const ruleQuery = useAdminTenantWorkflowRoutingRule(tenantId, workflowTypeId, ruleId);
  const rule = ruleQuery.data ?? null;
  const definition =
    definitionsQuery.data?.find((item) => item.workflowTypeId === workflowTypeId) ?? null;
  const dimension =
    catalogQuery.data?.find((item) => item.workflowTypeId === workflowTypeId)?.routingDimension ??
    null;
  const subjectOptions = useRoutingSubjectOptions(tenantId, dimension);
  const dimensionLabels = useMemo(
    () => (dimension ? routingDimensionLabels(dimension, t) : null),
    [dimension, t],
  );
  const loading =
    catalogQuery.isPending ||
    definitionsQuery.isPending ||
    ruleQuery.isPending ||
    orgTreeQuery.isPending;
  const loadFailed =
    catalogQuery.isError || definitionsQuery.isError || ruleQuery.isError || orgTreeQuery.isError;

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
      void markAdminTenantCatalogStale(queryClient, 'workflows', tenantId);
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
