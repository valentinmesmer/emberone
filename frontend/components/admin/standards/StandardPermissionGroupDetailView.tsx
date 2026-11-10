/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Pencil, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import {
  StandardDetailAction,
  StandardDetailCardHeader,
} from '@/components/admin/standards/StandardDetailCardHeader';
import { StandardPermissionTypePicker } from '@/components/admin/standards/StandardPermissionTypePicker';
import {
  findStandardItem,
  isSystemStandardPermissionGroupItem,
  standardPermissionGroupItemType,
  standardPermissionGroupTypeLabel,
  standardPermissionLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
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
import { standardsApi } from '@/lib/api/standards';
import { getAdminStandardDetailPath } from '@/lib/navigation/adminWorkspace';
import { formBlockers } from '@/lib/utils';

interface StandardPermissionGroupDetailViewProps {
  state: StandardContentState;
  configKey: string;
}

export function StandardPermissionGroupDetailView({
  state,
  configKey,
}: StandardPermissionGroupDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { tenantId, standardId, items, editable, busy, runMutation, enabledModules } = state;

  const item = useMemo(
    () => findStandardItem(items, 'permission-groups', configKey),
    [items, configKey],
  );
  const a = item?.attributes ?? {};

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [permsDraft, setPermsDraft] = useState<Set<string> | null>(null);

  if (!item) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  const isSystemGroup = isSystemStandardPermissionGroupItem(item);
  const isAdminGroup = standardPermissionGroupItemType(item) === 'ADMIN';
  const displayedPermissions = item.permissionTypes;

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
        standardsApi.updatePermissionGroup(tenantId, standardId, configKey, {
          name: name.trim(),
          description: description.trim() || undefined,
          permissionTypes: item.permissionTypes,
        }),
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) setEditOpen(false);
  };

  const handleSavePermissions = async () => {
    if (!permsDraft) return;
    const ok = await runMutation(
      () =>
        standardsApi.updatePermissionGroup(tenantId, standardId, configKey, {
          name: item.title,
          description: a.description || undefined,
          permissionTypes: Array.from(permsDraft),
        }),
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) setPermsDraft(null);
  };

  const handleDelete = async () => {
    const ok = await runMutation(
      () => standardsApi.deletePermissionGroup(tenantId, standardId, configKey),
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
                <Badge tone="neutral">{standardPermissionGroupTypeLabel(t, item.subtitle)}</Badge>
              ) : null}
              {isSystemGroup ? (
                <Badge tone="accent">{t('admin.standards.systemPermissionGroupRequired')}</Badge>
              ) : null}
            </>
          }
          subtitle={
            isAdminGroup
              ? t('admin.standards.adminPermissionGroupHint')
              : isSystemGroup
                ? t('admin.standards.systemPermissionGroupRequiredHint')
                : undefined
          }
          actions={
            editable && !isAdminGroup ? (
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

      <Card className="p-3 sm:p-4">
        <OrganizationManageSection
          embedded
          title={t('admin.standards.permissionsLabel')}
          actionInline
          action={
            editable && !isAdminGroup ? (
              <StandardDetailAction
                label={t('common.edit')}
                icon={<Pencil className="h-4 w-4" aria-hidden />}
                onClick={() => setPermsDraft(new Set(item.permissionTypes))}
                disabled={busy}
              />
            ) : undefined
          }
        >
          {displayedPermissions.length === 0 ? (
            <p className="text-sm text-[var(--text-secondary)]">
              {t('admin.standards.noPermissions')}
            </p>
          ) : (
            <div className="flex flex-wrap gap-1.5">
              {displayedPermissions.map((perm) => (
                <Badge key={perm} tone="neutral">
                  {standardPermissionLabel(t, perm)}
                </Badge>
              ))}
            </div>
          )}
        </OrganizationManageSection>
      </Card>

      {editable ? (
        <ActionsSection>
          <ListRow
            leading={<ActionRowIcon icon={Trash2} />}
            title={t('admin.standards.deleteItem')}
            subtitle={
              isSystemGroup
                ? t('admin.standards.systemPermissionGroupRequiredHint')
                : t('common.deleteHint')
            }
            onClick={busy || isSystemGroup ? undefined : () => setDeleteOpen(true)}
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
          <div className="space-y-3">
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

      {permsDraft ? (
        <Modal
          isOpen
          onClose={() => setPermsDraft(null)}
          title={t('admin.standards.permissionsLabel')}
          size="lg"
          footer={
            <ModalFooter
              onCancel={() => setPermsDraft(null)}
              onSave={() => void handleSavePermissions()}
              isLoading={busy}
            />
          }
        >
          <StandardPermissionTypePicker
            selected={permsDraft}
            onChange={setPermsDraft}
            disabled={busy}
            enabledModules={enabledModules}
          />
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={deleteOpen}
        onClose={() => setDeleteOpen(false)}
        onConfirm={() => void handleDelete()}
        message={t('admin.standards.deleteItemConfirm')}
        confirmText={t('admin.standards.deleteItem')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
    </div>
  );
}
