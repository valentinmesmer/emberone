/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Archive, Pencil, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantCertificationEditModal } from '@/components/admin/AdminTenantCertificationEditModal';
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
import {
  type TenantCertDefinitionDto,
  tenantCertificationsApi,
} from '@/lib/api/tenantCertifications';
import {
  getAdminTenantCertificationDetailPath,
  getAdminTenantStandardsPath,
} from '@/lib/utils/adminPaths';

interface AdminTenantCertificationDetailViewProps {
  tenantId: number;
  configKey: string;
}

export function AdminTenantCertificationDetailView({
  tenantId,
  configKey,
}: AdminTenantCertificationDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [definition, setDefinition] = useState<TenantCertDefinitionDto | null>(null);
  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [unarchiveOpen, setUnarchiveOpen] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    try {
      setDefinition(await tenantCertificationsApi.get(tenantId, configKey));
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
      await tenantCertificationsApi.delete(tenantId, definition.configKey);
      showSuccess(t('admin.tenantStandards.certifications.deleted'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.certifications.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!definition) return;
    setBusy(true);
    try {
      await tenantCertificationsApi.archive(tenantId, definition.configKey);
      showSuccess(t('admin.tenantStandards.certifications.archived'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.certifications.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!definition) return;
    setBusy(true);
    try {
      await tenantCertificationsApi.unarchive(tenantId, definition.configKey);
      showSuccess(t('admin.tenantStandards.certifications.unarchived'));
      setUnarchiveOpen(false);
      await loadData();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.certifications.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !definition) {
    return <NoticeCard>{t('admin.tenantStandards.certifications.loadFailed')}</NoticeCard>;
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
            label={t('admin.tenantStandards.certifications.abbreviationLabel')}
            value={definition.abbreviation}
          />
          {definition.description ? (
            <FieldDisplay label={t('admin.standards.description')} value={definition.description} />
          ) : null}
          <FieldDisplay
            label={t('admin.tenantStandards.certifications.evidenceTypesLabel')}
            value={
              definition.evidenceTypes?.length
                ? definition.evidenceTypes
                    .map((type) => t(`admin.tenantStandards.certifications.evidenceType.${type}`))
                    .join(', ')
                : t('admin.tenantStandards.certifications.evidenceTypesNone')
            }
          />
          <FieldDisplay
            label={t('admin.tenantStandards.certifications.intervalDaysLabel')}
            value={
              definition.intervalDays != null
                ? t('admin.tenantStandards.certifications.intervalDays', {
                    days: definition.intervalDays,
                  })
                : t('admin.tenantStandards.certifications.noInterval')
            }
          />
          <FieldDisplay
            label={t('admin.tenantStandards.certifications.reminderDaysLabel')}
            value={String(definition.reminderDaysBeforeExpiry)}
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

      <AdminTenantCertificationEditModal
        key={definition.configKey}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={(savedConfigKey) => {
          setEditOpen(false);
          if (savedConfigKey !== configKey) {
            router.replace(getAdminTenantCertificationDetailPath(tenantId, savedConfigKey));
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
          title={t('admin.tenantStandards.certifications.deleteTitle')}
          message={t('admin.tenantStandards.certifications.deleteMessage', {
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
          title={t('admin.tenantStandards.certifications.archiveTitle')}
          message={t('admin.tenantStandards.certifications.archiveMessage', {
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
          title={t('admin.tenantStandards.certifications.unarchiveTitle')}
          message={t('admin.tenantStandards.certifications.unarchiveMessage', {
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
