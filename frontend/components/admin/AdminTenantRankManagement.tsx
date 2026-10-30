/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Archive, Pencil, Plus, RotateCcw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantRankEditModal } from '@/components/admin/AdminTenantRankEditModal';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { SectionActions, SectionHeader } from '@/components/ui/section-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { type TenantRankDto, tenantRanksApi } from '@/lib/api/tenantRanks';

interface AdminTenantRankManagementProps {
  tenantId: number;
  embedded?: boolean;
}

export function AdminTenantRankManagement({
  tenantId,
  embedded = false,
}: AdminTenantRankManagementProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [loading, setLoading] = useState(true);
  const [ranks, setRanks] = useState<TenantRankDto[]>([]);
  const [busy, setBusy] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TenantRankDto | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<TenantRankDto | null>(null);
  const [unarchiveTarget, setUnarchiveTarget] = useState<TenantRankDto | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setRanks(await tenantRanksApi.list(tenantId));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.ranks.loadFailed'));
      setRanks([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId, showError, t]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const handleArchive = async () => {
    if (!archiveTarget) return;
    setBusy(true);
    try {
      await tenantRanksApi.archive(tenantId, archiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.ranks.archived'));
      setArchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.ranks.archiveFailed'));
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!unarchiveTarget) return;
    setBusy(true);
    try {
      await tenantRanksApi.unarchive(tenantId, unarchiveTarget.configKey);
      showSuccess(t('admin.tenantStandards.ranks.unarchived'));
      setUnarchiveTarget(null);
      await loadData();
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.ranks.unarchiveFailed'));
    } finally {
      setBusy(false);
    }
  };

  const addButton = (
    <SectionActions layout="inline">
      <Button
        size="xs"
        onClick={() => {
          setEditing(null);
          setFormOpen(true);
        }}
        disabled={busy || loading}
        fullWidth="responsive"
      >
        <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
        {t('admin.tenantStandards.ranks.add')}
      </Button>
    </SectionActions>
  );

  const header = embedded ? null : (
    <SectionHeader
      className="mb-3"
      title={t('admin.tenantStandards.ranks.title')}
      subtitle={t('admin.tenantStandards.ranks.subtitle')}
      action={addButton}
    />
  );

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : ranks.length === 0 ? (
    <EmptyStateCard>{t('admin.tenantStandards.ranks.empty')}</EmptyStateCard>
  ) : (
    <ListContainer label={t('admin.tenantStandards.ranks.title')}>
      {ranks.map((rank) => (
        <ListRow
          key={rank.configKey}
          compact
          title={rank.name}
          subtitle={rank.abbreviation}
          badges={
            <>
              {!rank.active ? <Badge tone="neutral">{t('common.inactive')}</Badge> : null}
              {rank.archived ? <Badge tone="warning">{t('common.archived')}</Badge> : null}
            </>
          }
          actions={
            <div className="flex items-center gap-1">
              <IconButton
                icon={<Pencil className="h-4 w-4" />}
                aria-label={t('common.edit')}
                onClick={() => {
                  setEditing(rank);
                  setFormOpen(true);
                }}
                disabled={busy}
              />
              {rank.archived ? (
                <IconButton
                  icon={<RotateCcw className="h-4 w-4" />}
                  aria-label={t('common.unarchive')}
                  onClick={() => setUnarchiveTarget(rank)}
                  disabled={busy}
                />
              ) : (
                <IconButton
                  icon={<Archive className="h-4 w-4" />}
                  aria-label={t('common.archive')}
                  onClick={() => setArchiveTarget(rank)}
                  disabled={busy}
                />
              )}
            </div>
          }
        />
      ))}
    </ListContainer>
  );

  return (
    <>
      {header}
      {embedded ? (
        <>
          <SectionHeader
            className="mb-3"
            title={t('admin.tenantStandards.ranks.title')}
            subtitle={t('admin.tenantStandards.ranks.subtitle')}
            action={addButton}
          />
          {listContent}
        </>
      ) : (
        listContent
      )}

      {formOpen ? (
        <AdminTenantRankEditModal
          tenantId={tenantId}
          editing={editing}
          isOpen
          onClose={() => setFormOpen(false)}
          onSaved={() => void loadData()}
        />
      ) : null}

      <ConfirmDialog
        isOpen={archiveTarget != null}
        onClose={() => setArchiveTarget(null)}
        onConfirm={() => void handleArchive()}
        message={t('admin.tenantStandards.ranks.archiveConfirm')}
        confirmText={t('common.archive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
      <ConfirmDialog
        isOpen={unarchiveTarget != null}
        onClose={() => setUnarchiveTarget(null)}
        onConfirm={() => void handleUnarchive()}
        message={t('admin.tenantStandards.ranks.unarchiveConfirm')}
        confirmText={t('common.unarchive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
    </>
  );
}
