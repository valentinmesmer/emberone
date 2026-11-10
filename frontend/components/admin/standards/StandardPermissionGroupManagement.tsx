/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Plus } from 'lucide-react';
import { useMemo, useState } from 'react';
import { StandardPermissionTypePicker } from '@/components/admin/standards/StandardPermissionTypePicker';
import {
  isSystemStandardPermissionGroupItem,
  standardPermissionGroupTypeLabel,
} from '@/components/admin/standards/standardContentUtils';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyStateCard } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { Textarea } from '@/components/ui/textarea';
import { useTranslation } from '@/hooks/useTranslation';
import { standardsApi } from '@/lib/api/standards';
import { getAdminStandardPermissionGroupPath } from '@/lib/navigation/adminWorkspace';
import { formBlockers } from '@/lib/utils';

interface StandardPermissionGroupManagementProps {
  state: StandardContentState;
  embedded?: boolean;
}

export function StandardPermissionGroupManagement({
  state,
  embedded = false,
}: StandardPermissionGroupManagementProps) {
  const t = useTranslation();
  const { tenantId, standardId, items, editable, busy, runMutation, enabledModules } = state;

  const permGroupItems = useMemo(
    () => items.filter((i) => i.domainKey === 'permission-groups'),
    [items],
  );

  const [formOpen, setFormOpen] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [perms, setPerms] = useState<Set<string>>(new Set());

  const openCreate = () => {
    setName('');
    setDescription('');
    setPerms(new Set());
    setFormOpen(true);
  };

  const closeForm = () => setFormOpen(false);

  const blockers = formBlockers([[t('admin.standards.name'), name.trim() !== '']]);

  const handleSave = async () => {
    if (blockers.length > 0) return;
    const ok = await runMutation(
      () =>
        standardsApi.addPermissionGroup(tenantId, standardId, {
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
              return (
                <ListRow
                  key={item.configKey}
                  compact
                  href={getAdminStandardPermissionGroupPath(tenantId, standardId, item.configKey)}
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
          title={t('admin.standards.addPermissionGroup')}
          size="lg"
          footer={
            <ModalFooter
              onCancel={closeForm}
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
    </>
  );
}
