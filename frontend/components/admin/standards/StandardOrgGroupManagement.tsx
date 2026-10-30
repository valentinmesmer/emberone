/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil, Plus, Trash2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useMemo, useState } from 'react';
import { StandardOrgGroupFunctionGrantFields } from '@/components/admin/standards/StandardOrgGroupFunctionGrantFields';
import { StandardPermissionGroupMappingPicker } from '@/components/admin/standards/StandardPermissionGroupMappingPicker';
import {
  availableOrgGroupTypesForCreate,
  findNewOrgGroupConfigKey,
  getMappedPermissionGroupKeys,
  getMappedPermissionGroupTitles,
  isSystemStandardOrgGroupItem,
  standardGroupTypeLabel,
  standardOrgGroupSupportsFunctionGrants,
} from '@/components/admin/standards/standardContentUtils';
import { syncStandardGroupMappings } from '@/components/admin/standards/syncStandardGroupMappings';
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
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import {
  mergeOrgGroupGrantsIntoAll,
  type OrgGroupFunctionGrantBySlot,
  orgGroupFunctionGrantsEqual,
  readOrgGroupFunctionGrantsFromItems,
} from '@/lib/admin/standardFunctionGrants';
import {
  formatOrgGroupRemovalImpactMessage,
  removalImpactRequiresConfirmation,
} from '@/lib/admin/standardImpact';
import type { FunctionGrantMembershipType } from '@/lib/api/standards';
import {
  type OrganizationGroupTypeName,
  type OrgGroupRemovalImpactDto,
  type StandardContentDto,
  type StandardContentItemDto,
  standardsApi,
} from '@/lib/api/standards';
import { getAdminStandardOrgGroupPath } from '@/lib/navigation/adminWorkspace';

interface StandardOrgGroupManagementProps {
  state: StandardContentState;
  embedded?: boolean;
}

