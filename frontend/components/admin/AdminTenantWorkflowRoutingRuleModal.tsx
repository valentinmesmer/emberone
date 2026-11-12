/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { StandardCatalogCheckboxList } from '@/components/admin/standards/StandardCatalogCheckboxList';
import { WorkflowDefinitionOrgField } from '@/components/admin/WorkflowDefinitionOrgField';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { workflowsApi } from '@/lib/api/workflows';
import { invalidateAdminTenantCatalog } from '@/lib/store';
import type { FlatAdminOrganization } from '@/lib/utils/adminOrgTree';
import type { RoutingSubjectOption } from '@/lib/workflows/routingDimensions';
import type { WorkflowRoutingRuleDto } from '@/types/workflow';

export interface AdminTenantWorkflowRoutingRuleModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSaved: (rule: WorkflowRoutingRuleDto) => void;
  tenantId: number;
  workflowTypeId: number;
  /** Rule to edit; null creates a new one. */
  rule: WorkflowRoutingRuleDto | null;
  /** All rules of the workflow type; subjects of other rules are not offered. */
  rules: WorkflowRoutingRuleDto[];
  subjectOptions: RoutingSubjectOption[];
  /** Plural noun of the routing dimension, e.g. "Zertifizierungen". */
  dimensionLabel: string;
  organizations: FlatAdminOrganization[];
}

export function AdminTenantWorkflowRoutingRuleModal({
  isOpen,
  onClose,
  onSaved,
  tenantId,
  workflowTypeId,
  rule,
  rules,
  subjectOptions,
  dimensionLabel,
  organizations,
}: AdminTenantWorkflowRoutingRuleModalProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const queryClient = useQueryClient();
  const [saving, setSaving] = useState(false);
  const [subjectIds, setSubjectIds] = useState<Set<number>>(() => new Set(rule?.subjectIds ?? []));
  const [staticOrgIds, setStaticOrgIds] = useState<Set<number>>(
    () => new Set(rule?.staticApproverOrganizationIds ?? []),
  );

  const items = useMemo(() => {
    const usedByOthers = new Set(
      rules.filter((r) => r.id !== rule?.id).flatMap((r) => r.subjectIds),
    );
    return subjectOptions
      .filter((option) => !usedByOthers.has(option.id))
      .filter((option) => !option.archived || subjectIds.has(option.id))
      .map((option) => ({ id: String(option.id), label: option.label }));
  }, [subjectOptions, rules, rule?.id, subjectIds]);

  const toggleSubject = (id: string) => {
    const numericId = Number(id);
    setSubjectIds((current) => {
      const next = new Set(current);
      if (!next.delete(numericId)) next.add(numericId);
      return next;
    });
  };

  const handleSave = async () => {
    const body = {
      subjectIds: [...subjectIds].sort((a, b) => a - b),
      staticApproverOrganizationIds: [...staticOrgIds].sort((a, b) => a - b),
    };
    setSaving(true);
    try {
      const saved = rule
        ? await workflowsApi.updateTenantWorkflowRoutingRule(
            tenantId,
            workflowTypeId,
            rule.id,
            body,
          )
        : await workflowsApi.createTenantWorkflowRoutingRule(tenantId, workflowTypeId, body);
      await invalidateAdminTenantCatalog(queryClient, 'workflows', tenantId);
      showSuccess(t('admin.workflowDefinitions.routing.saved'));
      onSaved(saved);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.workflowDefinitions.routing.errors.saveFailed'),
      );
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={
        rule
          ? t('admin.workflowDefinitions.routing.editTitle')
          : t('admin.workflowDefinitions.routing.createTitle')
      }
      size="lg"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSave()}
          isLoading={saving}
          blockers={subjectIds.size === 0 ? [dimensionLabel] : []}
          saveText={t('common.save')}
        />
      }
    >
      <div className="space-y-4">
        <div className="space-y-1.5">
          <p className="text-sm font-medium text-[var(--text-primary)]">{dimensionLabel}</p>
          {items.length === 0 ? (
            <p className="text-xs text-[var(--text-secondary)]">
              {t('admin.workflowDefinitions.routing.noSubjectsAvailable')}
            </p>
          ) : (
            <StandardCatalogCheckboxList
              listLabel={dimensionLabel}
              idPrefix="wf-routing-subject"
              items={items}
              selected={new Set([...subjectIds].map(String))}
              onToggle={toggleSubject}
              disabled={saving}
              searchable
              maxHeightClassName="max-h-64"
            />
          )}
        </div>
        <WorkflowDefinitionOrgField
          organizations={organizations}
          staticOrgIds={staticOrgIds}
          onStaticOrgIdsChange={setStaticOrgIds}
        />
      </div>
    </Modal>
  );
}
