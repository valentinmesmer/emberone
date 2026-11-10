/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Pencil } from 'lucide-react';
import { useMemo, useState } from 'react';
import { StandardDetailAction } from '@/components/admin/standards/StandardDetailCardHeader';
import { StandardPermissionGroupMappingPicker } from '@/components/admin/standards/StandardPermissionGroupMappingPicker';
import { getMappedPermissionGroupKeys } from '@/components/admin/standards/standardContentUtils';
import { syncStandardGroupMappings } from '@/components/admin/standards/syncStandardGroupMappings';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { OrganizationManageSection } from '@/components/organization/OrganizationManageSection';
import { Card } from '@/components/ui/card';
import { ListContainer, ListRow } from '@/components/ui/list';
import { EmptyState } from '@/components/ui/message-cards';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import { getAdminStandardPermissionGroupPath } from '@/lib/navigation/adminWorkspace';

interface Props {
  state: StandardContentState;
  configKey: string;
}

/** Permission groups mapped to one organization group; edited independently of the other cards. */
export function StandardOrgGroupPermissionGroupsCard({ state, configKey }: Props) {
  const t = useTranslation();
  const { tenantId, standardId, items, editable, busy, runMutation } = state;
  const [selection, setSelection] = useState<Set<string> | null>(null);

  const permGroupItems = useMemo(
    () => items.filter((i) => i.domainKey === 'permission-groups'),
    [items],
  );
  const mappedKeys = useMemo(
    () => getMappedPermissionGroupKeys(items, configKey),
    [items, configKey],
  );
  const mappedGroups = useMemo(
    () => permGroupItems.filter((pg) => mappedKeys.includes(pg.configKey)),
    [permGroupItems, mappedKeys],
  );

  const handleSave = async () => {
    if (!selection) return;
    const ok = await runMutation(
      () =>
        syncStandardGroupMappings(tenantId, standardId, configKey, new Set(mappedKeys), selection),
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) setSelection(null);
  };

  const title = t('organizationManagement.organizationGroups.permissionGroups');

  return (
    <Card className="p-3 sm:p-4">
      <OrganizationManageSection
        embedded
        title={title}
        actionInline
        action={
          editable ? (
            <StandardDetailAction
              label={t('common.edit')}
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={() => setSelection(new Set(mappedKeys))}
              disabled={busy}
            />
          ) : undefined
        }
      >
        {mappedGroups.length === 0 ? (
          <EmptyState>
            {t('organizationManagement.organizationGroups.noPermissionGroupsAssigned')}
          </EmptyState>
        ) : (
          <ListContainer label={title}>
            {mappedGroups.map((pg) => (
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
      {selection ? (
        <Modal
          isOpen
          onClose={() => setSelection(null)}
          title={title}
          size="lg"
          footer={
            <ModalFooter
              onCancel={() => setSelection(null)}
              onSave={() => void handleSave()}
              isLoading={busy}
            />
          }
        >
          <StandardPermissionGroupMappingPicker
            permissionGroups={permGroupItems}
            selected={selection}
            onChange={setSelection}
            disabled={busy}
          />
        </Modal>
      ) : null}
    </Card>
  );
}