export function StandardOrgGroupManagement({
  state,
  embedded = false,
}: StandardOrgGroupManagementProps) {
  const t = useTranslation();
  const router = useRouter();
  const { tenantId, standardId, items, editable, busy, runMutation } = state;

  const orgGroupItems = useMemo(
    () => items.filter((i) => i.domainKey === 'organization-groups'),
    [items],
  );
  const permGroupItems = useMemo(
    () => items.filter((i) => i.domainKey === 'permission-groups'),
    [items],
  );

  const [formOpen, setFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<StandardContentItemDto | null>(null);
  const [deleteKey, setDeleteKey] = useState<string | null>(null);
  const [deleteImpact, setDeleteImpact] = useState<OrgGroupRemovalImpactDto | null>(null);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<OrganizationGroupTypeName>('CUSTOM');
  const [selectedPermKeys, setSelectedPermKeys] = useState<Set<string>>(new Set());
  const [permSelectionOverride, setPermSelectionOverride] = useState<Set<string> | null>(null);
  const [grantDraft, setGrantDraft] = useState<OrgGroupFunctionGrantBySlot | null>(null);

  const editPermSelection = useMemo(() => {
    if (!editingItem) return selectedPermKeys;
    if (permSelectionOverride !== null) return permSelectionOverride;
    return new Set(getMappedPermissionGroupKeys(items, editingItem.configKey));
  }, [editingItem, permSelectionOverride, items, selectedPermKeys]);

  const displayPermSelection = editingItem ? editPermSelection : selectedPermKeys;

  const editGrantSlots = useMemo(() => {
    if (!editingItem) return {};
    return grantDraft ?? readOrgGroupFunctionGrantsFromItems(items, editingItem.configKey);
  }, [editingItem, grantDraft, items]);

  const availableTypes = useMemo(() => availableOrgGroupTypesForCreate(items), [items]);

  useEffect(() => {
    if (!deleteKey) return;
    let cancelled = false;
    void standardsApi.getOrgGroupRemovalImpact(tenantId, standardId, deleteKey).then((impact) => {
      if (!cancelled) setDeleteImpact(impact);
    });
    return () => {
      cancelled = true;
    };
  }, [deleteKey, tenantId, standardId]);

  const openDelete = (configKey: string) => {
    setDeleteKey(configKey);
    setDeleteImpact(null);
  };

  const closeDelete = () => {
    setDeleteKey(null);
    setDeleteImpact(null);
  };

  const deleteConfirmMessage = deleteImpact
    ? formatOrgGroupRemovalImpactMessage(deleteImpact, t)
    : t('admin.standards.deleteItemConfirm');

  const openCreate = () => {
    setEditingItem(null);
    setName('');
    setDescription('');
    setType(availableTypes.includes('CUSTOM') ? 'CUSTOM' : (availableTypes[0] ?? 'CUSTOM'));
    setSelectedPermKeys(new Set());
    setPermSelectionOverride(null);
    setGrantDraft(null);
    setFormOpen(true);
  };

  const openEdit = (item: StandardContentItemDto) => {
    const a = item.attributes ?? {};
    setEditingItem(item);
    setName(item.title);
    setDescription(a.description ?? '');
    setType((a.type as OrganizationGroupTypeName) ?? 'CUSTOM');
    setPermSelectionOverride(null);
    setGrantDraft(readOrgGroupFunctionGrantsFromItems(items, item.configKey));
    setFormOpen(true);
  };

  const closeForm = () => {
    setFormOpen(false);
    setEditingItem(null);
    setPermSelectionOverride(null);
    setGrantDraft(null);
  };

  const handleSave = async () => {
    if (!name.trim()) return;
    const trimmedName = name.trim();
    const ok = await runMutation(
      async () => {
        let content: StandardContentDto;
        let orgKey: string;

        if (editingItem) {
          orgKey = editingItem.configKey;
          const currentKeys = new Set(getMappedPermissionGroupKeys(items, orgKey));
          const grantsBefore = readOrgGroupFunctionGrantsFromItems(items, orgKey);
          const grantsAfter = grantDraft ?? grantsBefore;
          await standardsApi.updateOrgGroup(tenantId, standardId, orgKey, {
            name: trimmedName,
            description: description.trim() || undefined,
          });
          content = await syncStandardGroupMappings(
            tenantId,
            standardId,
            orgKey,
            currentKeys,
            displayPermSelection,
          );
          if (
            standardOrgGroupSupportsFunctionGrants(editingItem) &&
            !orgGroupFunctionGrantsEqual(grantsBefore, grantsAfter)
          ) {
            content = await standardsApi.updateFunctionGrants(tenantId, standardId, {
              grants: mergeOrgGroupGrantsIntoAll(content.items, orgKey, grantsAfter),
            });
          }
          return content;
        }

        const beforeItems = items;
        content = await standardsApi.addOrgGroup(tenantId, standardId, {
          name: trimmedName,
          description: description.trim() || undefined,
          type,
        });
        orgKey = findNewOrgGroupConfigKey(beforeItems, content.items, trimmedName) ?? '';
        if (!orgKey) {
          return content;
        }
        if (selectedPermKeys.size === 0) {
          return content;
        }
        return syncStandardGroupMappings(tenantId, standardId, orgKey, new Set(), selectedPermKeys);
      },
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) closeForm();
  };

  const handleDelete = async () => {
    if (!deleteKey) return;
    const ok = await runMutation(
      () => standardsApi.deleteOrgGroup(tenantId, standardId, deleteKey),
      'admin.standards.itemDeleted',
      'admin.standards.errors.itemDeleteFailed',
    );
    if (ok) closeDelete();
  };

  const setPermSelection = (next: Set<string>) => {
    if (editingItem) {
      setPermSelectionOverride(next);
    } else {
      setSelectedPermKeys(next);
    }
  };

  return (
    <>
      <OrganizationManageSection
        embedded={embedded}
        title={t('admin.standards.sections.organizationGroups')}
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
        {orgGroupItems.length === 0 ? (
          <EmptyStateCard>{t('admin.standards.detail.empty')}</EmptyStateCard>
        ) : (
          <ListContainer label={t('admin.standards.sections.organizationGroups')}>
            {orgGroupItems.map((item) => {
              const mappedTitles = getMappedPermissionGroupTitles(items, item.configKey);
              const isSystemGroup = isSystemStandardOrgGroupItem(item);
              return (
                <ListRow
                  key={item.configKey}
                  compact
                  keepActionsOnMobile
                  onClick={() =>
                    router.push(getAdminStandardOrgGroupPath(tenantId, standardId, item.configKey))
                  }
                  title={item.title}
                  subtitle={
                    mappedTitles.length > 0 ? (
                      <span className="line-clamp-2">{mappedTitles.join(', ')}</span>
                    ) : undefined
                  }
                  badges={
                    item.subtitle || isSystemGroup ? (
                      <>
                        {item.subtitle ? (
                          <Badge tone="neutral">{standardGroupTypeLabel(t, item.subtitle)}</Badge>
                        ) : null}
                        {isSystemGroup ? (
                          <Badge tone="accent">{t('admin.standards.systemOrgGroupRequired')}</Badge>
                        ) : null}
                      </>
                    ) : undefined
                  }
                  actions={
                    editable ? (
                      <>
                        <IconButton
                          variant="action"
                          icon={<Pencil className="h-4 w-4" aria-hidden />}
                          aria-label={t('common.edit')}
                          title={t('common.edit')}
                          onClick={(e) => {
                            e.stopPropagation();
                            openEdit(item);
                          }}
                          disabled={busy}
                        />
                        {!isSystemGroup ? (
                          <IconButton
                            variant="action"
                            icon={<Trash2 className="h-4 w-4" aria-hidden />}
                            aria-label={t('admin.standards.deleteItem')}
                            title={t('admin.standards.deleteItem')}
                            onClick={(e) => {
                              e.stopPropagation();
                              openDelete(item.configKey);
                            }}
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
          title={editingItem ? t('common.edit') : t('admin.standards.addOrgGroup')}
          size="xl"
          footer={
            <ModalFooter
              onCancel={closeForm}
              onSave={() => void handleSave()}
              isValid={name.trim().length > 0}
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
            {!editingItem ? (
              <Select
                label={t('admin.standards.typeLabel')}
                value={type}
                disabled={busy || availableTypes.length <= 1}
                options={availableTypes.map((tp) => ({
                  value: tp,
                  label: standardGroupTypeLabel(t, tp),
                }))}
                onChange={(e) => setType(e.target.value as OrganizationGroupTypeName)}
              />
            ) : null}
            {!editingItem && availableTypes.length === 1 && availableTypes[0] === 'CUSTOM' ? (
              <p className="text-xs text-[var(--text-secondary)]">
                {t('admin.standards.systemOrgGroupsAlreadyPresent')}
              </p>
            ) : null}
            <StandardPermissionGroupMappingPicker
              permissionGroups={permGroupItems}
              selected={displayPermSelection}
              onChange={setPermSelection}
              disabled={busy}
            />
            {editingItem && standardOrgGroupSupportsFunctionGrants(editingItem) ? (
              <StandardOrgGroupFunctionGrantFields
                tenantId={tenantId}
                items={items}
                valueBySlot={editGrantSlots}
                onSlotChange={(slot: FunctionGrantMembershipType, functionConfigKey: string) => {
                  setGrantDraft((prev) => {
                    const base =
                      prev ?? readOrgGroupFunctionGrantsFromItems(items, editingItem.configKey);
                    const next = { ...base };
                    if (functionConfigKey) {
                      next[slot] = functionConfigKey;
                    } else {
                      delete next[slot];
                    }
                    return next;
                  });
                }}
                disabled={busy}
              />
            ) : null}
          </div>
        </Modal>
      ) : null}

      <ConfirmDialog
        isOpen={deleteKey != null}
        onClose={closeDelete}
        onConfirm={() => void handleDelete()}
        message={deleteConfirmMessage}
        confirmText={t('admin.standards.deleteItem')}
        cancelText={t('common.cancel')}
        variant={
          deleteImpact && removalImpactRequiresConfirmation(deleteImpact) ? 'danger' : 'info'
        }
        isLoading={busy}
      />
    </>
  );
}
