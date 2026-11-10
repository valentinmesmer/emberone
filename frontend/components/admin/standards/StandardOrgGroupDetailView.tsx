/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { StandardOrgGroupFunctionGrantsCard } from '@/components/admin/standards/StandardOrgGroupFunctionGrantsCard';
import { StandardOrgGroupPermissionGroupsCard } from '@/components/admin/standards/StandardOrgGroupPermissionGroupsCard';
import {
  findStandardItem,
  isSystemStandardOrgGroupItem,
  standardGroupTypeLabel,
  standardOrgGroupSupportsFunctionGrants,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { Input } from '@/components/ui/input';
import { ListRow } from '@/components/ui/list';
import { NotFoundState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import {
  formatOrgGroupRemovalImpactMessage,
  removalImpactRequiresConfirmation,
} from '@/lib/admin/standardImpact';
import type { OrgGroupRemovalImpactDto } from '@/lib/api/standards';
import { standardsApi } from '@/lib/api/standards';
import { getAdminStandardDetailPath } from '@/lib/navigation/adminWorkspace';
import { formBlockers } from '@/lib/utils';

interface StandardOrgGroupDetailViewProps {
  state: StandardContentState;
  configKey: string;
}

export function StandardOrgGroupDetailView({ state, configKey }: StandardOrgGroupDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { tenantId, standardId, items, editable, busy, runMutation } = state;

  const item = useMemo(
    () => findStandardItem(items, 'organization-groups', configKey),
    [items, configKey],
  );
  const a = item?.attributes ?? {};

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteImpact, setDeleteImpact] = useState<OrgGroupRemovalImpactDto | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');

  const isSystemGroup = isSystemStandardOrgGroupItem(item);
  const supportsFunctionGrants = standardOrgGroupSupportsFunctionGrants(item);

  useEffect(() => {
    if (!deleteOpen) return;
    let cancelled = false;
    void standardsApi.getOrgGroupRemovalImpact(tenantId, standardId, configKey).then((impact) => {
      if (!cancelled) setDeleteImpact(impact);
    });
    return () => {
      cancelled = true;
    };
  }, [deleteOpen, tenantId, standardId, configKey]);

  if (!item) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  const openEdit = () => {
    setName(item.title);
    setDescription(a.description ?? '');
    setEditOpen(true);
  };

  const blockers = formBlockers([[t('admin.standards.name'), name.trim() !== '']]);

  const handleSave = async () => {
    if (blockers.length > 0) return;
    const ok = await runMutation(
      () =>
        standardsApi.updateOrgGroup(tenantId, standardId, configKey, {
          name: name.trim(),
          description: description.trim() || undefined,
        }),
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) setEditOpen(false);
  };

  const handleDelete = async () => {
    const ok = await runMutation(
      () => standardsApi.deleteOrgGroup(tenantId, standardId, configKey),
      'admin.standards.itemDeleted',
      'admin.standards.errors.itemDeleteFailed',
    );
    if (ok) {
      setDeleteOpen(false);
      router.push(getAdminStandardDetailPath(tenantId, standardId));
    }
  };

  return (
    <div className="w-full space-y-3">
      <Card className="p-3 sm:p-4">
        <StandardDetailCardHeader
          title={item.title}
          badges={
            <>
              {item.subtitle ? (
                <Badge tone="neutral">{standardGroupTypeLabel(t, item.subtitle)}</Badge>
              ) : null}
              {isSystemGroup ? (
                <Badge tone="accent">{t('admin.standards.systemOrgGroupRequired')}</Badge>
              ) : null}
            </>
          }
          subtitle={isSystemGroup ? t('admin.standards.systemOrgGroupRequiredHint') : undefined}
          actions={
            editable ? (
              <StandardDetailAction
                label={t('common.edit')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={openEdit}
                disabled={busy}
              />
            ) : undefined
          }
        />
        <FieldDisplay
          label={t('admin.standards.description')}
          value={a.description}
          fallback={t('organizationManagement.permissionGroups.noDescription')}
        />
      </Card>

      {supportsFunctionGrants ? (
        <StandardOrgGroupFunctionGrantsCard state={state} configKey={configKey} />
      ) : null}

      <StandardOrgGroupPermissionGroupsCard state={state} configKey={configKey} />

      {editable ? (
        <ActionsSection>
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('admin.standards.deleteItem')}
            subtitle={
              isSystemGroup
                ? t('admin.standards.systemOrgGroupRequiredHint')
                : t('common.deleteHint')
            }
            onClick={
              busy || isSystemGroup
                ? undefined
                : () => {
                    setDeleteImpact(null);
                    setDeleteOpen(true);
                  }
            }
          />
        </ActionsSection>
      ) : null}

      {editOpen ? (
        <Modal
          isOpen
          onClose={() => setEditOpen(false)}
          title={t('common.edit')}
          size="lg"
          footer={
            <ModalFooter
              onCancel={() => setEditOpen(false)}
              onSave={() => void handleSave()}
              blockers={blockers}
              isLoading={busy}
            />
          }
        >
          <div className="space-y-4">
            <Input
              label={t('admin.standards.name')}
              value={name}
              onChange={(e) => setName(e.target.value)}
              disabled={busy}
              required
            />
            <Textarea
              label={t('admin.standards.description')}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={busy}
            />
          </div>
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => void handleDelete()}
        message={
          deleteImpact
            ? formatOrgGroupRemovalImpactMessage(deleteImpact, t)
            : t('admin.standards.deleteItemConfirm')
        }
        confirmText={t('admin.standards.deleteItem')}
        cancelText={t('common.cancel')}
        variant={
          deleteImpact && removalImpactRequiresConfirmation(deleteImpact) ? 'danger' : 'info'
        }
        isLoading={busy}
      />
    </div>
  );
}
