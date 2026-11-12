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
import { AdminTenantFunctionEditModal } from '@/components/admin/AdminTenantFunctionEditModal';
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
import { tenantFunctionsApi } from '@/lib/api/tenantFunctions';
import {
  invalidateAdminTenantCatalog,
  markAdminTenantCatalogStale,
  useTenantFunction,
} from '@/lib/store';
import { getAdminTenantStandardsPath } from '@/lib/utils/adminPaths';

interface AdminTenantFunctionDetailViewProps {
  tenantId: number;
  configKey: string;
}

export function AdminTenantFunctionDetailView({
  tenantId,
  configKey,
}: AdminTenantFunctionDetailViewProps) {
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
  } = useTenantFunction(tenantId, configKey);
  const reload = () => void invalidateAdminTenantCatalog(queryClient, 'functions', tenantId);
  const staleOnLeave = () => void markAdminTenantCatalogStale(queryClient, 'functions', tenantId);

  const handleArchive = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantFunctionsApi.archive(tenantId, item.configKey);
      staleOnLeave();
      showSuccess(t('admin.tenantStandards.functions.archived'));
      router.push(getAdminTenantStandardsPath(tenantId));
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.functions.archiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  const handleUnarchive = async () => {
    if (!item) return;
    setBusy(true);
    try {
      await tenantFunctionsApi.unarchive(tenantId, item.configKey);
      reload();
      showSuccess(t('admin.tenantStandards.functions.unarchived'));
      setUnarchiveOpen(false);
    } catch (e) {
      showError(
        e instanceof Error ? e.message : t('admin.tenantStandards.functions.unarchiveFailed'),
      );
    } finally {
      setBusy(false);
    }
  };

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (loadFailed || !item) {
    return <NoticeCard>{t('admin.tenantStandards.functions.loadFailed')}</NoticeCard>;
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
            label={t('admin.tenantStandards.functions.abbreviation')}
            value={item.abbreviation}
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
        <AdminTenantFunctionEditModal
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
        message={t('admin.tenantStandards.functions.archiveConfirm')}
        confirmText={t('common.archive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
      <ConfirmDialog
        isOpen={unarchiveOpen}
        onClose={() => setUnarchiveOpen(false)}
        onConfirm={() => void handleUnarchive()}
        message={t('admin.tenantStandards.functions.unarchiveConfirm')}
        confirmText={t('common.unarchive')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
    </div>
  );
}
