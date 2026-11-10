/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useState } from 'react';
import { WorkflowDefinitionExclusiveSwitch } from '@/components/admin/WorkflowDefinitionExclusiveSwitch';
import { WorkflowDefinitionOrgField } from '@/components/admin/WorkflowDefinitionOrgField';
import {
  definitionToFormState,
  formStateToRequest,
} from '@/components/admin/workflowDefinitionFormModel';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { workflowsApi } from '@/lib/api/workflows';
import type { FlatAdminOrganization } from '@/lib/utils/adminOrgTree';
import { workflowTypeTranslationKey } from '@/lib/utils/workflowLabels';
import type { TenantWorkflowDefinitionDto, WorkflowTypeCatalogDto } from '@/types/workflow';

export interface AdminTenantWorkflowDefinitionEditModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: () => void;
  tenantId: number;
  catalogEntry: WorkflowTypeCatalogDto;
  tenantDefault: TenantWorkflowDefinitionDto;
  organizations: FlatAdminOrganization[];
}

export function AdminTenantWorkflowDefinitionEditModal({
  isOpen,
  onClose,
  onSaved,
  tenantId,
  catalogEntry,
  tenantDefault,
  organizations,
}: AdminTenantWorkflowDefinitionEditModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [saving, setSaving] = useState(false);

  const initial = definitionToFormState(tenantDefault);
  const [staticApproversExclusive, setStaticApproversExclusive] = useState(
    initial.staticApproversExclusive,
  );
  const [staticOrgIds, setStaticOrgIds] = useState<Set<number>>(
    () => new Set(initial.staticOrgIds),
  );

  const handleSave = async () => {
    const exclusive = staticApproversExclusive && staticOrgIds.size > 0;

    setSaving(true);
    try {
      await workflowsApi.updateTenantWorkflowDefinition(
        tenantId,
        catalogEntry.workflowTypeId,
        formStateToRequest({ staticApproversExclusive: exclusive, staticOrgIds }),
      );
      showSuccess(t('admin.workflowDefinitions.success.tenantDefaultSaved'));
      onSaved();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.workflowDefinitions.errors.saveFailed'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t(workflowTypeTranslationKey(catalogEntry.workflowTypeId))}
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isLoading={saving}
          saveText={t('common.save')}
        />
      }
    >
      <div className="space-y-3">
        <WorkflowDefinitionOrgField
          organizations={organizations}
          staticOrgIds={staticOrgIds}
          onStaticOrgIdsChange={setStaticOrgIds}
        />
        {staticOrgIds.size > 0 ? (
          <WorkflowDefinitionExclusiveSwitch
            checked={staticApproversExclusive}
            onCheckedChange={setStaticApproversExclusive}
          />
        ) : null}
      </div>
    </Modal>
  );
}
