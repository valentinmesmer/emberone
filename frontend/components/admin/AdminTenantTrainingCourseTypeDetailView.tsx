/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Archive, Pencil, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { AdminTenantTrainingCourseTypeEditModal } from '@/components/admin/AdminTenantTrainingCourseTypeEditModal';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { tenantTrainingCourseTypesApi } from '@/lib/api/tenantTrainingCourseTypes';
import {
  invalidateAdminTenantCatalog,
  markAdminTenantCatalogStale,
  useTenantTrainingCourseType,
} from '@/lib/store';
import {
  getAdminTenantStandardsPath,
  getAdminTenantTrainingCourseTypeDetailPath,
} from '@/lib/utils/adminPaths';

interface AdminTenantTrainingCourseTypeDetailViewProps {
  tenantId: number;
  configKey: string;
}

export function AdminTenantTrainingCourseTypeDetailView({
  tenantId,
  configKey,
}: AdminTenantTrainingCourseTypeDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [unarchiveOpen, setUnarchiveOpen] = useState(false);

  const queryClient = useQueryClient();
  const {
    data: type = null,
    isPending: loading,
    isError: loadFailed,
  } = useTenantTrainingCourseType(tenantId, configKey);
  const reload = () =>
    void invalidateAdminTenantCatalog(queryClient, 'trainingCourseTypes', tenantId);
  const staleOnLeave = () =>
    void markAdminTenantCatalogStale(queryClient, 'trainingCourseTypes', tenantId);

  const navigateBack = () => {
    router.push(getAdminTenantStandardsPath(tenantId));
  };

  const handleDelete = async () => {
    if (!type) return;
    setBusy(true);
    try {
      await tenantTrainingCourseTypesApi.delete(tenantId, type.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.trainingCourses.deleted'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!type) return;
    setBusy(true);
    try {
      await tenantTrainingCourseTypesApi.archive(tenantId, type.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.trainingCourses.archived'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!type) return;
    setBusy(true);
    try {
      await tenantTrainingCourseTypesApi.unarchive(tenantId, type.configKey);
      reload();
      showSuccess(t('admin.tenantStandards.trainingCourses.unarchived'));
      setUnarchiveOpen(false);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !type) {
    return <NoticeCard>{t('admin.tenantStandards.trainingCourses.loadFailed')}</NoticeCard>;
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          className="mb-2"
          title={type.name}
          badges={
            type.archived ? (
              <Badge tone="neutral" pill>
                {t('admin.tenantStandards.archivedBadge')}
              </Badge>
            ) : (
              <Badge tone={type.active ? 'success' : 'neutral'} pill>
                {type.active
                  ? t('admin.standards.active')
                  : t('admin.workflowDefinitions.tenantDefaultDisabled')}
              </Badge>
            )
          }
          actions={
            !type.archived ? (
              <StandardDetailAction
                label={t('common.edit')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={() => setEditOpen(true)}
                disabled={busy}
              />
            ) : undefined
          }
        />

        <div className="space-y-4 border-t border-[var(--border-color)] pt-4">
          <FieldDisplay
            label={t('admin.tenantStandards.trainingCourses.abbreviationLabel')}
            value={type.abbreviation}
          />
        </div>
      </Card>

      <ActionsSection>
        {type.archived ? (
          <ListRow
            leading={<ActionRowIcon icon={RotateCcw} />}
            title={t('common.unarchive')}
            subtitle={t('common.unarchiveHint')}
            onClick={busy ? undefined : () => setUnarchiveOpen(true)}
          />
        ) : null}
        {!type.archived && type.archivable ? (
          <ListRow
            leading={<ActionRowIcon icon={Archive} />}
            title={t('common.archive')}
            subtitle={t('common.archiveHint')}
            onClick={busy ? undefined : () => setArchiveOpen(true)}
          />
        ) : null}
        {!type.archived && type.deletable ? (
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('common.delete')}
            subtitle={t('common.deleteHint')}
            onClick={busy ? undefined : () => setDeleteOpen(true)}
          />
        ) : null}
      </ActionsSection>

      <AdminTenantTrainingCourseTypeEditModal
        key={type.configKey}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={(savedConfigKey) => {
          setEditOpen(false);
          if (savedConfigKey !== configKey) {
            staleOnLeave();
            router.replace(getAdminTenantTrainingCourseTypeDetailPath(tenantId, savedConfigKey));
            return;
          }
          reload();
        }}
        tenantId={tenantId}
        type={type}
      />

      {deleteOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteOpen(false)}
          title={t('admin.tenantStandards.trainingCourses.deleteTitle')}
          message={t('admin.tenantStandards.trainingCourses.deleteMessage', { name: type.name })}
          confirmText={t('common.delete')}
          onConfirm={() => void handleDelete()}
          isLoading={busy}
        />
      ) : null}

      {archiveOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setArchiveOpen(false)}
          title={t('admin.tenantStandards.trainingCourses.archiveTitle')}
          message={t('admin.tenantStandards.trainingCourses.archiveMessage', { name: type.name })}
          confirmText={t('common.archive')}
          onConfirm={() => void handleArchive()}
          isLoading={busy}
        />
      ) : null}

      {unarchiveOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setUnarchiveOpen(false)}
          title={t('admin.tenantStandards.trainingCourses.unarchiveTitle')}
          message={t('admin.tenantStandards.trainingCourses.unarchiveMessage', { name: type.name })}
          confirmText={t('common.unarchive')}
          onConfirm={() => void handleUnarchive()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
