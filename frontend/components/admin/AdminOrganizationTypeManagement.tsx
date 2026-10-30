/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AdminOrganizationTypeEditModal } from '@/components/admin/AdminOrganizationTypeEditModal';
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
import { type OrganizationTypeDto, organizationTypesApi } from '@/lib/api/organizationTypes';
import { getAdminOrganizationTypeDetailPath } from '@/lib/utils/adminPaths';

interface AdminOrganizationTypeManagementProps {
  tenantId: number;
  embedded?: boolean;
}

export function AdminOrganizationTypeManagement({
  tenantId,
  embedded = false,
}: AdminOrganizationTypeManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [loading, setLoading] = useState(true);
  const [types, setTypes] = useState<OrganizationTypeDto[]>([]);
  const [busy, setBusy] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<OrganizationTypeDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<OrganizationTypeDto | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setTypes(await organizationTypesApi.list(tenantId));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.organizationTypes.errors.loadFailed'));
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

  const openEdit = (type: OrganizationTypeDto) => {
    setEditing(type);
    setFormOpen(true);
  };

  const openDetail = (type: OrganizationTypeDto) => {
    router.push(getAdminOrganizationTypeDetailPath(tenantId, type.id));
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await organizationTypesApi.remove(tenantId, deleteTarget.id);
      showSuccess(t('admin.organizationTypes.deleted'));
      setDeleteTarget(null);
      await loadData();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.organizationTypes.errors.deleteFailed'));
    } finally {
      setBusy(false);
    }
  };

  const addButton = (
    <Button size="xs" onClick={openCreate} disabled={busy || loading} fullWidth="responsive">
      <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
      {t('admin.organizationTypes.add')}
    </Button>
  );

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : types.length === 0 ? (
    <EmptyStateCard>{t('admin.organizationTypes.empty')}</EmptyStateCard>
  ) : (
    <ListContainer label={t('admin.organizationTypes.listTitle')} variant="inset">
      {types.map((type) => (
        <ListRow
          key={type.id}
          onClick={() => openDetail(type)}
          detailHref={getAdminOrganizationTypeDetailPath(tenantId, type.id)}
          title={type.name}
          badges={
            !type.active ? (
              <Badge tone="neutral">{t('admin.organizationTypes.inactive')}</Badge>
            ) : undefined
          }
          actions={
            <div className="flex items-center gap-1">
              <IconButton
                icon={<Pencil className="h-4 w-4" />}
                aria-label={t('common.edit')}
                onClick={(event) => {
                  event.stopPropagation();
                  openEdit(type);
                }}
                disabled={busy}
              />
              <IconButton
                icon={<Trash2 className="h-4 w-4" />}
                aria-label={t('common.delete')}
                onClick={(event) => {
                  event.stopPropagation();
                  setDeleteTarget(type);
                }}
                disabled={busy}
              />
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
        title={t('admin.organizationTypes.title')}
        subtitle={embedded ? t('admin.organizationTypes.subtitle') : undefined}
        action={addButton}
      />
      {listContent}

      <AdminOrganizationTypeEditModal
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          void loadData();
        }}
        tenantId={tenantId}
        organizationType={editing}
      />

      {deleteTarget ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteTarget(null)}
          title={t('common.delete')}
          message={t('admin.organizationTypes.deleteConfirm')}
          confirmText={t('common.delete')}
          cancelText={t('common.cancel')}
          onConfirm={() => void handleDelete()}
          isLoading={busy}
        />
      ) : null}
    </>
  );
}
