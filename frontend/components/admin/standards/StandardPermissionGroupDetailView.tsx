/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
  ALL_PERMISSION_TYPES,
  findStandardItem,
  isSystemStandardPermissionGroupItem,
  standardPermissionGroupItemType,
  standardPermissionGroupTypeLabel,
  standardPermissionLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { Input } from '@/components/ui/input';
import { NotFoundState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import { standardsApi } from '@/lib/api/standards';
import { getAdminStandardDetailPath } from '@/lib/navigation/adminWorkspace';

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
  const [perms, setPerms] = useState<Set<string>>(new Set());

  if (!item) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  const isSystemGroup = isSystemStandardPermissionGroupItem(item);
  const isAdminGroup = standardPermissionGroupItemType(item) === 'ADMIN';
  const displayedPermissions = isAdminGroup ? ALL_PERMISSION_TYPES : item.permissionTypes;

  const openEdit = () => {
    setName(item.title);
    setDescription(a.description ?? '');
    setPerms(new Set(isAdminGroup ? ALL_PERMISSION_TYPES : item.permissionTypes));
    setEditOpen(true);
  };

  const handleSave = async () => {
    if (!name.trim()) return;
    const ok = await runMutation(
      () =>
        standardsApi.updatePermissionGroup(tenantId, standardId, configKey, {
          name: name.trim(),
          description: description.trim() || undefined,
          permissionTypes: isAdminGroup ? ALL_PERMISSION_TYPES : Array.from(perms),
        }),
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) setEditOpen(false);
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
              <>
                <StandardDetailAction
                  label={t('common.edit')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={openEdit}
                  disabled={busy}
                />
                {!isSystemGroup ? (
                  <StandardDetailAction
                    label={t('admin.standards.deleteItem')}
                    icon={<Trash2 className="h-4 w-4" aria-hidden />}
                    onClick={() => setDeleteOpen(true)}
                    disabled={busy}
                  />
                ) : null}
              </>
            ) : undefined
          }
        />
        <div className="space-y-3">
          <FieldDisplay
            label={t('admin.standards.description')}
            value={a.description}
            fallback={t('organizationManagement.permissionGroups.noDescription')}
          />
          <div>
            <p className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
              {t('admin.standards.permissionsLabel')}
            </p>
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
          </div>
        </div>
      </Card>

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
              isValid={name.trim().length > 0}
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
            />
            <Textarea
              label={t('admin.standards.description')}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={busy}
            />
            <div>
              <div className="mb-1.5 text-sm font-medium text-[var(--text-primary)]">
                {t('admin.standards.permissionsLabel')}
              </div>
              {isAdminGroup ? (
                <p className="text-sm text-[var(--text-secondary)]">
                  {t('admin.standards.adminPermissionGroupHint')}
                </p>
              ) : (
                <StandardPermissionTypePicker
                  selected={perms}
                  onChange={setPerms}
                  disabled={busy}
                  enabledModules={enabledModules}
                />
              )}
            </div>
          </div>
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
