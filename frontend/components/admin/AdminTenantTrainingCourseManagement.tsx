/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Archive, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantTrainingCourseTypeEditModal } from '@/components/admin/AdminTenantTrainingCourseTypeEditModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  type TenantTrainingCourseTypeDto,
  tenantTrainingCourseTypesApi,
} from '@/lib/api/tenantTrainingCourseTypes';
import { getAdminTenantTrainingCourseTypeDetailPath } from '@/lib/utils/adminPaths';

interface AdminTenantTrainingCourseManagementProps {
  tenantId: number;
  embedded?: boolean;
}

export function AdminTenantTrainingCourseManagement({
  tenantId,
  embedded = false,
}: AdminTenantTrainingCourseManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [loading, setLoading] = useState(true);
  const [types, setTypes] = useState<TenantTrainingCourseTypeDto[]>([]);
  const [busy, setBusy] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TenantTrainingCourseTypeDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TenantTrainingCourseTypeDto | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<TenantTrainingCourseTypeDto | null>(null);
  const [unarchiveTarget, setUnarchiveTarget] = useState<TenantTrainingCourseTypeDto | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setTypes(await tenantTrainingCourseTypesApi.list(tenantId));
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.loadFailed'),
      );
      setTypes([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId, showError, t]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (type: TenantTrainingCourseTypeDto) => {
    setEditing(type);
    setFormOpen(true);
  };

  const openDetail = (type: TenantTrainingCourseTypeDto) => {
    router.push(getAdminTenantTrainingCourseTypeDetailPath(tenantId, type.configKey));
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await tenantTrainingCourseTypesApi.delete(tenantId, deleteTarget.configKey);
      showSuccess(t('admin.tenantStandards.trainingCourses.deleted'));
      setDeleteTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!archiveTarget) return;
    setBusy(true);
    try {
      await tenantTrainingCourseTypesApi.archive(tenantId, archiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.trainingCourses.archived'));
      setArchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!unarchiveTarget) return;
    setBusy(true);
    try {
      await tenantTrainingCourseTypesApi.unarchive(tenantId, unarchiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.trainingCourses.unarchived'));
      setUnarchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.trainingCourses.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const addButton = (
    <Button size="xs" onClick={openCreate} disabled={busy || loading} fullWidth="responsive">
      <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
      {t('admin.tenantStandards.trainingCourses.add')}
    </Button>
  );

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : types.length === 0 ? (
    <EmptyStateCard>{t('admin.tenantStandards.trainingCourses.empty')}</EmptyStateCard>
  ) : (
    <ListContainer label={t('admin.tenantStandards.trainingCourses.listLabel')} variant="inset">
      {types.map((type) => (
        <ListRow
          key={type.configKey}
          onClick={() => openDetail(type)}
          detailHref={getAdminTenantTrainingCourseTypeDetailPath(tenantId, type.configKey)}
          title={type.name}
          tone={type.archived ? 'neutral' : 'default'}
          badges={
            type.archived ? (
              <Badge tone="neutral">{t('admin.tenantStandards.archivedBadge')}</Badge>
            ) : null
          }
          actions={
            <div className="flex items-center gap-1">
              {type.archived ? (
                <IconButton
                  icon={<RotateCcw className="h-4 w-4" />}
                  aria-label={t('common.unarchive')}
                  onClick={(event) => {
                    event.stopPropagation();
                    setUnarchiveTarget(type);
                  }}
                  disabled={busy}
                />
              ) : (
                <>
                  <IconButton
                    icon={<Pencil className="h-4 w-4" />}
                    aria-label={t('common.edit')}
                    onClick={(event) => {
                      event.stopPropagation();
                      openEdit(type);
                    }}
                    disabled={busy}
                  />
                  {type.deletable ? (
                    <IconButton
                      icon={<Trash2 className="h-4 w-4" />}
                      aria-label={t('common.delete')}
                      onClick={(event) => {
                        event.stopPropagation();
                        setDeleteTarget(type);
                      }}
                      disabled={busy}
                    />
                  ) : null}
                  {type.archivable ? (
                    <IconButton
                      icon={<Archive className="h-4 w-4" />}
                      aria-label={t('common.archive')}
                      onClick={(event) => {
                        event.stopPropagation();
                        setArchiveTarget(type);
                      }}
                      disabled={busy}
                    />
                  ) : null}
                </>
              )}
            </div>
          }
        />
      ))}
    </ListContainer>
  );

  return (
    <>
      <SectionHeader
        className="mb-3"
        title={t('admin.tenantStandards.trainingCoursesSectionTitle')}
        subtitle={embedded ? t('admin.tenantStandards.trainingCoursesSectionHint') : undefined}
        action={addButton}
      />
      {listContent}

      <AdminTenantTrainingCourseTypeEditModal
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          void loadData();
        }}
        tenantId={tenantId}
        type={editing}
      />

      {deleteTarget ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteTarget(null)}
          title={t('admin.tenantStandards.trainingCourses.deleteTitle')}
          message={t('admin.tenantStandards.trainingCourses.deleteMessage', {
            name: deleteTarget.name,
          })}
          confirmText={t('common.delete')}
          onConfirm={() => void handleDelete()}
          isLoading={busy}
        />
      ) : null}

      {archiveTarget ? (
        <ConfirmDialog
          isOpen
          onClose={() => setArchiveTarget(null)}
          title={t('admin.tenantStandards.trainingCourses.archiveTitle')}
          message={t('admin.tenantStandards.trainingCourses.archiveMessage', {
            name: archiveTarget.name,
          })}
          confirmText={t('common.archive')}
          onConfirm={() => void handleArchive()}
          isLoading={busy}
        />
      ) : null}

      {unarchiveTarget ? (
        <ConfirmDialog
          isOpen
          onClose={() => setUnarchiveTarget(null)}
          title={t('admin.tenantStandards.trainingCourses.unarchiveTitle')}
          message={t('admin.tenantStandards.trainingCourses.unarchiveMessage', {
            name: unarchiveTarget.name,
          })}
          confirmText={t('common.unarchive')}
          onConfirm={() => void handleUnarchive()}
          isLoading={busy}
        />
      ) : null}
    </>
  );
}
