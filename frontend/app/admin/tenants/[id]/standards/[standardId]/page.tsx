/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Copy, Pencil } from 'lucide-react';
import { useParams, useRouter } from 'next/navigation';
import { Suspense, useState } from 'react';
import { StandardCertificationSelectionManagement } from '@/components/admin/standards/StandardCertificationSelectionManagement';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { StandardDrivingLicenseSelectionManagement } from '@/components/admin/standards/StandardDrivingLicenseSelectionManagement';
import { StandardFunctionSelectionManagement } from '@/components/admin/standards/StandardFunctionSelectionManagement';
import { StandardModuleManagement } from '@/components/admin/standards/StandardModuleManagement';
import { StandardOnboardingStepManagement } from '@/components/admin/standards/StandardOnboardingStepManagement';
import { StandardOrgGroupManagement } from '@/components/admin/standards/StandardOrgGroupManagement';
import { StandardPageGate } from '@/components/admin/standards/StandardPageGate';
import { StandardPermissionGroupManagement } from '@/components/admin/standards/StandardPermissionGroupManagement';
import { StandardQualificationSelectionManagement } from '@/components/admin/standards/StandardQualificationSelectionManagement';
import { StandardRankSelectionManagement } from '@/components/admin/standards/StandardRankSelectionManagement';
import { StandardTrainingCourseTypeSelectionManagement } from '@/components/admin/standards/StandardTrainingCourseTypeSelectionManagement';
import { StandardWorkflowManagement } from '@/components/admin/standards/StandardWorkflowManagement';
import { useStandardContent } from '@/components/admin/standards/useStandardContent';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { standardsApi } from '@/lib/api/standards';

