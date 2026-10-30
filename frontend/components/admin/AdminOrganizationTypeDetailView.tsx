/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { AdminOrganizationTypeEditModal } from '@/components/admin/AdminOrganizationTypeEditModal';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard, NoticeCard } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { adminApi } from '@/lib/api/admin';
import { type OrganizationTypeDto, organizationTypesApi } from '@/lib/api/organizationTypes';
import { compareText } from '@/lib/i18n/locale';
import { type FlatAdminOrganization, flattenAdminOrgTree } from '@/lib/utils/adminOrgTree';
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

  const [loading, setLoading] = useState(true);
  const [loadFailed, setLoadFailed] = useState(false);
  const [organizationType, setOrganizationType] = useState<OrganizationTypeDto | null>(null);
  const [organizations, setOrganizations] = useState<FlatAdminOrganization[]>([]);
  const [organizationsLoadFailed, setOrganizationsLoadFailed] = useState(false);
  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  const loadData = useCallback(async () => {
    setLoading(true);
    setLoadFailed(false);
    setOrganizationsLoadFailed(false);
    try {
      const types = await organizationTypesApi.list(tenantId);
      const match = types.find((entry) => entry.id === typeId) ?? null;
      if (!match) {
        setLoadFailed(true);
        setOrganizationType(null);
        setOrganizations([]);
        return;
      }

      setOrganizationType(match);
      try {
        const orgTree = await adminApi.getAdminOrganizationTree(tenantId);
        setOrganizations(
          flattenAdminOrgTree(orgTree.roots).filter((org) => org.organizationTypeId === typeId),
        );
      } catch {
        setOrganizations([]);
        setOrganizationsLoadFailed(true);
      }
    } catch {
      setLoadFailed(true);
      setOrganizationType(null);
      setOrganizations([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId, typeId]);

  const sortedOrganizations = useMemo(
    () => [...organizations].sort((a, b) => compareText(a.name, b.name)),
    [organizations],
  );

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const navigateBack = () => {
    router.push(getAdminTenantStandardsPath(tenantId));
  };

  const handleDelete = async () => {
    if (!organizationType) return;
    setBusy(true);
    try {
      await organizationTypesApi.remove(tenantId, organizationType.id);
      showSuccess(t('admin.organizationTypes.deleted'));
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
            <>
              <StandardDetailAction
                label={t('common.delete')}
                icon={<Trash2 className="h-4 w-4" aria-hidden />}
                onClick={() => setDeleteOpen(true)}
                disabled={busy}
              />
              <StandardDetailAction
                label={t('common.edit')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={() => setEditOpen(true)}
                disabled={busy}
              />
            </>
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

      <AdminOrganizationTypeEditModal
        key={organizationType.id}
        isOpen={editOpen}
        onClose={() => setEditOpen(false)}
        onSaved={() => {
          setEditOpen(false);
          void loadData();
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
