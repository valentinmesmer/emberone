/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Archive, Pencil, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantDrivingLicenseDefinitionEditModal } from '@/components/admin/AdminTenantDrivingLicenseDefinitionEditModal';
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
  type TenantDrivingLicenseDefinitionDto,
  tenantDrivingLicenseDefinitionsApi,
} from '@/lib/api/tenantDrivingLicenseDefinitions';
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

  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [definition, setDefinition] = useState<TenantDrivingLicenseDefinitionDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [unarchiveOpen, setUnarchiveOpen] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    try {
      setDefinition(await tenantDrivingLicenseDefinitionsApi.get(tenantId, configKey));
    } catch {
      setLoadFailed(true);
      setDefinition(null);
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
    if (!definition) return;
    setBusy(true);
    try {
      await tenantDrivingLicenseDefinitionsApi.delete(tenantId, definition.configKey);
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
      showSuccess(t('admin.tenantStandards.drivingLicenses.unarchived'));
      setUnarchiveOpen(false);
      await loadData();
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
            definition.archived ? (
              <StandardDetailAction
                label={t('common.unarchive')}
                icon={<RotateCcw className="h-4 w-4" aria-hidden />}
                onClick={() => setUnarchiveOpen(true)}
                disabled={busy}
              />
            ) : (
              <>
                {definition.deletable ? (
                  <StandardDetailAction
                    label={t('common.delete')}
                    icon={<Trash2 className="h-4 w-4" aria-hidden />}
                    onClick={() => setDeleteOpen(true)}
                    disabled={busy}
                  />
                ) : null}
                {definition.archivable ? (
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
            label={t('admin.tenantStandards.drivingLicenses.abbreviationLabel')}
            value={definition.abbreviation}
          />
        </div>
      </Card>

      <AdminTenantDrivingLicenseDefinitionEditModal
        key={definition.configKey}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={(savedConfigKey) => {
          setEditOpen(false);
          if (savedConfigKey !== configKey) {
            router.replace(
              getAdminTenantDrivingLicenseDefinitionDetailPath(tenantId, savedConfigKey),
            );
            return;
          }
          void loadData();
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
