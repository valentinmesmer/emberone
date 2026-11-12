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
import { AdminTenantDrivingLicenseDefinitionEditModal } from '@/components/admin/AdminTenantDrivingLicenseDefinitionEditModal';
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
import { tenantDrivingLicenseDefinitionsApi } from '@/lib/api/tenantDrivingLicenseDefinitions';
import {
  invalidateAdminTenantCatalog,
  markAdminTenantCatalogStale,
  useTenantDrivingLicenseDefinition,
} from '@/lib/store';
import {
  getAdminTenantDrivingLicenseDefinitionDetailPath,
  getAdminTenantStandardsPath,
} from '@/lib/utils/adminPaths';

interface AdminTenantDrivingLicenseDefinitionDetailViewProps {
  tenantId: number;
  configKey: string;
}

export function AdminTenantDrivingLicenseDefinitionDetailView({
  tenantId,
  configKey,
}: AdminTenantDrivingLicenseDefinitionDetailViewProps) {
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
    data: definition = null,
    isPending: loading,
    isError: loadFailed,
  } = useTenantDrivingLicenseDefinition(tenantId, configKey);
  const reload = () => void invalidateAdminTenantCatalog(queryClient, 'drivingLicenses', tenantId);
  const staleOnLeave = () =>
    void markAdminTenantCatalogStale(queryClient, 'drivingLicenses', tenantId);

  const navigateBack = () => {
    router.push(getAdminTenantStandardsPath(tenantId));
  };

  const handleDelete = async () => {
    if (!definition) return;
    setBusy(true);
    try {
      await tenantDrivingLicenseDefinitionsApi.delete(tenantId, definition.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.drivingLicenses.deleted'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!definition) return;
    setBusy(true);
    try {
      await tenantDrivingLicenseDefinitionsApi.archive(tenantId, definition.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.drivingLicenses.archived'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!definition) return;
    setBusy(true);
    try {
      await tenantDrivingLicenseDefinitionsApi.unarchive(tenantId, definition.configKey);
      reload();
      showSuccess(t('admin.tenantStandards.drivingLicenses.unarchived'));
      setUnarchiveOpen(false);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.drivingLicenses.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !definition) {
    return <NoticeCard>{t('admin.tenantStandards.drivingLicenses.loadFailed')}</NoticeCard>;
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          className="mb-2"
          title={definition.name}
          badges={
            definition.archived ? (
              <Badge tone="neutral" pill>
                {t('admin.tenantStandards.archivedBadge')}
              </Badge>
            ) : (
              <Badge tone={definition.active ? 'success' : 'neutral'} pill>
                {definition.active
                  ? t('admin.standards.active')
                  : t('admin.workflowDefinitions.tenantDefaultDisabled')}
              </Badge>
            )
          }
          actions={
            !definition.archived ? (
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
            label={t('admin.tenantStandards.drivingLicenses.abbreviationLabel')}
            value={definition.abbreviation}
          />
        </div>
      </Card>

      <ActionsSection>
        {definition.archived ? (
          <ListRow
            leading={<ActionRowIcon icon={RotateCcw} />}
            title={t('common.unarchive')}
            subtitle={t('common.unarchiveHint')}
            onClick={busy ? undefined : () => setUnarchiveOpen(true)}
          />
        ) : null}
        {!definition.archived && definition.archivable ? (
          <ListRow
            leading={<ActionRowIcon icon={Archive} />}
            title={t('common.archive')}
            subtitle={t('common.archiveHint')}
            onClick={busy ? undefined : () => setArchiveOpen(true)}
          />
        ) : null}
        {!definition.archived && definition.deletable ? (
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('common.delete')}
            subtitle={t('common.deleteHint')}
            onClick={busy ? undefined : () => setDeleteOpen(true)}
          />
        ) : null}
      </ActionsSection>

      <AdminTenantDrivingLicenseDefinitionEditModal
        key={definition.configKey}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={(savedConfigKey) => {
          setEditOpen(false);
          if (savedConfigKey !== configKey) {
            staleOnLeave();
            router.replace(
              getAdminTenantDrivingLicenseDefinitionDetailPath(tenantId, savedConfigKey),
            );
            return;
          }
          reload();
        }}
        tenantId={tenantId}
        definition={definition}
      />

      {deleteOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteOpen(false)}
          title={t('admin.tenantStandards.drivingLicenses.deleteTitle')}
          message={t('admin.tenantStandards.drivingLicenses.deleteMessage', {
            name: definition.name,
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
          title={t('admin.tenantStandards.drivingLicenses.archiveTitle')}
          message={t('admin.tenantStandards.drivingLicenses.archiveMessage', {
            name: definition.name,
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
          title={t('admin.tenantStandards.drivingLicenses.unarchiveTitle')}
          message={t('admin.tenantStandards.drivingLicenses.unarchiveMessage', {
            name: definition.name,
          })}
          confirmText={t('common.unarchive')}
          onConfirm={() => void handleUnarchive()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
