/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil, RotateCcw } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { AdminTenantWorkflowDefinitionEditModal } from '@/components/admin/AdminTenantWorkflowDefinitionEditForm';
import { AdminTenantWorkflowRoutingRuleModal } from '@/components/admin/AdminTenantWorkflowRoutingRuleModal';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { workflowDefinitionBadges } from '@/components/admin/workflowDefinitionBadges';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { SectionActions, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { workflowsApi } from '@/lib/api/workflows';
import {
  invalidateAdminTenantCatalog,
  useAdminOrganizationTree,
  useAdminTenantWorkflowDefinitions,
  useAdminWorkflowTypeCatalog,
} from '@/lib/store';
import { type FlatAdminOrganization, flattenAdminOrgTree } from '@/lib/utils/adminOrgTree';
import { getAdminTenantWorkflowRoutingRuleDetailPath } from '@/lib/utils/adminPaths';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import {
  routingDimensionLabels,
  useRoutingSubjectOptions,
} from '@/lib/workflows/routingDimensions';
import type { WorkflowRoutingRuleDto } from '@/types/workflow';

interface AdminTenantWorkflowDefinitionDetailViewProps {
  tenantId: number;
  workflowTypeId: number;
}

export function AdminTenantWorkflowDefinitionDetailView({
  tenantId,
  workflowTypeId,
}: AdminTenantWorkflowDefinitionDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const queryClient = useQueryClient();
  const { showError, showSuccess } = useToast();
  const [isEditOpen, setIsEditOpen] = useState(false);
  const [ruleModalOpen, setRuleModalOpen] = useState(false);
  const [restoreOpen, setRestoreOpen] = useState(false);
  const [busy, setBusy] = useState(false);

  const orgTreeQuery = useAdminOrganizationTree(tenantId);
  const organizations: FlatAdminOrganization[] = useMemo(
    () => flattenAdminOrgTree(orgTreeQuery.data?.roots ?? []),
    [orgTreeQuery.data],
  );
  const catalogQuery = useAdminWorkflowTypeCatalog(tenantId);
  const definitionsQuery = useAdminTenantWorkflowDefinitions(tenantId);
  const catalogEntry =
    catalogQuery.data?.find((item) => item.workflowTypeId === workflowTypeId) ?? null;
  const tenantDefault =
    definitionsQuery.data?.find((item) => item.workflowTypeId === workflowTypeId) ?? null;
  const subjectOptions = useRoutingSubjectOptions(tenantId, catalogEntry?.routingDimension ?? null);
  const loading = catalogQuery.isPending || definitionsQuery.isPending || orgTreeQuery.isPending;
  const loadFailed = catalogQuery.isError || definitionsQuery.isError || orgTreeQuery.isError;

  const orgNameById = useMemo(() => {
    const map = new Map<number, string>();
    organizations.forEach((org) => {
      map.set(org.id, org.name);
    });
    return map;
  }, [organizations]);

  const subjectLabelById = useMemo(
    () => new Map(subjectOptions.map((option) => [option.id, option.label])),
    [subjectOptions],
  );

  const handleSaved = () => {
    setIsEditOpen(false);
  };

  const handleRuleCreated = (rule: WorkflowRoutingRuleDto) => {
    setRuleModalOpen(false);
    router.push(getAdminTenantWorkflowRoutingRuleDetailPath(tenantId, workflowTypeId, rule.id));
  };

  const handleRestore = async () => {
    setBusy(true);
    try {
      await workflowsApi.restoreTenantWorkflowDefinition(tenantId, workflowTypeId);
      await invalidateAdminTenantCatalog(queryClient, 'workflows', tenantId);
      showSuccess(t('admin.workflowDefinitions.restored'));
      setRestoreOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.workflowDefinitions.restoreFailed'));
    } finally {
      setBusy(false);
    }
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

  const dimension = catalogEntry.routingDimension ?? null;
  const labels = dimension ? routingDimensionLabels(dimension, t) : null;

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          title={t(workflowTypeTranslationKey(workflowTypeId))}
          subtitle={
            labels
              ? t('admin.workflowDefinitions.routing.defaultRuleHint', { plural: labels.plural })
              : undefined
          }
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

      {dimension && labels ? (
        <Card className="p-3 sm:p-4">
          <SectionHeader
            title={t('admin.workflowDefinitions.routing.title', { singular: labels.singular })}
            subtitle={t('admin.workflowDefinitions.routing.subtitle', { plural: labels.plural })}
            action={
              <SectionActions layout="inline">
                <Button
                  variant="primary"
                  size="xs"
                  fullWidth="responsive"
                  onClick={() => setRuleModalOpen(true)}
                >
                  {t('admin.workflowDefinitions.routing.add')}
                </Button>
              </SectionActions>
            }
          />

          {tenantDefault.routingRules.length === 0 ? (
            <p className="mt-3 text-sm text-[var(--text-secondary)]">
              {t('admin.workflowDefinitions.routing.empty', { plural: labels.plural })}
            </p>
          ) : (
            <ListContainer
              className="mt-3"
              variant="inset"
              label={t('admin.workflowDefinitions.routing.title', { singular: labels.singular })}
            >
              {tenantDefault.routingRules.map((rule) => (
                <ListRow
                  key={rule.id}
                  href={getAdminTenantWorkflowRoutingRuleDetailPath(
                    tenantId,
                    workflowTypeId,
                    rule.id,
                  )}
                  title={rule.subjectIds
                    .map((id) => subjectLabelById.get(id) ?? `#${id}`)
                    .join(', ')}
                  subtitle={
                    rule.staticApproverOrganizationIds.length > 0
                      ? rule.staticApproverOrganizationIds
                          .map((id) => orgNameById.get(id) ?? `#${id}`)
                          .join(', ')
                      : t('admin.workflowDefinitions.tenantDefaultNoStaticOrgs')
                  }
                />
              ))}
            </ListContainer>
          )}
        </Card>
      ) : null}

      {tenantDefault.customized ? (
        <ActionsSection>
          <ListRow
            leading={<ActionRowIcon icon={RotateCcw} />}
            title={t('admin.workflowDefinitions.restore')}
            subtitle={t('admin.workflowDefinitions.restoreHint')}
            onClick={busy ? undefined : () => setRestoreOpen(true)}
          />
        </ActionsSection>
      ) : null}

      {dimension && labels && ruleModalOpen ? (
        <AdminTenantWorkflowRoutingRuleModal
          isOpen
          onClose={() => setRuleModalOpen(false)}
          onSaved={handleRuleCreated}
          tenantId={tenantId}
          workflowTypeId={workflowTypeId}
          rule={null}
          rules={tenantDefault.routingRules}
          subjectOptions={subjectOptions}
          dimensionLabel={labels.plural}
          organizations={organizations}
        />
      ) : null}

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

      {restoreOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setRestoreOpen(false)}
          title={t('admin.workflowDefinitions.restoreTitle')}
          message={t('admin.workflowDefinitions.restoreMessage')}
          confirmText={t('admin.workflowDefinitions.restore')}
          onConfirm={() => void handleRestore()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
