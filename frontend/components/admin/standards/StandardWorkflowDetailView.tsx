/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil } from 'lucide-react';
import { useMemo, useState } from 'react';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { StandardWorkflowEditFormFields } from '@/components/admin/standards/StandardWorkflowEditFormFields';
import {
  buildStandardTitleByKey,
  eligibleWorkflowApproverOrgGroups,
  findStandardItem,
  standardOrgKeyLabel,
  standardWorkflowTypeLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { NotFoundState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { standardsApi } from '@/lib/api/standards';

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
  const a = item?.attributes ?? {};

  const [editOpen, setEditOpen] = useState(false);
  const [approverOrgGroupConfigKey, setApproverOrgGroupConfigKey] = useState('');
  const [autoApprove, setAutoApprove] = useState(false);
  const [enabled, setEnabled] = useState(true);
  const [selfApproveAllowed, setSelfApproveAllowed] = useState(false);

  if (!item) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  const openEdit = () => {
    setApproverOrgGroupConfigKey(a.approverOrgGroupConfigKey ?? '');
    setAutoApprove(a.autoApprove === 'true');
    setEnabled(a.enabled !== 'false');
    setSelfApproveAllowed(a.selfApproveAllowed === 'true');
    setEditOpen(true);
  };

  const handleSave = async () => {
    const ok = await runMutation(
      () =>
        standardsApi.updateWorkflow(tenantId, standardId, configKey, {
          approverOrgGroupConfigKey,
          autoApprove,
          enabled,
          selfApproveAllowed,
        }),
      'admin.standards.workflowSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) setEditOpen(false);
  };

  return (
    <div className="w-full space-y-3">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          title={standardWorkflowTypeLabel(t, item.title)}
          actions={
            editable ? (
              <StandardDetailAction
                label={t('admin.standards.editWorkflow')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={openEdit}
                disabled={busy}
              />
            ) : undefined
          }
        />
        <div className="space-y-3">
          <FieldDisplay
            label={t('admin.standards.fields.enabled')}
            value={a.enabled !== 'false' ? t('common.yes') : t('common.no')}
          />
          <FieldDisplay
            label={t('admin.standards.approverLabel')}
            value={standardOrgKeyLabel(
              t,
              a.approverOrgGroupConfigKey ?? item.subtitle ?? '',
              titleByKey,
            )}
          />
          <FieldDisplay
            label={t('admin.standards.fields.selfApproveAllowed')}
            value={a.selfApproveAllowed === 'true' ? t('common.yes') : t('common.no')}
          />
          <FieldDisplay
            label={t('admin.standards.fields.autoApprove')}
            value={a.autoApprove === 'true' ? t('common.yes') : t('common.no')}
          />
        </div>
      </Card>

      {editOpen ? (
        <Modal
          isOpen
          onClose={() => setEditOpen(false)}
          title={t('admin.standards.editWorkflow')}
          size="lg"
          footer={
            <ModalFooter
              onCancel={() => setEditOpen(false)}
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
    </div>
  );
}
