/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus, Trash2 } from 'lucide-react';
import { useEffect, useState } from 'react';
import {
  AdminManagementAudienceEntryModal,
  managementAudienceKindLabelKey,
} from '@/components/admin/AdminManagementAudienceEntryModal';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { SectionActions, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { ManagementAudienceEntryDto, ManagementAudienceEntryRequest } from '@/lib/api/admin';
import { useAdminManagementAudiences, useReplaceAdminManagementAudiences } from '@/lib/store';
import { getAdminManagementAudienceDetailPath } from '@/lib/utils/adminPaths';
import {
  managementAudienceTargetName,
  toManagementAudienceRequest,
} from '@/lib/utils/managementAudience';

interface AdminOrganizationManagementSectionProps {
  tenantId: number;
  organizationId: number;
}

export function AdminOrganizationManagementSection({
  tenantId,
  organizationId,
}: AdminOrganizationManagementSectionProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const { data, isLoading, isError, error } = useAdminManagementAudiences(tenantId, organizationId);
  const replaceAudiences = useReplaceAdminManagementAudiences(tenantId, organizationId);

  const [adding, setAdding] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<ManagementAudienceEntryDto | null>(null);

  useEffect(() => {
    if (!isError) return;
    showError(error instanceof Error ? error.message : t('admin.managementAudience.loadError'));
  }, [isError, error, showError, t]);

  if (isLoading) {
    return (
      <Card className="p-3 sm:p-4">
        <LoadingState />
      </Card>
    );
  }
  if (!data) return null;

  const entries = data.entries;

  const handleModalSave = async (request: ManagementAudienceEntryRequest) => {
    try {
      await replaceAudiences.mutateAsync({
        entries: [...entries.map(toManagementAudienceRequest), request],
      });
      setAdding(false);
      showSuccess(t('admin.managementAudience.saved'));
    } catch (err) {
      showError(err instanceof Error ? err.message : t('admin.managementAudience.saveFailed'));
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await replaceAudiences.mutateAsync({
        entries: entries
          .filter((entry) => entry.id !== deleteTarget.id)
          .map(toManagementAudienceRequest),
      });
      setDeleteTarget(null);
      showSuccess(t('admin.managementAudience.deleted'));
    } catch (err) {
      showError(err instanceof Error ? err.message : t('admin.managementAudience.saveFailed'));
    }
  };

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader
        title={t('admin.managementAudience.title')}
        subtitle={t('admin.managementAudience.intro')}
        action={
          <SectionActions layout="inline">
            <Button
              type="button"
              variant="secondary"
              size="xs"
              fullWidth="responsive"
              onClick={() => setAdding(true)}
            >
              <Plus className="h-4 w-4" aria-hidden />
              {t('admin.managementAudience.addEntry')}
            </Button>
          </SectionActions>
        }
      />

      {entries.length > 0 ? (
        <ListContainer className="mt-3" variant="inset" label={t('admin.managementAudience.title')}>
          {entries.map((entry) => (
            <ListRow
              key={entry.id}
              accent={false}
              title={t(managementAudienceKindLabelKey(entry.kind))}
              subtitle={managementAudienceTargetName(t, entry, data) ?? undefined}
              href={getAdminManagementAudienceDetailPath(tenantId, organizationId, entry.id)}
              keepActionsOnMobile
              actions={
                <IconButton
                  variant="action"
                  aria-label={t('admin.managementAudience.removeEntry')}
                  icon={<Trash2 className="h-4 w-4" aria-hidden />}
                  onClick={() => setDeleteTarget(entry)}
                />
              }
            />
          ))}
        </ListContainer>
      ) : (
        <EmptyStateCard className="mt-3">{t('admin.managementAudience.empty')}</EmptyStateCard>
      )}

      <ConfirmDialog
        isOpen={deleteTarget != null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => void handleDelete()}
        title={t('admin.managementAudience.deleteTitle')}
        message={t('admin.managementAudience.deleteMessage')}
        confirmText={t('common.delete')}
        variant="danger"
        isLoading={replaceAudiences.isPending}
      />

      {adding ? (
        <AdminManagementAudienceEntryModal
          entry={null}
          organizations={data.organizations}
          organizationTypes={data.organizationTypes}
          taken={entries}
          isSaving={replaceAudiences.isPending}
          onClose={() => setAdding(false)}
          onSave={(request) => void handleModalSave(request)}
        />
      ) : null}
    </Card>
  );
}
