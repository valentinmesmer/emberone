/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil, Power } from 'lucide-react';
import { useCallback, useMemo, useState } from 'react';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { StandardWorkflowEditModal } from '@/components/admin/standards/StandardWorkflowEditModal';
import {
  buildStandardTitleByKey,
  eligibleWorkflowApproverOrgGroups,
  findStandardItem,
  standardOrgKeyLabel,
  standardWorkflowTypeLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer, ListRow, ListRowMeta, ListRowMetaItem } from '@/components/ui/list';
import { NotFoundState } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { standardsApi } from '@/lib/api/standards';
import { adminTenantCatalogKeys, useQueryErrorToast, useStandardWorkflow } from '@/lib/store';
import { workflowSlotRoleHint, workflowSlotRoleLabel } from '@/lib/workflows/slotRoles';

interface StandardWorkflowDetailViewProps {
  state: StandardContentState;
  configKey: string;
}

export function StandardWorkflowDetailView({ state, configKey }: StandardWorkflowDetailViewProps) {
  const t = useTranslation();
  const { tenantId, standardId, items, editable, busy, runMutation } = state;

  const item = useMemo(
    () => findStandardItem(items, 'workflow-type-mappings', configKey),
    [items, configKey],
  );
  const orgGroupItems = useMemo(() => eligibleWorkflowApproverOrgGroups(items), [items]);
  const titleByKey = useMemo(() => buildStandardTitleByKey(items), [items]);

  const [editOpen, setEditOpen] = useState(false);
  const closeEdit = useCallback(() => setEditOpen(false), []);
  const [toggleOpen, setToggleOpen] = useState(false);
  const hasItem = item != null;

  // The content items only summarize the CONTEXT role; the detail needs every role.
  const queryClient = useQueryClient();
  const workflowQuery = useStandardWorkflow(tenantId, standardId, configKey, hasItem);
  useQueryErrorToast(workflowQuery.error, { messageKey: 'admin.standards.detail.loadFailed' });
  const workflow = workflowQuery.data ?? null;
  const reloadWorkflow = () =>
    void queryClient.invalidateQueries({
      queryKey: adminTenantCatalogKeys.standardWorkflow(tenantId, standardId, configKey),
    });

  if (!item) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  const typeKey = item.title;
  const enabled = workflow?.enabled ?? item.attributes?.enabled !== 'false';
  const toggleKey = enabled ? 'disable' : 'enable';

  const handleToggle = async () => {
    if (!workflow) return;
    const ok = await runMutation(
      () =>
        standardsApi.updateWorkflow(tenantId, standardId, configKey, {
          enabled: !enabled,
          slots: workflow.slots,
        }),
      `admin.standards.workflow.${toggleKey}Done`,
      'admin.standards.errors.saveFailed',
    );
    if (ok) {
      setToggleOpen(false);
      reloadWorkflow();
    }
  };

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          className="mb-2"
          title={standardWorkflowTypeLabel(t, typeKey)}
          badges={
            <Badge tone={enabled ? 'success' : 'neutral'} pill>
              {enabled ? t('admin.standards.active') : t('admin.standards.fields.disabled')}
            </Badge>
          }
          actions={
            editable && workflow ? (
              <StandardDetailAction
                label={t('admin.standards.editWorkflow')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={() => setEditOpen(true)}
                disabled={busy}
              />
            ) : undefined
          }
        />

        <SectionHeader title={t('admin.standards.workflow.slotsTitle')} />
        <ListContainer
          className="mt-3"
          variant="inset"
          label={t('admin.standards.workflow.slotsTitle')}
        >
          {(workflow?.slots ?? []).map((slot) => (
            <ListRow
              key={slot.slotRole}
              title={workflowSlotRoleLabel(slot.slotRole, t, typeKey)}
              subtitle={workflowSlotRoleHint(slot.slotRole, t, typeKey)}
              meta={
                <ListRowMeta>
                  <ListRowMetaItem>
                    {standardOrgKeyLabel(t, slot.approverOrgGroupConfigKey, titleByKey)}
                  </ListRowMetaItem>
                  <ListRowMetaItem>
                    {slot.autoApprove
                      ? t('admin.standards.workflow.summaryAuto')
                      : t('admin.standards.workflow.summaryManual')}
                  </ListRowMetaItem>
                  <ListRowMetaItem>
                    {slot.selfApproveAllowed
                      ? t('admin.standards.workflow.summarySelfAllowed')
                      : t('admin.standards.workflow.summarySelfBlocked')}
                  </ListRowMetaItem>
                </ListRowMeta>
              }
            />
          ))}
        </ListContainer>
      </Card>

      {editable && workflow ? (
        <ActionsSection>
          <ListRow
            leading={<ActionRowIcon icon={Power} />}
            title={t(`admin.standards.workflow.${toggleKey}`)}
            subtitle={t(`admin.standards.workflow.${toggleKey}Hint`)}
            onClick={busy ? undefined : () => setToggleOpen(true)}
          />
        </ActionsSection>
      ) : null}

      {editOpen && workflow ? (
        <StandardWorkflowEditModal
          state={state}
          workflow={workflow}
          typeKey={typeKey}
          orgGroupItems={orgGroupItems}
          onClose={closeEdit}
          onSaved={reloadWorkflow}
        />
      ) : null}

      {toggleOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setToggleOpen(false)}
          title={t(`admin.standards.workflow.${toggleKey}`)}
          message={t(`admin.standards.workflow.${toggleKey}Message`, {
            name: standardWorkflowTypeLabel(t, typeKey),
          })}
          confirmText={t(`admin.standards.workflow.${toggleKey}`)}
          onConfirm={() => void handleToggle()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
