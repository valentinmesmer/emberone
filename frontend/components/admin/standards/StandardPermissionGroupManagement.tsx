/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useMemo, useState } from 'react';
import { StandardPermissionTypePicker } from '@/components/admin/standards/StandardPermissionTypePicker';
import {
  isAdminStandardPermissionGroupItem,
  isSystemStandardPermissionGroupItem,
  standardPermissionGroupTypeLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { IconButton } from '@/components/ui/icon-button';
import { Input } from '@/components/ui/input';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import { type StandardContentItemDto, standardsApi } from '@/lib/api/standards';
import { getAdminStandardPermissionGroupPath } from '@/lib/navigation/adminWorkspace';

interface StandardPermissionGroupManagementProps {
  state: StandardContentState;
  embedded?: boolean;
}

export function StandardPermissionGroupManagement({
  state,
  embedded = false,
}: StandardPermissionGroupManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { tenantId, standardId, items, editable, busy, runMutation, enabledModules } = state;

  const permGroupItems = useMemo(
    () => items.filter((i) => i.domainKey === 'permission-groups'),
    [items],
  );

  const [formOpen, setFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<StandardContentItemDto | null>(null);
  const [deleteKey, setDeleteKey] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [perms, setPerms] = useState<Set<string>>(new Set());

  const openCreate = () => {
    setEditingItem(null);
    setName('');
    setDescription('');
    setPerms(new Set());
    setFormOpen(true);
  };

  const openEdit = (item: StandardContentItemDto) => {
    const a = item.attributes ?? {};
    setEditingItem(item);
    setName(item.title);
    setDescription(a.description ?? '');
    setPerms(new Set(item.permissionTypes));
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditingItem(null);
  };

  const handleSave = async () => {
    if (!name.trim()) return;
    const ok = await runMutation(
      () =>
        editingItem
          ? standardsApi.updatePermissionGroup(tenantId, standardId, editingItem.configKey, {
              name: name.trim(),
              description: description.trim() || undefined,
              permissionTypes: Array.from(perms),
            })
          : standardsApi.addPermissionGroup(tenantId, standardId, {
              name: name.trim(),
              description: description.trim() || undefined,
              type: 'CUSTOM',
              permissionTypes: Array.from(perms),
            }),
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) closeForm();
  };

  const handleDelete = async () => {
    if (!deleteKey) return;
    const ok = await runMutation(
      () => standardsApi.deletePermissionGroup(tenantId, standardId, deleteKey),
      'admin.standards.itemDeleted',
      'admin.standards.errors.itemDeleteFailed',
    );
    if (ok) setDeleteKey(null);
  };

  return (
    <>
      <OrganizationManageSection
        embedded={embedded}
        title={t('admin.standards.sections.permissionGroups')}
        action={
          editable ? (
            <Button
              variant="primary"
              size="xs"
              onClick={openCreate}
              disabled={busy}
              fullWidth="responsive"
              icon={<Plus className="h-3.5 w-3.5" aria-hidden />}
            >
              {t('common.add')}
            </Button>
          ) : undefined
        }
      >
        {permGroupItems.length === 0 ? (
          <EmptyStateCard>{t('admin.standards.detail.empty')}</EmptyStateCard>
        ) : (
          <ListContainer label={t('admin.standards.sections.permissionGroups')}>
            {permGroupItems.map((item) => {
              const isSystemGroup = isSystemStandardPermissionGroupItem(item);
              const isAdminGroup = isAdminStandardPermissionGroupItem(item);
              return (
                <ListRow
                  key={item.configKey}
                  compact
                  onClick={() =>
                    router.push(
                      getAdminStandardPermissionGroupPath(tenantId, standardId, item.configKey),
                    )
                  }
                  title={item.title}
                  subtitle={
                    item.permissionTypes.length > 0 ? (
                      <span className="line-clamp-1">
                        {item.permissionTypes.length} {t('admin.standards.permissionsLabel')}
                      </span>
                    ) : undefined
                  }
                  badges={
                    item.subtitle || isSystemGroup ? (
                      <>
                        {item.subtitle ? (
                          <Badge tone="neutral">
                            {standardPermissionGroupTypeLabel(t, item.subtitle)}
                          </Badge>
                        ) : null}
                        {isSystemGroup ? (
                          <Badge tone="accent">
                            {t('admin.standards.systemPermissionGroupRequired')}
                          </Badge>
                        ) : null}
                      </>
                    ) : undefined
                  }
                  actions={
                    editable ? (
                      <>
                        {!isAdminGroup ? (
                          <IconButton
                            variant="action"
                            icon={<Pencil className="h-4 w-4" aria-hidden />}
                            aria-label={t('common.edit')}
                            title={t('common.edit')}
                            onClick={() => openEdit(item)}
                            disabled={busy}
                          />
                        ) : null}
                        {!isSystemGroup ? (
                          <IconButton
                            variant="action"
                            icon={<Trash2 className="h-4 w-4" aria-hidden />}
                            aria-label={t('admin.standards.deleteItem')}
                            title={t('admin.standards.deleteItem')}
                            onClick={() => setDeleteKey(item.configKey)}
                            disabled={busy}
                          />
                        ) : null}
                      </>
                    ) : undefined
                  }
                />
              );
            })}
          </ListContainer>
        )}
      </OrganizationManageSection>

      {formOpen ? (
        <Modal
          isOpen
          onClose={closeForm}
          title={editingItem ? t('common.edit') : t('admin.standards.addPermissionGroup')}
          size="lg"
          footer={
            <ModalFooter
              onCancel={closeForm}
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
              required
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
              <StandardPermissionTypePicker
                selected={perms}
                onChange={setPerms}
                disabled={busy}
                enabledModules={enabledModules}
              />
            </div>
          </div>
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={deleteKey != null}
        onClose={() => setDeleteKey(null)}
        onConfirm={() => void handleDelete()}
        message={t('admin.standards.deleteItemConfirm')}
        confirmText={t('admin.standards.deleteItem')}
        cancelText={t('common.cancel')}
        isLoading={busy}
      />
    </>
  );
}
