/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Archive, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantDrivingLicenseDefinitionEditModal } from '@/components/admin/AdminTenantDrivingLicenseDefinitionEditModal';
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
  type TenantDrivingLicenseDefinitionDto,
  tenantDrivingLicenseDefinitionsApi,
} from '@/lib/api/tenantDrivingLicenseDefinitions';
import { getAdminTenantDrivingLicenseDefinitionDetailPath } from '@/lib/utils/adminPaths';

interface AdminTenantDrivingLicenseManagementProps {
  tenantId: number;
  embedded?: boolean;
}

export function AdminTenantDrivingLicenseManagement({
  tenantId,
  embedded = false,
}: AdminTenantDrivingLicenseManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [loading, setLoading] = useState(true);
  const [definitions, setDefinitions] = useState<TenantDrivingLicenseDefinitionDto[]>([]);
  const [busy, setBusy] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TenantDrivingLicenseDefinitionDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TenantDrivingLicenseDefinitionDto | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<TenantDrivingLicenseDefinitionDto | null>(
    null,
  );
  const [unarchiveTarget, setUnarchiveTarget] = useState<TenantDrivingLicenseDefinitionDto | null>(
    null,
  );

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setDefinitions(await tenantDrivingLicenseDefinitionsApi.list(tenantId));
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.loadFailed'),
      );
      setDefinitions([]);
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

  const openEdit = (definition: TenantDrivingLicenseDefinitionDto) => {
    setEditing(definition);
    setFormOpen(true);
  };

  const openDetail = (definition: TenantDrivingLicenseDefinitionDto) => {
    router.push(getAdminTenantDrivingLicenseDefinitionDetailPath(tenantId, definition.configKey));
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await tenantDrivingLicenseDefinitionsApi.delete(tenantId, deleteTarget.configKey);
      showSuccess(t('admin.tenantStandards.drivingLicenses.deleted'));
      setDeleteTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!archiveTarget) return;
    setBusy(true);
    try {
      await tenantDrivingLicenseDefinitionsApi.archive(tenantId, archiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.drivingLicenses.archived'));
      setArchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!unarchiveTarget) return;
    setBusy(true);
    try {
      await tenantDrivingLicenseDefinitionsApi.unarchive(tenantId, unarchiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.drivingLicenses.unarchived'));
      setUnarchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const addButton = (
    <Button size="xs" onClick={openCreate} disabled={busy || loading} fullWidth="responsive">
      <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
      {t('admin.tenantStandards.drivingLicenses.add')}
    </Button>
  );

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : definitions.length === 0 ? (
    <EmptyStateCard>{t('admin.tenantStandards.drivingLicenses.empty')}</EmptyStateCard>
  ) : (
    <ListContainer label={t('admin.tenantStandards.drivingLicenses.listLabel')} variant="inset">
      {definitions.map((definition) => (
        <ListRow
          key={definition.configKey}
          onClick={() => openDetail(definition)}
          detailHref={getAdminTenantDrivingLicenseDefinitionDetailPath(
            tenantId,
            definition.configKey,
          )}
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
        title={t('admin.tenantStandards.drivingLicensesSectionTitle')}
        subtitle={embedded ? t('admin.tenantStandards.drivingLicensesSectionHint') : undefined}
        action={addButton}
      />
      {listContent}

      <AdminTenantDrivingLicenseDefinitionEditModal
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          void loadData();
        }}
        tenantId={tenantId}
        definition={editing}
      />

      {deleteTarget ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteTarget(null)}
          title={t('admin.tenantStandards.drivingLicenses.deleteTitle')}
          message={t('admin.tenantStandards.drivingLicenses.deleteMessage', {
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
          title={t('admin.tenantStandards.drivingLicenses.archiveTitle')}
          message={t('admin.tenantStandards.drivingLicenses.archiveMessage', {
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
          title={t('admin.tenantStandards.drivingLicenses.unarchiveTitle')}
          message={t('admin.tenantStandards.drivingLicenses.unarchiveMessage', {
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
