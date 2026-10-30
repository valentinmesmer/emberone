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
import { StandardOrgGroupFunctionGrantFields } from '@/components/admin/standards/StandardOrgGroupFunctionGrantFields';
import { StandardOrgGroupFunctionGrantSummary } from '@/components/admin/standards/StandardOrgGroupFunctionGrantSummary';
import { StandardPermissionGroupMappingPicker } from '@/components/admin/standards/StandardPermissionGroupMappingPicker';
import {
  findStandardItem,
  getMappedPermissionGroupKeys,
  isSystemStandardOrgGroupItem,
  standardGroupTypeLabel,
  standardOrgGroupSupportsFunctionGrants,
} from '@/components/admin/standards/standardContentUtils';
import { syncStandardGroupMappings } from '@/components/admin/standards/syncStandardGroupMappings';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { Input } from '@/components/ui/input';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyState, NotFoundState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import {
  mergeOrgGroupGrantsIntoAll,
  type OrgGroupFunctionGrantBySlot,
  orgGroupFunctionGrantsEqual,
  readOrgGroupFunctionGrantsFromItems,
} from '@/lib/admin/standardFunctionGrants';
import type { FunctionGrantMembershipType } from '@/lib/api/standards';
import { standardsApi } from '@/lib/api/standards';
import {
  getAdminStandardDetailPath,
  getAdminStandardPermissionGroupPath,
} from '@/lib/navigation/adminWorkspace';

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
  const permGroupItems = useMemo(
    () => items.filter((i) => i.domainKey === 'permission-groups'),
    [items],
  );
  const a = item?.attributes ?? {};

  const mappedPermKeys = useMemo(
    () => (item ? getMappedPermissionGroupKeys(items, configKey) : []),
    [item, items, configKey],
  );
  const mappedPermGroups = useMemo(
    () => permGroupItems.filter((pg) => mappedPermKeys.includes(pg.configKey)),
    [permGroupItems, mappedPermKeys],
  );

  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [permSelectionOverride, setPermSelectionOverride] = useState<Set<string> | null>(null);
  const [grantDraft, setGrantDraft] = useState<OrgGroupFunctionGrantBySlot | null>(null);

  const isSystemGroup = isSystemStandardOrgGroupItem(item);
  const supportsFunctionGrants = standardOrgGroupSupportsFunctionGrants(item);

  if (!item) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  const editPermSelection =
    permSelectionOverride ?? new Set(getMappedPermissionGroupKeys(items, configKey));

  const editGrantSlots = grantDraft ?? readOrgGroupFunctionGrantsFromItems(items, configKey);

  const openEdit = () => {
    setName(item.title);
    setDescription(a.description ?? '');
    setPermSelectionOverride(null);
    setGrantDraft(readOrgGroupFunctionGrantsFromItems(items, configKey));
    setEditOpen(true);
  };

  const handleSave = async () => {
    if (!name.trim()) return;
    const currentKeys = new Set(getMappedPermissionGroupKeys(items, configKey));
    const grantsBefore = readOrgGroupFunctionGrantsFromItems(items, configKey);
    const grantsAfter = grantDraft ?? grantsBefore;
    const ok = await runMutation(
      async () => {
        await standardsApi.updateOrgGroup(tenantId, standardId, configKey, {
          name: name.trim(),
          description: description.trim() || undefined,
        });
        let content = await syncStandardGroupMappings(
          tenantId,
          standardId,
          configKey,
          currentKeys,
          editPermSelection,
        );
        if (supportsFunctionGrants && !orgGroupFunctionGrantsEqual(grantsBefore, grantsAfter)) {
          content = await standardsApi.updateFunctionGrants(tenantId, standardId, {
            grants: mergeOrgGroupGrantsIntoAll(content.items, configKey, grantsAfter),
          });
        }
        return content;
      },
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

  const onGrantSlotChange = (slot: FunctionGrantMembershipType, functionConfigKey: string) => {
    setGrantDraft((prev) => {
      const base = prev ?? readOrgGroupFunctionGrantsFromItems(items, configKey);
      const next = { ...base };
      if (functionConfigKey) {
        next[slot] = functionConfigKey;
      } else {
        delete next[slot];
      }
      return next;
    });
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
        <FieldDisplay
          label={t('admin.standards.description')}
          value={a.description}
          fallback={t('organizationManagement.permissionGroups.noDescription')}
        />
      </Card>

      {supportsFunctionGrants ? (
        <Card className="p-3 sm:p-4">
          <StandardOrgGroupFunctionGrantSummary
            items={items}
            organizationGroupConfigKey={configKey}
          />
        </Card>
      ) : null}

      <Card className="p-3 sm:p-4">
        <OrganizationManageSection
          embedded
          title={t('organizationManagement.organizationGroups.permissionGroups')}
        >
          {mappedPermGroups.length === 0 ? (
            <EmptyState>
              {t('organizationManagement.organizationGroups.noPermissionGroupsAssigned')}
            </EmptyState>
          ) : (
            <ListContainer label={t('organizationManagement.organizationGroups.permissionGroups')}>
              {mappedPermGroups.map((pg) => (
                <ListRow
                  key={pg.configKey}
                  compact
                  href={getAdminStandardPermissionGroupPath(tenantId, standardId, pg.configKey)}
                  title={pg.title}
                />
              ))}
            </ListContainer>
          )}
        </OrganizationManageSection>
      </Card>

      {editOpen ? (
        <Modal
          isOpen
          onClose={() => setEditOpen(false)}
          title={t('common.edit')}
          size="xl"
          footer={
            <ModalFooter
              onCancel={() => setEditOpen(false)}
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
            />
            <Textarea
              label={t('admin.standards.description')}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              disabled={busy}
            />
            <StandardPermissionGroupMappingPicker
              permissionGroups={permGroupItems}
              selected={editPermSelection}
              onChange={setPermSelectionOverride}
              disabled={busy}
            />
            {supportsFunctionGrants ? (
              <StandardOrgGroupFunctionGrantFields
                tenantId={tenantId}
                items={items}
                valueBySlot={editGrantSlots}
                onSlotChange={onGrantSlotChange}
                disabled={busy}
              />
            ) : null}
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
