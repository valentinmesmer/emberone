/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Trash2 } from 'lucide-react';
import { useEffect, useMemo, useState } from 'react';
import { CertificationExpiryDate } from '@/components/my-area/certifications/CertificationExpiryDate';
import { Badge } from '@/components/ui/badge';
import { CheckboxControl } from '@/components/ui/checkbox';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { CertificationStatus } from '@/lib/api/certifications';
import { filterAssignableDefinitions } from '@/lib/certifications/assignmentEligibility';
import { partitionCertificationStatusItems } from '@/lib/certifications/statusPartition';
import {
  CERTIFICATION_STATUS_BADGE_TONE,
  CERTIFICATION_STATUS_ROW_TONE,
} from '@/lib/certifications/statusPresentation';
import {
  filterOpenCertificationRecordWorkflows,
  resolveCertificationRecordWorkflowEntityTitle,
} from '@/lib/certifications/submittableCertifications';
import {
  getOrganizationUserCertificationDetailPath,
  getOrganizationUserWorkflowDetailPath,
} from '@/lib/navigation/userManagementSections';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useCreateCertificationAssignment,
  useDeleteCertificationAssignment,
  useOrganizationUserWorkflows,
  useOrgCertificationCatalog,
  useUserCertificationAssignments,
  useUserCertificationStatus,
} from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import { PermissionTypeEnum } from '@/types/permission';
import type { WorkflowDto } from '@/types/workflow';

interface Props {
  organizationId: number;
  userId: number;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
}

