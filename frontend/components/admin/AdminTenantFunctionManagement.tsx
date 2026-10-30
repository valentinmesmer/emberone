/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Archive, Pencil, Plus, RotateCcw } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import { AdminTenantFunctionEditModal } from '@/components/admin/AdminTenantFunctionEditModal';
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
import { type TenantFunctionDto, tenantFunctionsApi } from '@/lib/api/tenantFunctions';

interface AdminTenantFunctionManagementProps {
  tenantId: number;
}

export function AdminTenantFunctionManagement({ tenantId }: AdminTenantFunctionManagementProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [loading, setLoading] = useState(true);
  const [definitions, setDefinitions] = useState<TenantFunctionDto[]>([]);
  const [busy, setBusy] = useState(false);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<TenantFunctionDto | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<TenantFunctionDto | null>(null);
  const [unarchiveTarget, setUnarchiveTarget] = useState<TenantFunctionDto | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      setDefinitions(await tenantFunctionsApi.list(tenantId));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.tenantStandards.functions.loadFailed'));
      setDefinitions([]);
    } finally {
      setLoading(false);
    }
  }, [tenantId, showError, t]);

  useEffect(() => {
    queueMicrotask(() => {
      void loadData();
    });
  }, [loadData]);

  const listContent = loading ? (
    <LoadingState variant="embedded" size="sm" />
  ) : definitions.length === 0 ? (
    <EmptyStateCard>{t('admin.tenantStandards.functions.empty')}</EmptyStateCard>
  ) : (
    <ListContainer label={t('admin.tenantStandards.functions.title')}>
      {definitions.map((definition) => (
        <ListRow
          key={definition.configKey}
          compact
          title={definition.name}
          subtitle={definition.abbreviation}
          badges={
            <>
              {!definition.active ? <Badge tone="neutral">{t('common.inactive')}</Badge> : null}
              {definition.archived ? <Badge tone="warning">{t('common.archived')}</Badge> : null}
            </>
          }
          actions={
            <div className="flex items-center gap-1">
              <IconButton
                icon={<Pencil className="h-4 w-4" />}
                aria-label={t('common.edit')}
                onClick={() => {
                  setEditing(definition);
                  setFormOpen(true);
                }}
                disabled={busy}
              />
              {definition.archived ? (
                <IconButton
                  icon={<RotateCcw className="h-4 w-4" />}
                  aria-label={t('common.unarchive')}
                  onClick={() => setUnarchiveTarget(definition)}
                  disabled={busy}
                />
              ) : (
                <IconButton
                  icon={<Archive className="h-4 w-4" />}
                  aria-label={t('common.archive')}
                  onClick={() => setArchiveTarget(definition)}
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
      <SectionHeader
        className="mb-3"
        title={t('admin.tenantStandards.functions.title')}
        subtitle={t('admin.tenantStandards.functions.subtitle')}
        action={
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
              {t('admin.tenantStandards.functions.add')}
            </Button>
          </SectionActions>
        }
      />
      {listContent}

      {formOpen ? (
        <AdminTenantFunctionEditModal
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
        onConfirm={async () => {
          if (!archiveTarget) return;
          setBusy(true);
          try {
            await tenantFunctionsApi.archive(tenantId, archiveTarget.configKey);
            showSuccess(t('admin.tenantStandards.functions.archived'));
            setArchiveTarget(null);
            await loadData();
          } catch (e) {
            showError(
              e instanceof Error ? e.message : t('admin.tenantStandards.functions.archiveFailed'),
            );
          } finally {
            setBusy(false);
          }
        }}
        message={t('admin.tenantStandards.functions.archiveConfirm')}
        confirmText={t('common.archive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
      <ConfirmDialog
        isOpen={unarchiveTarget != null}
        onClose={() => setUnarchiveTarget(null)}
        onConfirm={async () => {
          if (!unarchiveTarget) return;
          setBusy(true);
          try {
            await tenantFunctionsApi.unarchive(tenantId, unarchiveTarget.configKey);
            showSuccess(t('admin.tenantStandards.functions.unarchived'));
            setUnarchiveTarget(null);
            await loadData();
          } catch (e) {
            showError(
              e instanceof Error ? e.message : t('admin.tenantStandards.functions.unarchiveFailed'),
            );
          } finally {
            setBusy(false);
          }
        }}
        message={t('admin.tenantStandards.functions.unarchiveConfirm')}
        confirmText={t('common.unarchive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
    </>
  );
}
