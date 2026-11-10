/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Pencil } from 'lucide-react';
import { useState } from 'react';
import { StandardDetailAction } from '@/components/admin/standards/StandardDetailCardHeader';
import { StandardOrgGroupFunctionGrantFields } from '@/components/admin/standards/StandardOrgGroupFunctionGrantFields';
import { StandardOrgGroupFunctionGrantSummary } from '@/components/admin/standards/StandardOrgGroupFunctionGrantSummary';
import type { StandardContentState } from '@/components/admin/standards/useStandardContent';
import { Card } from '@/components/ui/card';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import {
  mergeOrgGroupGrantsIntoAll,
  type OrgGroupFunctionGrantBySlot,
  orgGroupFunctionGrantsEqual,
  readOrgGroupFunctionGrantsFromItems,
} from '@/lib/admin/standardFunctionGrants';
import type { FunctionGrantMembershipType } from '@/lib/api/standards';
import { standardsApi } from '@/lib/api/standards';

interface Props {
  state: StandardContentState;
  configKey: string;
}

/** Function grants of one organization group; edited independently of the other cards. */
export function StandardOrgGroupFunctionGrantsCard({ state, configKey }: Props) {
  const t = useTranslation();
  const { tenantId, standardId, items, editable, busy, runMutation } = state;
  const [draft, setDraft] = useState<OrgGroupFunctionGrantBySlot | null>(null);

  const onSlotChange = (slot: FunctionGrantMembershipType, functionConfigKey: string) => {
    setDraft((prev) => {
      const next = { ...(prev ?? {}) };
      if (functionConfigKey) {
        next[slot] = functionConfigKey;
      } else {
        delete next[slot];
      }
      return next;
    });
  };

  const handleSave = async () => {
    if (!draft) return;
    const before = readOrgGroupFunctionGrantsFromItems(items, configKey);
    if (orgGroupFunctionGrantsEqual(before, draft)) {
      setDraft(null);
      return;
    }
    const ok = await runMutation(
      () =>
        standardsApi.updateFunctionGrants(tenantId, standardId, {
          grants: mergeOrgGroupGrantsIntoAll(items, configKey, draft),
        }),
      'admin.standards.itemSaved',
      'admin.standards.errors.saveFailed',
    );
    if (ok) setDraft(null);
  };

  return (
    <Card className="p-3 sm:p-4">
      <StandardOrgGroupFunctionGrantSummary
        items={items}
        organizationGroupConfigKey={configKey}
        action={
          editable ? (
            <StandardDetailAction
              label={t('common.edit')}
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={() => setDraft(readOrgGroupFunctionGrantsFromItems(items, configKey))}
              disabled={busy}
            />
          ) : undefined
        }
      />
      {draft ? (
        <Modal
          isOpen
          onClose={() => setDraft(null)}
          title={t('admin.standards.functions.grantsTitle')}
          size="lg"
          footer={
            <ModalFooter
              onCancel={() => setDraft(null)}
              onSave={() => void handleSave()}
              isLoading={busy}
            />
          }
        >
          <StandardOrgGroupFunctionGrantFields
            tenantId={tenantId}
            items={items}
            valueBySlot={draft}
            onSlotChange={onSlotChange}
            disabled={busy}
          />
        </Modal>
      ) : null}
    </Card>
  );
}
