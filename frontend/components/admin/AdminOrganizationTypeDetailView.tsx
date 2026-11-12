/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { AdminOrganizationTypeEditModal } from '@/components/admin/AdminOrganizationTypeEditModal';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { organizationTypesApi } from '@/lib/api/organizationTypes';
import {
  invalidateAdminTenantCatalog,
  markAdminTenantCatalogStale,
  useAdminOrganizationTree,
  useTenantOrganizationTypes,
} from '@/lib/store';
import { compareText } from '@/lib/i18n/locale';
import { flattenAdminOrgTree } from '@/lib/utils/adminOrgTree';
import {
  getAdminOrganizationDetailPath,
  getAdminTenantStandardsPath,
} from '@/lib/utils/adminPaths';

interface AdminOrganizationTypeDetailViewProps {
  tenantId: number;
  typeId: number;
}

export function AdminOrganizationTypeDetailView({
  tenantId,
  typeId,
}: AdminOrganizationTypeDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const queryClient = useQueryClient();
  const typesQuery = useTenantOrganizationTypes(tenantId);
  const treeQuery = useAdminOrganizationTree(tenantId, typesQuery.isSuccess);
  const organizationType = useMemo(
    () => typesQuery.data?.find((entry) => entry.id === typeId) ?? null,
    [typesQuery.data, typeId],
  );
  const loading = typesQuery.isPending;
  const loadFailed = typesQuery.isError;
  const organizationsLoadFailed = treeQuery.isError;

  const sortedOrganizations = useMemo(
    () =>
      flattenAdminOrgTree(treeQuery.data?.roots ?? [])
        .filter((org) => org.organizationTypeId === typeId)
        .sort((a, b) => compareText(a.name, b.name)),
    [treeQuery.data, typeId],
  );

  const navigateBack = () => {
    router.push(getAdminTenantStandardsPath(tenantId));
  };

  const handleDelete = async () => {
    if (!organizationType) return;
    setBusy(true);
    try {
      await organizationTypesApi.remove(tenantId, organizationType.id);
      showSuccess(t('admin.organizationTypes.deleted'));
      void markAdminTenantCatalogStale(queryClient, 'organizationTypes', tenantId);
      navigateBack();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.organizationTypes.errors.deleteFailed'));
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !organizationType) {
    return <NoticeCard>{t('admin.organizationTypes.errors.loadFailed')}</NoticeCard>;
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          className="mb-0"
          title={organizationType.name}
          badges={
            <Badge tone={organizationType.active ? 'success' : 'neutral'} pill>
              {organizationType.active
                ? t('admin.standards.active')
                : t('admin.organizationTypes.inactive')}
            </Badge>
          }
          actions={
            <StandardDetailAction
              label={t('common.edit')}
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={() => setEditOpen(true)}
              disabled={busy}
            />
          }
        />
      </Card>

      {organizationsLoadFailed ? (
        <NoticeCard>{t('admin.organizationTypes.errors.organizationsLoadFailed')}</NoticeCard>
      ) : sortedOrganizations.length === 0 ? (
        <EmptyStateCard>{t('admin.organizationTypes.organizationsEmpty')}</EmptyStateCard>
      ) : (
        <ListContainer label={t('admin.organizationTypes.organizationsTitle')} variant="card">
          {sortedOrganizations.map((org) => (
            <ListRow
              key={org.id}
              href={getAdminOrganizationDetailPath(tenantId, org.id)}
              title={org.name}
              subtitle={org.organizationCode}
            />
          ))}
        </ListContainer>
      )}

      <ActionsSection>
        <ListRow
          leading={<ActionRowIcon icon={Trash2} />}
          title={t('common.delete')}
          subtitle={t('common.deleteHint')}
          onClick={busy ? undefined : () => setDeleteOpen(true)}
        />
      </ActionsSection>

      <AdminOrganizationTypeEditModal
        key={organizationType.id}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={() => {
          setEditOpen(false);
          void invalidateAdminTenantCatalog(queryClient, 'organizationTypes', tenantId);
        }}
        tenantId={tenantId}
        organizationType={organizationType}
      />

      {deleteOpen ? (
        <ConfirmDialog
          isOpen
          onClose={() => setDeleteOpen(false)}
          title={t('common.delete')}
          message={t('admin.organizationTypes.deleteConfirm')}
          confirmText={t('common.delete')}
          cancelText={t('common.cancel')}
          onConfirm={() => void handleDelete()}
          isLoading={busy}
        />
      ) : null}
    </div>
  );
}
