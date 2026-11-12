/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Archive, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { AdminTenantCertificationEditModal } from '@/components/admin/AdminTenantCertificationEditModal';
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
  type TenantCertDefinitionDto,
  tenantCertificationsApi,
} from '@/lib/api/tenantCertifications';
import {
  invalidateAdminTenantCatalog,
  useQueryErrorToast,
  useTenantCertifications,
} from '@/lib/store';
import { getAdminTenantCertificationDetailPath } from '@/lib/utils/adminPaths';

interface AdminTenantCertificationManagementProps {
  tenantId: number;
  embedded?: boolean;
}

export function AdminTenantCertificationManagement({
  tenantId,
  embedded = false,
}: AdminTenantCertificationManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [busy, setBusy] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TenantCertDefinitionDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TenantCertDefinitionDto | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<TenantCertDefinitionDto | null>(null);
  const [unarchiveTarget, setUnarchiveTarget] = useState<TenantCertDefinitionDto | null>(null);

  const queryClient = useQueryClient();
  const {
    data: definitions = [],
    isPending: loading,
    error: loadError,
  } = useTenantCertifications(tenantId);
  useQueryErrorToast(loadError, { messageKey: 'admin.tenantStandards.certifications.loadFailed' });
  const reload = () => void invalidateAdminTenantCatalog(queryClient, 'certifications', tenantId);

  const openCreate = () => {
    setEditing(null);
    setFormOpen(true);
  };

  const openEdit = (definition: TenantCertDefinitionDto) => {
    setEditing(definition);
    setFormOpen(true);
  };

  const openDetail = (definition: TenantCertDefinitionDto) => {
    router.push(getAdminTenantCertificationDetailPath(tenantId, definition.configKey));
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await tenantCertificationsApi.delete(tenantId, deleteTarget.configKey);
      showSuccess(t('admin.tenantStandards.certifications.deleted'));
      setDeleteTarget(null);
      reload();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.certifications.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!archiveTarget) return;
    setBusy(true);
    try {
      await tenantCertificationsApi.archive(tenantId, archiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.certifications.archived'));
      setArchiveTarget(null);
      reload();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.certifications.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!unarchiveTarget) return;
    setBusy(true);
    try {
      await tenantCertificationsApi.unarchive(tenantId, unarchiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.certifications.unarchived'));
      setUnarchiveTarget(null);
      reload();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.certifications.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const addButton = (
    <Button size="xs" onClick={openCreate} disabled={busy || loading} fullWidth="responsive">
      <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
      {t('admin.tenantStandards.certifications.add')}
    </Button>
  );

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : definitions.length === 0 ? (
    <EmptyStateCard>{t('admin.tenantStandards.certifications.empty')}</EmptyStateCard>
  ) : (
    <ListContainer label={t('admin.tenantStandards.certifications.listLabel')} variant="inset">
      {definitions.map((definition) => (
        <ListRow
          key={definition.configKey}
          onClick={() => openDetail(definition)}
          detailHref={getAdminTenantCertificationDetailPath(tenantId, definition.configKey)}
          title={definition.name}
          tone={definition.archived ? 'neutral' : 'default'}
          badges={
            definition.archived ? (
              <Badge tone="neutral">{t('admin.tenantStandards.archivedBadge')}</Badge>
            ) : null
          }
          actions={
            <div className="flex items-center gap-1">
              {definition.archived ? (
                <IconButton
                  icon={<RotateCcw className="h-4 w-4" />}
                  aria-label={t('common.unarchive')}
                  onClick={(event) => {
                    event.stopPropagation();
                    setUnarchiveTarget(definition);
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
                      openEdit(definition);
                    }}
                    disabled={busy}
                  />
                  {definition.deletable ? (
                    <IconButton
                      icon={<Trash2 className="h-4 w-4" />}
                      aria-label={t('common.delete')}
                      onClick={(event) => {
                        event.stopPropagation();
                        setDeleteTarget(definition);
                      }}
                      disabled={busy}
                    />
                  ) : null}
                  {definition.archivable ? (
                    <IconButton
                      icon={<Archive className="h-4 w-4" />}
                      aria-label={t('common.archive')}
                      onClick={(event) => {
                        event.stopPropagation();
                        setArchiveTarget(definition);
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
        title={t('admin.tenantStandards.certificationsSectionTitle')}
        subtitle={embedded ? t('admin.tenantStandards.certificationsSectionHint') : undefined}
        action={addButton}
      />
      {listContent}

      <AdminTenantCertificationEditModal
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          reload();
        }}
        tenantId={tenantId}
        definition={editing}
      />

      {deleteTarget ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteTarget(null)}
          title={t('admin.tenantStandards.certifications.deleteTitle')}
          message={t('admin.tenantStandards.certifications.deleteMessage', {
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
          title={t('admin.tenantStandards.certifications.archiveTitle')}
          message={t('admin.tenantStandards.certifications.archiveMessage', {
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
          title={t('admin.tenantStandards.certifications.unarchiveTitle')}
          message={t('admin.tenantStandards.certifications.unarchiveMessage', {
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
