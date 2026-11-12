/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Archive, Pencil, RotateCcw } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { AdminTenantRankEditModal } from '@/components/admin/AdminTenantRankEditModal';
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
import { tenantRanksApi } from '@/lib/api/tenantRanks';
import {
  invalidateAdminTenantCatalog,
  markAdminTenantCatalogStale,
  useTenantRank,
} from '@/lib/store';
import { getAdminTenantStandardsPath } from '@/lib/utils/adminPaths';

interface AdminTenantRankDetailViewProps {
  tenantId: number;
  configKey: string;
}

export function AdminTenantRankDetailView({ tenantId, configKey }: AdminTenantRankDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();

  const [busy, setBusy] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [archiveOpen, setArchiveOpen] = useState(false);
  const [unarchiveOpen, setUnarchiveOpen] = useState(false);

  const queryClient = useQueryClient();
  const {
    data: item = null,
    isPending: loading,
    isError: loadFailed,
  } = useTenantRank(tenantId, configKey);
  const reload = () => void invalidateAdminTenantCatalog(queryClient, 'ranks', tenantId);
  const staleOnLeave = () => void markAdminTenantCatalogStale(queryClient, 'ranks', tenantId);

  const handleArchive = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantRanksApi.archive(tenantId, item.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.ranks.archived'));
      router.push(getAdminTenantStandardsPath(tenantId));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.ranks.archiveFailed'));
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantRanksApi.unarchive(tenantId, item.configKey);
      reload();
      showSuccess(t('admin.tenantStandards.ranks.unarchived'));
      setUnarchiveOpen(false);
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.ranks.unarchiveFailed'));
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !item) {
    return <NoticeCard>{t('admin.tenantStandards.ranks.loadFailed')}</NoticeCard>;
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
            label={t('admin.tenantStandards.ranks.abbreviation')}
            value={item.abbreviation}
          />
          <FieldDisplay
            label={t('admin.tenantStandards.ranks.sortOrder')}
            value={String(item.sortOrder)}
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
        ) : (
          <ListRow
            leading={<ActionRowIcon icon={Archive} />}
            title={t('common.archive')}
            subtitle={t('common.archiveHint')}
            onClick={busy ? undefined : () => setArchiveOpen(true)}
          />
        )}
      </ActionsSection>

      {editOpen ? (
        <AdminTenantRankEditModal
          tenantId={tenantId}
          editing={item}
          isOpen
          onClose={() => setEditOpen(false)}
          onSaved={reload}
        />
      ) : null}

      <ConfirmDialog
        isOpen={archiveOpen}
        onClose={() => setArchiveOpen(false)}
        onConfirm={() => void handleArchive()}
        message={t('admin.tenantStandards.ranks.archiveConfirm')}
        confirmText={t('common.archive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
      <ConfirmDialog
        isOpen={unarchiveOpen}
        onClose={() => setUnarchiveOpen(false)}
        onConfirm={() => void handleUnarchive()}
        message={t('admin.tenantStandards.ranks.unarchiveConfirm')}
        confirmText={t('common.unarchive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
    </div>
  );
}
