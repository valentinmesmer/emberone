/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Archive, Pencil, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantTrainingCourseTypeEditModal } from '@/components/admin/AdminTenantTrainingCourseTypeEditModal';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type TenantTrainingCourseTypeDto,
  tenantTrainingCourseTypesApi,
} from '@/lib/api/tenantTrainingCourseTypes';
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

  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [type, setType] = useState<TenantTrainingCourseTypeDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [unarchiveOpen, setUnarchiveOpen] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    try {
      setType(await tenantTrainingCourseTypesApi.get(tenantId, configKey));
    } catch {
      setLoadFailed(true);
      setType(null);
    } finally {
      setLoading(false);
    }
  }, [tenantId, configKey]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const navigateBack = () => {
    router.push(getAdminTenantStandardsPath(tenantId));
  };

  const handleDelete = async () => {
    if (!type) return;
    setBusy(true);
    try {
      await tenantTrainingCourseTypesApi.delete(tenantId, type.configKey);
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
      showSuccess(t('admin.tenantStandards.trainingCourses.unarchived'));
      setUnarchiveOpen(false);
      await loadData();
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
            type.archived ? (
              <StandardDetailAction
                label={t('common.unarchive')}
                icon={<RotateCcw className="h-4 w-4" aria-hidden />}
                onClick={() => setUnarchiveOpen(true)}
                disabled={busy}
              />
            ) : (
              <>
                {type.deletable ? (
                  <StandardDetailAction
                    label={t('common.delete')}
                    icon={<Trash2 className="h-4 w-4" aria-hidden />}
                    onClick={() => setDeleteOpen(true)}
                    disabled={busy}
                  />
                ) : null}
                {type.archivable ? (
                  <StandardDetailAction
                    label={t('common.archive')}
                    icon={<Archive className="h-4 w-4" aria-hidden />}
                    onClick={() => setArchiveOpen(true)}
                    disabled={busy}
                  />
                ) : null}
                <StandardDetailAction
                  label={t('common.edit')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setEditOpen(true)}
                  disabled={busy}
                />
              </>
            )
          }
        />

        <div className="space-y-4 border-t border-[var(--border-color)] pt-4">
          <FieldDisplay
            label={t('admin.tenantStandards.trainingCourses.abbreviationLabel')}
            value={type.abbreviation}
          />
        </div>
      </Card>

      <AdminTenantTrainingCourseTypeEditModal
        key={type.configKey}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={(savedConfigKey) => {
          setEditOpen(false);
          if (savedConfigKey !== configKey) {
            router.replace(getAdminTenantTrainingCourseTypeDetailPath(tenantId, savedConfigKey));
            return;
          }
          void loadData();
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
