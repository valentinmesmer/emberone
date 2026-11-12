/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Archive, Download, Pencil, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { AdminTenantOnboardingDocumentEditModal } from '@/components/admin/AdminTenantOnboardingDocumentEditModal';
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
import { tenantOnboardingDocumentsApi } from '@/lib/api/tenantOnboardingDocuments';
import {
  invalidateAdminTenantCatalog,
  markAdminTenantCatalogStale,
  useTenantOnboardingDocument,
} from '@/lib/store';
import { downloadBlob } from '@/lib/utils/downloadBlob';
import { formatFileSize } from '@/lib/utils/userDocumentFormat';
import { getAdminTenantStandardsPath } from '@/lib/utils/adminPaths';

interface AdminTenantOnboardingDocumentDetailViewProps {
  tenantId: number;
  configKey: string;
}

export function AdminTenantOnboardingDocumentDetailView({
  tenantId,
  configKey,
}: AdminTenantOnboardingDocumentDetailViewProps) {
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
    data: item = null,
    isPending: loading,
    isError: loadFailed,
  } = useTenantOnboardingDocument(tenantId, configKey);
  const reload = () =>
    void invalidateAdminTenantCatalog(queryClient, 'onboardingDocuments', tenantId);
  const staleOnLeave = () =>
    void markAdminTenantCatalogStale(queryClient, 'onboardingDocuments', tenantId);

  const navigateBack = () => {
    router.push(getAdminTenantStandardsPath(tenantId));
  };

  const handleDelete = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantOnboardingDocumentsApi.delete(tenantId, item.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.onboardingDocuments.deleted'));
      navigateBack();
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
    if (!item) return;
    setBusy(true);
    try {
      await tenantOnboardingDocumentsApi.archive(tenantId, item.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.onboardingDocuments.archived'));
      navigateBack();
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
    if (!item) return;
    setBusy(true);
    try {
      await tenantOnboardingDocumentsApi.unarchive(tenantId, item.configKey);
      reload();
      showSuccess(t('admin.tenantStandards.onboardingDocuments.unarchived'));
      setUnarchiveOpen(false);
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

  const handleDownloadTemplate = async () => {
    if (!item?.hasTemplate) return;
    setBusy(true);
    try {
      const blob = await tenantOnboardingDocumentsApi.downloadTemplate(tenantId, item.configKey);
      downloadBlob(blob, item.templateFileName ?? 'document');
    } catch (e) {
      showError(
        e instanceof Error
          ? e.message
          : t('admin.tenantStandards.onboardingDocuments.templateDownloadFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !item) {
    return <NoticeCard>{t('admin.tenantStandards.onboardingDocuments.loadFailed')}</NoticeCard>;
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          className="mb-2"
          title={item.name}
          badges={
            item.archived ? (
              <Badge tone="neutral" pill>
                {t('admin.tenantStandards.archivedBadge')}
              </Badge>
            ) : (
              <Badge tone={item.active ? 'success' : 'neutral'} pill>
                {item.active ? t('admin.standards.active') : t('common.inactive')}
              </Badge>
            )
          }
          actions={
            !item.archived ? (
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
            label={t('admin.tenantStandards.onboardingDocuments.abbreviationLabel')}
            value={item.abbreviation}
          />
          <FieldDisplay
            label={t('admin.tenantStandards.onboardingDocuments.descriptionLabel')}
            value={item.description}
            fallback={t('common.notAvailable')}
            multiline
          />
          <FieldDisplay
            label={t('admin.tenantStandards.onboardingDocuments.fulfillmentModeLabel')}
            value={t(
              `admin.tenantStandards.onboardingDocuments.fulfillmentMode.${item.fulfillmentMode}`,
            )}
          />
          <FieldDisplay
            label={t('admin.tenantStandards.onboardingDocuments.mandatoryLabel')}
            value={item.mandatory ? t('common.yes') : t('common.no')}
          />
          <FieldDisplay
            label={t('admin.tenantStandards.onboardingDocuments.templateLabel')}
            value={
              item.hasTemplate
                ? `${item.templateFileName ?? ''}${item.templateSizeBytes != null ? ` (${formatFileSize(item.templateSizeBytes)})` : ''}`
                : t('admin.tenantStandards.onboardingDocuments.missingTemplateBadge')
            }
          />
        </div>
      </Card>

      <ActionsSection>
        {item.hasTemplate ? (
          <ListRow
            leading={<ActionRowIcon icon={Download} />}
            title={t('admin.tenantStandards.onboardingDocuments.templateDownload')}
            subtitle={item.templateFileName ?? undefined}
            onClick={busy ? undefined : () => void handleDownloadTemplate()}
          />
        ) : null}
        {item.archived ? (
          <ListRow
            leading={<ActionRowIcon icon={RotateCcw} />}
            title={t('common.unarchive')}
            subtitle={t('common.unarchiveHint')}
            onClick={busy ? undefined : () => setUnarchiveOpen(true)}
          />
        ) : null}
        {!item.archived && item.archivable ? (
          <ListRow
            leading={<ActionRowIcon icon={Archive} />}
            title={t('common.archive')}
            subtitle={t('common.archiveHint')}
            onClick={busy ? undefined : () => setArchiveOpen(true)}
          />
        ) : null}
        {!item.archived && item.deletable ? (
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('common.delete')}
            subtitle={t('common.deleteHint')}
            onClick={busy ? undefined : () => setDeleteOpen(true)}
          />
        ) : null}
      </ActionsSection>

      <AdminTenantOnboardingDocumentEditModal
        key={item.configKey}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={() => {
          setEditOpen(false);
          reload();
        }}
        tenantId={tenantId}
        document={item}
      />

      {deleteOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteOpen(false)}
          title={t('admin.tenantStandards.onboardingDocuments.deleteTitle')}
          message={t('admin.tenantStandards.onboardingDocuments.deleteMessage', {
            name: item.name,
          })}
          confirmText={t('common.delete')}
          onConfirm={() => void handleDelete()}
          isLoading={busy}
        />
      ) : null}

      {archiveOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setArchiveOpen(false)}
          title={t('admin.tenantStandards.onboardingDocuments.archiveTitle')}
          message={t('admin.tenantStandards.onboardingDocuments.archiveMessage', {
            name: item.name,
          })}
          confirmText={t('common.archive')}
          onConfirm={() => void handleArchive()}
          isLoading={busy}
        />
      ) : null}

      {unarchiveOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setUnarchiveOpen(false)}
          title={t('admin.tenantStandards.onboardingDocuments.unarchiveTitle')}
          message={t('admin.tenantStandards.onboardingDocuments.unarchiveMessage', {
            name: item.name,
          })}
          confirmText={t('common.unarchive')}
          onConfirm={() => void handleUnarchive()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
