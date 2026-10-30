/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { StandardWorkflowEditFormFields } from '@/components/admin/standards/StandardWorkflowEditFormFields';
import {
  buildStandardTitleByKey,
  eligibleWorkflowApproverOrgGroups,
  standardOrgKeyLabel,
  standardWorkflowTypeId,
  standardWorkflowTypeLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Badge } from '@/components/ui/badge';
import { IconButton } from '@/components/ui/icon-button';
import { ListRow } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { WorkflowOpenScopeListSections } from '@/components/workflow/WorkflowOpenScopeListSections';
import { useTranslation } from '@/hooks/useTranslation';
import { type StandardContentItemDto, standardsApi } from '@/lib/api/standards';
import { getAdminStandardWorkflowPath } from '@/lib/navigation/adminWorkspace';

interface StandardWorkflowManagementProps {
  state: StandardContentState;
  embedded?: boolean;
}

function workflowBadges(attributes: Record<string, string>, t: (key: string) => string) {
  const badges = [];
  if (attributes.enabled === 'false') {
    badges.push(
      <Badge key="disabled" tone="neutral">
        {t('admin.standards.fields.disabled')}
      </Badge>,
    );
  }
  if (attributes.selfApproveAllowed === 'true') {
    badges.push(
      <Badge key="self-approve" tone="neutral">
        {t('admin.standards.fields.selfApproveAllowed')}
      </Badge>,
    );
  }
  if (attributes.autoApprove === 'true') {
    badges.push(
      <Badge key="auto-approve" tone="neutral">
        {t('admin.standards.fields.autoApprove')}
      </Badge>,
    );
  }
  return badges;
}

export function StandardWorkflowManagement({
  state,
  embedded = false,
}: StandardWorkflowManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { tenantId, standardId, items, editable, busy, runMutation } = state;

  const workflowItems = useMemo(
    () => items.filter((i) => i.domainKey === 'workflow-type-mappings'),
    [items],
  );
  const orgGroupItems = useMemo(() => eligibleWorkflowApproverOrgGroups(items), [items]);
  const titleByKey = useMemo(() => buildStandardTitleByKey(items), [items]);

  const [editOpen, setEditOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<StandardContentItemDto | null>(null);
  const [approverOrgGroupConfigKey, setApproverOrgGroupConfigKey] = useState('');
  const [autoApprove, setAutoApprove] = useState(false);
  const [enabled, setEnabled] = useState(true);
  const [selfApproveAllowed, setSelfApproveAllowed] = useState(false);

  const openEdit = (item: StandardContentItemDto) => {
    const a = item.attributes ?? {};
    setEditingItem(item);
    setApproverOrgGroupConfigKey(a.approverOrgGroupConfigKey ?? '');
    setAutoApprove(a.autoApprove === 'true');
    setEnabled(a.enabled !== 'false');
    setSelfApproveAllowed(a.selfApproveAllowed === 'true');
    setEditOpen(true);
  };

  const closeEdit = () => {
    setEditOpen(false);
    setEditingItem(null);
  };

  const handleSave = async () => {
    if (!editingItem) return;
    const ok = await runMutation(
      () =>
        standardsApi.updateWorkflow(tenantId, standardId, editingItem.configKey, {
          approverOrgGroupConfigKey,
          autoApprove,
          enabled,
          selfApproveAllowed,
        }),
      'admin.standards.workflowSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) closeEdit();
  };

  return (
    <>
      <OrganizationManageSection
        embedded={embedded}
        title={t('admin.standards.sections.workflowTypeMappings')}
      >
        {workflowItems.length === 0 ? (
          <EmptyStateCard>{t('admin.standards.detail.empty')}</EmptyStateCard>
        ) : (
          <WorkflowOpenScopeListSections
            items={workflowItems}
            getWorkflowTypeId={standardWorkflowTypeId}
            getItemKey={(item) => item.configKey}
            listLabel={t('admin.standards.sections.workflowTypeMappings')}
            renderRow={(item) => {
              const a = item.attributes ?? {};
              const badges = workflowBadges(a, t);

              return (
                <ListRow
                  compact
                  onClick={() =>
                    router.push(getAdminStandardWorkflowPath(tenantId, standardId, item.configKey))
                  }
                  title={standardWorkflowTypeLabel(t, item.title)}
                  subtitle={
                    <span className="line-clamp-1">
                      {t('admin.standards.approverLabel')}:{' '}
                      {standardOrgKeyLabel(
                        t,
                        a.approverOrgGroupConfigKey ?? item.subtitle ?? '',
                        titleByKey,
                      )}
                    </span>
                  }
                  badges={badges.length > 0 ? badges : undefined}
                  actions={
                    editable ? (
                      <IconButton
                        variant="action"
                        icon={<Pencil className="h-4 w-4" aria-hidden />}
                        aria-label={t('admin.standards.editWorkflow')}
                        title={t('admin.standards.editWorkflow')}
                        onClick={() => openEdit(item)}
                        disabled={busy}
                      />
                    ) : undefined
                  }
                />
              );
            }}
          />
        )}
      </OrganizationManageSection>

      {editOpen && editingItem ? (
        <Modal
          isOpen
          onClose={closeEdit}
          title={t('admin.standards.editWorkflow')}
          size="lg"
          footer={
            <ModalFooter
              onCancel={closeEdit}
              onSave={() => void handleSave()}
              isValid={Boolean(approverOrgGroupConfigKey)}
              isLoading={busy}
            />
          }
        >
          <StandardWorkflowEditFormFields
            approverOrgGroupConfigKey={approverOrgGroupConfigKey}
            onApproverOrgGroupConfigKeyChange={setApproverOrgGroupConfigKey}
            orgGroupItems={orgGroupItems}
            enabled={enabled}
            onEnabledChange={setEnabled}
            selfApproveAllowed={selfApproveAllowed}
            onSelfApproveAllowedChange={setSelfApproveAllowed}
            autoApprove={autoApprove}
            onAutoApproveChange={setAutoApprove}
            disabled={busy}
          />
        </Modal>
      ) : null}
    </>
  );
}
