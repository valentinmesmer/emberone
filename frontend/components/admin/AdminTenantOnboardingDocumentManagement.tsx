/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Archive, Pencil, Plus, RotateCcw, Trash2 } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantOnboardingDocumentEditModal } from '@/components/admin/AdminTenantOnboardingDocumentEditModal';
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
  type TenantOnboardingDocumentDefinitionDto,
  tenantOnboardingDocumentsApi,
} from '@/lib/api/tenantOnboardingDocuments';

interface AdminTenantOnboardingDocumentManagementProps {
  tenantId: number;
  embedded?: boolean;
}

export function AdminTenantOnboardingDocumentManagement({
  tenantId,
  embedded = false,
}: AdminTenantOnboardingDocumentManagementProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();

  const [loading, setLoading] = useState(true);
  const [documents, setDocuments] = useState<TenantOnboardingDocumentDefinitionDto[]>([]);
  const [busy, setBusy] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TenantOnboardingDocumentDefinitionDto | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<TenantOnboardingDocumentDefinitionDto | null>(
    null,
  );
  const [archiveTarget, setArchiveTarget] = useState<TenantOnboardingDocumentDefinitionDto | null>(
    null,
  );
  const [unarchiveTarget, setUnarchiveTarget] =
    useState<TenantOnboardingDocumentDefinitionDto | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setDocuments(await tenantOnboardingDocumentsApi.list(tenantId));
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.onboardingDocuments.loadFailed'),
      );
      setDocuments([]);
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

  const openEdit = (document: TenantOnboardingDocumentDefinitionDto) => {
    setEditing(document);
    setFormOpen(true);
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    setBusy(true);
    try {
      await tenantOnboardingDocumentsApi.delete(tenantId, deleteTarget.configKey);
      showSuccess(t('admin.tenantStandards.onboardingDocuments.deleted'));
      setDeleteTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : t('admin.tenantStandards.onboardingDocuments.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!archiveTarget) return;
    setBusy(true);
    try {
      await tenantOnboardingDocumentsApi.archive(tenantId, archiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.onboardingDocuments.archived'));
      setArchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : t('admin.tenantStandards.onboardingDocuments.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!unarchiveTarget) return;
    setBusy(true);
    try {
      await tenantOnboardingDocumentsApi.unarchive(tenantId, unarchiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.onboardingDocuments.unarchived'));
      setUnarchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : t('admin.tenantStandards.onboardingDocuments.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const addButton = (
    <Button size="xs" onClick={openCreate} disabled={busy || loading} fullWidth="responsive">
      <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
      {t('admin.tenantStandards.onboardingDocuments.add')}
    </Button>
  );

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : documents.length === 0 ? (
    <EmptyStateCard>{t('admin.tenantStandards.onboardingDocuments.empty')}</EmptyStateCard>
  ) : (
    <ListContainer label={t('admin.tenantStandards.onboardingDocuments.listLabel')} variant="inset">
      {documents.map((document) => (
        <ListRow
          key={document.configKey}
          onClick={() => openEdit(document)}
          title={document.name}
          tone={document.archived ? 'neutral' : 'default'}
          badges={
            <>
              {document.archived ? (
                <Badge tone="neutral">{t('admin.tenantStandards.archivedBadge')}</Badge>
              ) : null}
              {!document.hasTemplate ? (
                <Badge tone="danger">
                  {t('admin.tenantStandards.onboardingDocuments.missingTemplateBadge')}
                </Badge>
              ) : null}
              {!document.active && !document.archived ? (
                <Badge tone="neutral">
                  {t('admin.tenantStandards.onboardingDocuments.inactiveBadge')}
                </Badge>
              ) : null}
            </>
          }
          actions={
            <div className="flex items-center gap-1">
              {document.archived ? (
                <IconButton
                  icon={<RotateCcw className="h-4 w-4" />}
                  aria-label={t('common.unarchive')}
                  onClick={(event) => {
                    event.stopPropagation();
                    setUnarchiveTarget(document);
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
                      openEdit(document);
                    }}
                    disabled={busy}
                  />
                  {document.deletable ? (
                    <IconButton
                      icon={<Trash2 className="h-4 w-4" />}
                      aria-label={t('common.delete')}
                      onClick={(event) => {
                        event.stopPropagation();
                        setDeleteTarget(document);
                      }}
                      disabled={busy}
                    />
                  ) : null}
                  {document.archivable ? (
                    <IconButton
                      icon={<Archive className="h-4 w-4" />}
                      aria-label={t('common.archive')}
                      onClick={(event) => {
                        event.stopPropagation();
                        setArchiveTarget(document);
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
        title={t('admin.tenantStandards.onboardingDocumentsSectionTitle')}
        subtitle={embedded ? t('admin.tenantStandards.onboardingDocumentsSectionHint') : undefined}
        action={addButton}
      />
      {listContent}

      <AdminTenantOnboardingDocumentEditModal
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          void loadData();
        }}
        tenantId={tenantId}
        document={editing}
      />

      <ConfirmDialog
        isOpen={deleteTarget != null}
        onClose={() => setDeleteTarget(null)}
        title={t('admin.tenantStandards.onboardingDocuments.deleteTitle')}
        message={t('admin.tenantStandards.onboardingDocuments.deleteMessage', {
          name: deleteTarget?.name ?? '',
        })}
        confirmText={t('common.delete')}
        onConfirm={() => void handleDelete()}
        isLoading={busy}
        variant="danger"
      />

      <ConfirmDialog
        isOpen={archiveTarget != null}
        onClose={() => setArchiveTarget(null)}
        title={t('admin.tenantStandards.onboardingDocuments.archiveTitle')}
        message={t('admin.tenantStandards.onboardingDocuments.archiveMessage', {
          name: archiveTarget?.name ?? '',
        })}
        confirmText={t('common.archive')}
        onConfirm={() => void handleArchive()}
        isLoading={busy}
      />

      <ConfirmDialog
        isOpen={unarchiveTarget != null}
        onClose={() => setUnarchiveTarget(null)}
        title={t('admin.tenantStandards.onboardingDocuments.unarchiveTitle')}
        message={t('admin.tenantStandards.onboardingDocuments.unarchiveMessage', {
          name: unarchiveTarget?.name ?? '',
        })}
        confirmText={t('common.unarchive')}
        onConfirm={() => void handleUnarchive()}
        isLoading={busy}
      />
    </>
  );
}