export function UserCertificationsSection({
  organizationId,
  userId,
  canWrite,
  isAddOpen,
  onAddClose,
}: Props) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { hasPermission } = usePermissions();

  const { data: status, isLoading } = useUserCertificationStatus(organizationId, userId);
  const { data: assignments = [] } = useUserCertificationAssignments(organizationId, userId);
  const { data: catalog } = useOrgCertificationCatalog(organizationId, userId);
  const createAssignment = useCreateCertificationAssignment(organizationId, userId);
  const deleteAssignment = useDeleteCertificationAssignment(organizationId, userId);
  const canReadWorkflows = hasPermission(organizationId, PermissionTypeEnum.WORKFLOW_READ);
  const { data: workflows = [] } = useOrganizationUserWorkflows(
    canReadWorkflows ? organizationId : null,
    canReadWorkflows ? userId : null,
    { fetchAll: true },
  );

  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [deleteAssignmentId, setDeleteAssignmentId] = useState<number | null>(null);

  const openCertificationRecordWorkflows = useMemo(
    () => filterOpenCertificationRecordWorkflows(workflows),
    [workflows],
  );

  const assignmentByDefinitionId = useMemo(() => {
    const map = new Map<number, (typeof assignments)[number]>();
    for (const assignment of assignments) {
      map.set(assignment.tenantCertDefinitionId, assignment);
    }
    return map;
  }, [assignments]);

  const options = useMemo(() => {
    if (!catalog) return [];
    return filterAssignableDefinitions(catalog.definitions, status?.items ?? []);
  }, [catalog, status?.items]);

  useEffect(() => {
    if (!isAddOpen) return;
    queueMicrotask(() => {
      setSelectedIds(options.length === 1 ? [options[0].id] : []);
    });
  }, [isAddOpen, options]);

  const canSubmit = selectedIds.length > 0 && options.length > 0 && !createAssignment.isPending;

  const statusLabel = (s: CertificationStatus) => t(`certifications.status.${s}`);
  const workflowTitle = (workflow: WorkflowDto) =>
    resolveCertificationRecordWorkflowEntityTitle(
      workflow,
      status?.items ?? [],
      catalog?.definitions ?? [],
    );

  const toggleDefinition = (definitionId: number) => {
    if (createAssignment.isPending) return;
    setSelectedIds((current) =>
      current.includes(definitionId)
        ? current.filter((id) => id !== definitionId)
        : [...current, definitionId],
    );
  };

  const handleCreate = async () => {
    if (selectedIds.length === 0) return;
    try {
      for (const tenantCertDefinitionId of selectedIds) {
        await createAssignment.mutateAsync({
          responsibleOrganizationId: organizationId,
          tenantCertDefinitionId,
        });
      }
      showSuccess(t('certifications.assign.success'));
      setSelectedIds([]);
      onAddClose();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('certifications.assign.error'));
    }
  };

  const handleDeleteAssignment = async () => {
    if (deleteAssignmentId == null) return;
    try {
      await deleteAssignment.mutateAsync(deleteAssignmentId);
      showSuccess(t('certifications.assign.removed'));
      setDeleteAssignmentId(null);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('certifications.assign.error'));
    }
  };

  if (isLoading) {
    return <LoadingState />;
  }

  const { active: activeItems, history: historyItems } = partitionCertificationStatusItems(
    status?.items ?? [],
  );
  const items = [...activeItems, ...historyItems];
  const showEmpty = items.length === 0 && openCertificationRecordWorkflows.length === 0;

  const renderStatusRow = (item: (typeof activeItems)[number], historical: boolean) => {
    const assignment = assignmentByDefinitionId.get(item.tenantCertDefinitionId);
    return (
      <ListRow
        key={item.tenantCertDefinitionId}
        href={getOrganizationUserCertificationDetailPath(
          organizationId,
          userId,
          item.tenantCertDefinitionId,
        )}
        tone={historical ? 'default' : CERTIFICATION_STATUS_ROW_TONE[item.status]}
        title={item.certDefinitionName}
        badges={
          historical ? (
            <Badge tone="neutral">{t('certifications.historicalTag')}</Badge>
          ) : (
            <Badge tone={CERTIFICATION_STATUS_BADGE_TONE[item.status]}>
              {statusLabel(item.status)}
            </Badge>
          )
        }
        meta={
          item.expiryDate ? (
            <div className="mt-0.5">
              <CertificationExpiryDate expiryDate={item.expiryDate} />
            </div>
          ) : undefined
        }
        actions={
          canWrite && assignment ? (
            <IconButton
              variant="action"
              icon={<Trash2 className="h-4 w-4" aria-hidden />}
              aria-label={t('certifications.assign.remove')}
              title={t('certifications.assign.remove')}
              onClick={() => setDeleteAssignmentId(assignment.id)}
            />
          ) : undefined
        }
      />
    );
  };

  const renderWorkflowRow = (workflow: WorkflowDto) => {
    return (
      <ListRow
        key={workflow.id}
        tone="warning"
        href={getOrganizationUserWorkflowDetailPath(organizationId, userId, workflow.id)}
        title={workflowTitle(workflow)}
      />
    );
  };

  return (
    <div className="space-y-4">
      {showEmpty ? (
        <EmptyStateCard>{t('certifications.empty')}</EmptyStateCard>
      ) : (
        <>
          {openCertificationRecordWorkflows.length > 0 ? (
            <section>
              <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                {t('certifications.openWorkflowsTitle')}
              </h2>
              <ListContainer label={t('certifications.openWorkflowsTitle')}>
                {openCertificationRecordWorkflows.map(renderWorkflowRow)}
              </ListContainer>
            </section>
          ) : null}

          {items.length > 0 ? (
            <section>
              {openCertificationRecordWorkflows.length > 0 ? (
                <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
                  {t('certifications.title')}
                </h2>
              ) : null}
              <ListContainer label={t('certifications.title')}>
                {items.map((item) => renderStatusRow(item, item.assigned === false))}
              </ListContainer>
            </section>
          ) : null}
        </>
      )}

      {isAddOpen ? (
        <Modal
          isOpen
          onClose={onAddClose}
          title={t('certifications.assign.title')}
          size="md"
          footer={
            <ModalFooter
              onCancel={onAddClose}
              onSave={() => void handleCreate()}
              isValid={canSubmit}
              isLoading={createAssignment.isPending}
              saveText={t('certifications.assign.action')}
              showSave
            />
          }
        >
          {options.length === 0 ? (
            <p className="text-sm text-[var(--text-secondary)]">
              {t('certifications.assign.empty')}
            </p>
          ) : (
            <fieldset
              className="max-h-64 space-y-1 overflow-y-auto rounded-md border border-[var(--border-color)]/60 p-2"
              aria-label={t('certifications.assign.definitionLabel')}
            >
              {options.map((definition) => {
                const checked = selectedIds.includes(definition.id);
                const controlId = `cert-assign-${definition.id}`;
                return (
                  <label
                    key={definition.id}
                    htmlFor={controlId}
                    className={cn(
                      'flex cursor-pointer items-center gap-3 rounded-md px-2 py-2 hover:bg-[var(--bg-secondary)]',
                      checked && 'bg-[var(--bg-secondary)]',
                      createAssignment.isPending && 'cursor-not-allowed opacity-50',
                    )}
                  >
                    <CheckboxControl
                      id={controlId}
                      checked={checked}
                      onCheckedChange={() => toggleDefinition(definition.id)}
                      disabled={createAssignment.isPending}
                      label={definition.name}
                      size="sm"
                    />
                    <span className="min-w-0 flex-1 text-sm text-[var(--text-primary)]">
                      {definition.name}
                    </span>
                  </label>
                );
              })}
            </fieldset>
          )}
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={deleteAssignmentId != null}
        onClose={() => setDeleteAssignmentId(null)}
        onConfirm={() => void handleDeleteAssignment()}
        message={t('certifications.assign.removeConfirm')}
        confirmText={t('certifications.assign.remove')}
        cancelText={t('common.cancel')}
        isLoading={deleteAssignment.isPending}
      />
    </div>
  );
}