function StandardDetailInner() {
  const params = useParams();
  const router = useRouter();
  const tenantId = Number.parseInt((params?.id as string) ?? '', 10);
  const standardId = Number.parseInt((params?.standardId as string) ?? '', 10);
  const state = useStandardContent(tenantId, standardId);
  const t = useTranslation();
  const { showSuccess, showError } = useToast();

  const [editingDetails, setEditingDetails] = useState(false);
  const [detailsName, setDetailsName] = useState('');
  const [detailsDescription, setDetailsDescription] = useState('');
  const [cloneOpen, setCloneOpen] = useState(false);
  const [cloneName, setCloneName] = useState('');
  const [cloneDescription, setCloneDescription] = useState('');

  function openDetailsEdit() {
    if (!state.standard) return;
    setDetailsName(state.standard.name);
    setDetailsDescription(state.standard.description ?? '');
    setEditingDetails(true);
  }

  const [detailsBusy, setDetailsBusy] = useState(false);
  const [cloneBusy, setCloneBusy] = useState(false);

  function openClone() {
    setCloneName('');
    setCloneDescription('');
    setCloneOpen(true);
  }

  async function submitClone() {
    if (!state.standard || !cloneName.trim()) return;
    setCloneBusy(true);
    try {
      const cloned = await standardsApi.cloneStandard(state.tenantId, {
        baseStandardId: state.standard.id,
        name: cloneName.trim(),
        description: cloneDescription.trim() || undefined,
      });
      setCloneOpen(false);
      showSuccess(t('admin.standards.created'));
      router.push(`/admin/tenants/${state.tenantId}/standards/${cloned.id}`);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.cloneFailed'));
    } finally {
      setCloneBusy(false);
    }
  }

  async function submitDetails() {
    if (!state.standard || !detailsName.trim()) return;
    setDetailsBusy(true);
    try {
      const updated = await standardsApi.updateStandard(state.tenantId, state.standard.id, {
        name: detailsName.trim(),
        description: detailsDescription.trim() || undefined,
      });
      state.updateStandardMeta(updated);
      setEditingDetails(false);
      showSuccess(t('admin.standards.detailsSaved'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.standards.errors.updateFailed'));
    } finally {
      setDetailsBusy(false);
    }
  }

  return (
    <StandardPageGate state={state} requireStandard>
      {state.loading || !state.standard ? (
        <LoadingState variant="embedded" size="sm" />
      ) : (
        <div className="w-full space-y-3">
          <Card className="p-3 sm:p-4">
            <StandardDetailCardHeader
              className="mb-0"
              title={state.standard.name}
              badges={
                <Badge tone={state.standard.system ? 'neutral' : 'accent'}>
                  {state.standard.system
                    ? t('admin.standards.system')
                    : t('admin.standards.custom')}
                </Badge>
              }
              subtitle={state.standard.description ?? undefined}
              actions={
                state.editable ? (
                  <StandardDetailAction
                    label={t('admin.standards.editDetails')}
                    icon={<Pencil className="h-4 w-4" aria-hidden />}
                    onClick={openDetailsEdit}
                    disabled={state.busy}
                  />
                ) : (
                  <StandardDetailAction
                    label={t('admin.standards.clone')}
                    icon={<Copy className="h-4 w-4" aria-hidden />}
                    onClick={openClone}
                    disabled={state.busy || cloneBusy}
                  />
                )
              }
            />
            {!state.editable ? (
              <NoticeCard className="mt-2 !p-2.5 text-xs">
                {t('admin.standards.detail.readOnlyContent')}
              </NoticeCard>
            ) : null}
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardModuleManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardOrgGroupManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardPermissionGroupManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardWorkflowManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardOnboardingStepManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardTrainingCourseTypeSelectionManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardDrivingLicenseSelectionManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardCertificationSelectionManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardQualificationSelectionManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardRankSelectionManagement state={state} embedded />
          </Card>

          <Card className="p-3 sm:p-4">
            <StandardFunctionSelectionManagement state={state} embedded />
          </Card>
        </div>
      )}

      {cloneOpen ? (
        <Modal
          isOpen
          onClose={() => setCloneOpen(false)}
          title={t('admin.standards.cloneTitle')}
          footer={
            <ModalFooter
              onCancel={() => setCloneOpen(false)}
              onSave={() => void submitClone()}
              isValid={cloneName.trim().length > 0}
              isLoading={cloneBusy}
            />
          }
        >
          <div className="space-y-3">
            <p className="text-sm text-[var(--text-secondary)]">
              {t('admin.standards.cloneFrom')}: <strong>{state.standard?.name}</strong>
            </p>
            <Input
              label={t('admin.standards.name')}
              value={cloneName}
              onChange={(e) => setCloneName(e.target.value)}
              disabled={cloneBusy}
            />
            <Textarea
              label={t('admin.standards.description')}
              value={cloneDescription}
              onChange={(e) => setCloneDescription(e.target.value)}
              disabled={cloneBusy}
            />
          </div>
        </Modal>
      ) : null}

      {editingDetails ? (
        <Modal
          isOpen
          onClose={() => setEditingDetails(false)}
          title={t('admin.standards.editDetails')}
          footer={
            <ModalFooter
              onCancel={() => setEditingDetails(false)}
              onSave={() => void submitDetails()}
              isValid={detailsName.trim().length > 0}
              isLoading={detailsBusy}
            />
          }
        >
          <div className="space-y-3">
            <Input
              label={t('admin.standards.name')}
              value={detailsName}
              onChange={(e) => setDetailsName(e.target.value)}
              disabled={state.busy}
            />
            <Textarea
              label={t('admin.standards.description')}
              value={detailsDescription}
              onChange={(e) => setDetailsDescription(e.target.value)}
              disabled={state.busy}
            />
          </div>
        </Modal>
      ) : null}
    </StandardPageGate>
  );
}

export default function StandardDetailPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen bg-[var(--page-bg)] pt-20">
          <LoadingState variant="embedded" />
        </div>
      }
    >
      <StandardDetailInner />
    </Suspense>
  );
}
