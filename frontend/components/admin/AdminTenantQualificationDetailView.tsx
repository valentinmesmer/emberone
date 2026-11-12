/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Archive, Pencil, RotateCcw, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState, useMemo } from 'react';
import { AdminTenantQualificationEditModal } from '@/components/admin/AdminTenantQualificationEditModal';
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
  type QualificationRequirementMode,
  tenantQualificationsApi,
} from '@/lib/api/tenantQualifications';
import {
  invalidateAdminTenantCatalog,
  markAdminTenantCatalogStale,
  useTenantCertifications,
  useTenantDrivingLicenseDefinitions,
  useTenantQualification,
  useTenantTrainingCourseTypes,
} from '@/lib/store';
import { getAdminTenantStandardsPath } from '@/lib/utils/adminPaths';

interface AdminTenantQualificationDetailViewProps {
  tenantId: number;
  configKey: string;
}

export function AdminTenantQualificationDetailView({
  tenantId,
  configKey,
}: AdminTenantQualificationDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [unarchiveOpen, setUnarchiveOpen] = useState(false);

  const queryClient = useQueryClient();
  const itemQuery = useTenantQualification(tenantId, configKey);
  const coursesQuery = useTenantTrainingCourseTypes(tenantId);
  const certsQuery = useTenantCertifications(tenantId);
  const licensesQuery = useTenantDrivingLicenseDefinitions(tenantId);
  const item = itemQuery.data ?? null;
  const loading =
    itemQuery.isPending ||
    coursesQuery.isPending ||
    certsQuery.isPending ||
    licensesQuery.isPending;
  const loadFailed =
    itemQuery.isError || coursesQuery.isError || certsQuery.isError || licensesQuery.isError;
  const nameByKey = useMemo(
    () =>
      new Map(
        [
          ...(coursesQuery.data ?? []),
          ...(certsQuery.data ?? []),
          ...(licensesQuery.data ?? []),
        ].map((entry) => [entry.configKey, entry.name]),
      ),
    [coursesQuery.data, certsQuery.data, licensesQuery.data],
  );
  const reload = () => void invalidateAdminTenantCatalog(queryClient, 'qualifications', tenantId);
  const staleOnLeave = () =>
    void markAdminTenantCatalogStale(queryClient, 'qualifications', tenantId);

  const navigateBack = () => {
    router.push(getAdminTenantStandardsPath(tenantId));
  };

  const handleDelete = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantQualificationsApi.delete(tenantId, item.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.qualifications.deleted'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.qualifications.deleteFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleArchive = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantQualificationsApi.archive(tenantId, item.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.qualifications.archived'));
      navigateBack();
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.qualifications.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantQualificationsApi.unarchive(tenantId, item.configKey);
      reload();
      showSuccess(t('admin.tenantStandards.qualifications.unarchived'));
      setUnarchiveOpen(false);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.qualifications.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const requirementValue = (keys: string[], mode: QualificationRequirementMode) =>
    keys.length === 0
      ? t('common.notAvailable')
      : `${keys.map((key) => nameByKey.get(key) ?? key).join(', ')}${
          keys.length > 1
            ? ` (${mode === 'ALL' ? t('admin.tenantStandards.qualifications.modeAll') : t('admin.tenantStandards.qualifications.modeAny')})`
            : ''
        }`;

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !item) {
    return <NoticeCard>{t('admin.tenantStandards.qualifications.loadFailed')}</NoticeCard>;
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
            label={t('admin.tenantStandards.qualifications.abbreviationLabel')}
            value={item.abbreviation}
          />
          {item.description ? (
            <FieldDisplay
              label={t('admin.standards.description')}
              value={item.description}
              multiline
            />
          ) : null}
          <FieldDisplay
            label={t('admin.tenantStandards.qualifications.trainingCourses')}
            value={requirementValue(
              item.trainingCourseConfigKeys,
              item.trainingCourseRequirementMode,
            )}
          />
          <FieldDisplay
            label={t('admin.tenantStandards.qualifications.certifications')}
            value={requirementValue(item.certDefinitionConfigKeys, item.certRequirementMode)}
          />
          <FieldDisplay
            label={t('admin.tenantStandards.qualifications.drivingLicenses')}
            value={requirementValue(
              item.drivingLicenseDefinitionConfigKeys,
              item.drivingLicenseRequirementMode,
            )}
          />
          <FieldDisplay
            label={t('admin.tenantStandards.qualifications.requiresOrgDecision')}
            value={item.requiresOrgDecision ? t('common.yes') : t('common.no')}
          />
        </div>
      </Card>

      <ActionsSection>
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

      <AdminTenantQualificationEditModal
        key={item.configKey}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={() => {
          setEditOpen(false);
          reload();
        }}
        tenantId={tenantId}
        definition={item}
      />

      {deleteOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteOpen(false)}
          title={t('admin.tenantStandards.qualifications.deleteTitle')}
          message={t('admin.tenantStandards.qualifications.deleteMessage', { name: item.name })}
          confirmText={t('common.delete')}
          onConfirm={() => void handleDelete()}
          isLoading={busy}
        />
      ) : null}

      {archiveOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setArchiveOpen(false)}
          title={t('admin.tenantStandards.qualifications.archiveTitle')}
          message={t('admin.tenantStandards.qualifications.archiveMessage', { name: item.name })}
          confirmText={t('common.archive')}
          onConfirm={() => void handleArchive()}
          isLoading={busy}
        />
      ) : null}

      {unarchiveOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setUnarchiveOpen(false)}
          title={t('admin.tenantStandards.qualifications.unarchiveTitle')}
          message={t('admin.tenantStandards.qualifications.unarchiveMessage', { name: item.name })}
          confirmText={t('common.unarchive')}
          onConfirm={() => void handleUnarchive()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
