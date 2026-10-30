/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import {
  AdminManagementAudienceEntryModal,
  managementAudienceKindLabelKey,
} from '@/components/admin/AdminManagementAudienceEntryModal';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { ManagementAudienceEntryRequest } from '@/lib/api/admin';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { useAdminManagementAudiences, useReplaceAdminManagementAudiences } from '@/lib/store';
import {
  getAdminManagementAudienceDetailPath,
  getAdminOrganizationDetailPath,
} from '@/lib/utils/adminPaths';
import { managementPermissionLabel } from '@/lib/utils/managementActivation';
import {
  managementAudienceTargetName,
  toManagementAudienceRequest,
} from '@/lib/utils/managementAudience';

interface AdminManagementAudienceDetailViewProps {
  tenantId: number;
  organizationId: number;
  entryId: number;
}

/** One audience entry of a managing organization: scope, allowed permissions, edit and remove. */
export function AdminManagementAudienceDetailView({
  tenantId,
  organizationId,
  entryId,
}: AdminManagementAudienceDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const setBackHref = useSetBackHref();
  const { showError, showSuccess } = useToast();
  const { data, isPending } = useAdminManagementAudiences(tenantId, organizationId);
  const replaceAudiences = useReplaceAdminManagementAudiences(tenantId, organizationId);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const organizationPath = getAdminOrganizationDetailPath(tenantId, organizationId);

  useEffect(() => {
    setBackHref(organizationPath);
    return () => setBackHref(null);
  }, [setBackHref, organizationPath]);

  if (isPending) {
    return <LoadingState variant="embedded" />;
  }

  const entry = data?.entries.find((candidate) => candidate.id === entryId);
  if (!data || !entry) {
    return <NotFoundState message={t('admin.managementAudience.notFound')} />;
  }

  const targetName = managementAudienceTargetName(t, entry, data);

  const save = async (request: ManagementAudienceEntryRequest) => {
    try {
      const settings = await replaceAudiences.mutateAsync({
        entries: data.entries.map((other) =>
          other.id === entry.id ? request : toManagementAudienceRequest(other),
        ),
      });
      setEditOpen(false);
      showSuccess(t('admin.managementAudience.saved'));
      // Changing scope or target yields a new entry id.
      const saved = settings.entries.find(
        (candidate) => candidate.kind === request.kind && candidate.refId === request.refId,
      );
      if (saved && saved.id !== entry.id) {
        router.replace(getAdminManagementAudienceDetailPath(tenantId, organizationId, saved.id));
      }
    } catch (err) {
      showError(err instanceof Error ? err.message : t('admin.managementAudience.saveFailed'));
    }
  };

  const remove = async () => {
    try {
      await replaceAudiences.mutateAsync({
        entries: data.entries
          .filter((other) => other.id !== entry.id)
          .map(toManagementAudienceRequest),
      });
      showSuccess(t('admin.managementAudience.deleted'));
      router.push(organizationPath);
    } catch (err) {
      showError(err instanceof Error ? err.message : t('admin.managementAudience.saveFailed'));
    }
  };

  return (
    <div className="space-y-3">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          title={t(managementAudienceKindLabelKey(entry.kind))}
          subtitle={targetName ?? undefined}
          actions={
            <DetailCardAction
              label={t('common.edit')}
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={() => setEditOpen(true)}
            />
          }
        />
        <dl className="grid grid-cols-1 gap-2 text-sm">
          <div>
            <dt className="text-[var(--text-secondary)]">
              {t('admin.managementAudience.durationLabel')}
            </dt>
            <dd>
              {t('admin.managementAudience.durationValue', { minutes: entry.maxDurationMinutes })}
            </dd>
          </div>
          <div>
            <dt className="text-[var(--text-secondary)]">
              {t('admin.managementAudience.allowedPermissions')}
            </dt>
            <dd className="mt-1 flex flex-wrap gap-1.5">
              {entry.permissionTypes.map((permission) => (
                <Badge key={permission} tone="neutral">
                  {managementPermissionLabel(t, permission)}
                </Badge>
              ))}
            </dd>
          </div>
        </dl>
      </Card>

      <ActionsSection title={t('admin.managementAudience.actionsTitle')}>
        <ListRow
          leading={<ActionRowIcon icon={Trash2} />}
          title={t('admin.managementAudience.removeEntry')}
          subtitle={t('admin.managementAudience.removeHint')}
          onClick={() => setDeleteOpen(true)}
        />
      </ActionsSection>

      {editOpen ? (
        <AdminManagementAudienceEntryModal
          entry={entry}
          organizations={data.organizations}
          organizationTypes={data.organizationTypes}
          taken={data.entries.filter((other) => other.id !== entry.id)}
          isSaving={replaceAudiences.isPending}
          onClose={() => setEditOpen(false)}
          onSave={(request) => void save(request)}
        />
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => void remove()}
        title={t('admin.managementAudience.deleteTitle')}
        message={t('admin.managementAudience.deleteMessage')}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={replaceAudiences.isPending}
      />
    </div>
  );
}
