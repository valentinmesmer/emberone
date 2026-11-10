/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Plus } from 'lucide-react';
import { useMemo, useState } from 'react';
import { StandardPermissionGroupMappingPicker } from '@/components/admin/standards/StandardPermissionGroupMappingPicker';
import {
  availableOrgGroupTypesForCreate,
  findNewOrgGroupConfigKey,
  getMappedPermissionGroupTitles,
  isSystemStandardOrgGroupItem,
  standardGroupTypeLabel,
} from '@/components/admin/standards/standardContentUtils';
import { syncStandardGroupMappings } from '@/components/admin/standards/syncStandardGroupMappings';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Select } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import { type OrganizationGroupTypeName, standardsApi } from '@/lib/api/standards';
import { getAdminStandardOrgGroupPath } from '@/lib/navigation/adminWorkspace';
import { formBlockers } from '@/lib/utils';

interface StandardOrgGroupManagementProps {
  state: StandardContentState;
  embedded?: boolean;
}

export function StandardOrgGroupManagement({
  state,
  embedded = false,
}: StandardOrgGroupManagementProps) {
  const t = useTranslation();
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
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [type, setType] = useState<OrganizationGroupTypeName>('CUSTOM');
  const [selectedPermKeys, setSelectedPermKeys] = useState<Set<string>>(new Set());

  const availableTypes = useMemo(() => availableOrgGroupTypesForCreate(items), [items]);

  const openCreate = () => {
    setName('');
    setDescription('');
    setType(availableTypes.includes('CUSTOM') ? 'CUSTOM' : (availableTypes[0] ?? 'CUSTOM'));
    setSelectedPermKeys(new Set());
    setFormOpen(true);
  };

  const closeForm = () => setFormOpen(false);

  const blockers = formBlockers([[t('admin.standards.name'), name.trim() !== '']]);

  const handleSave = async () => {
    if (blockers.length > 0) return;
    const trimmedName = name.trim();
    const ok = await runMutation(
      async () => {
        const beforeItems = items;
        const content = await standardsApi.addOrgGroup(tenantId, standardId, {
          name: trimmedName,
          description: description.trim() || undefined,
          type,
        });
        const orgKey = findNewOrgGroupConfigKey(beforeItems, content.items, trimmedName) ?? '';
        if (!orgKey || selectedPermKeys.size === 0) {
          return content;
        }
        return syncStandardGroupMappings(tenantId, standardId, orgKey, new Set(), selectedPermKeys);
      },
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) closeForm();
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
                  href={getAdminStandardOrgGroupPath(tenantId, standardId, item.configKey)}
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
          title={t('admin.standards.addOrgGroup')}
          size="xl"
          footer={
            <ModalFooter
              onCancel={closeForm}
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
            {availableTypes.length === 1 && availableTypes[0] === 'CUSTOM' ? (
              <p className="text-xs text-[var(--text-secondary)]">
                {t('admin.standards.systemOrgGroupsAlreadyPresent')}
              </p>
            ) : null}
            <StandardPermissionGroupMappingPicker
              permissionGroups={permGroupItems}
              selected={selectedPermKeys}
              onChange={setSelectedPermKeys}
              disabled={busy}
            />
          </div>
        </Modal>
      ) : null}
    </>
  );
}
