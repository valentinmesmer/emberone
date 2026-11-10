/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { Select } from '@/components/ui/select';
import { SwitchField } from '@/components/ui/switch';
import { useTranslation } from '@/hooks/useTranslation';
import type { StandardContentItemDto, StandardWorkflowSlotDto } from '@/lib/api/standards';
import { cn } from '@/lib/utils/cn';
import { workflowSlotRoleHint, workflowSlotRoleLabel } from '@/lib/workflows/slotRoles';

export interface StandardWorkflowEditFormFieldsProps {
  /** Workflow type enum key; selects the type-specific role names. */
  typeKey: string;
  /** One entry per approver slot role of the workflow type, in display order. */
  slots: StandardWorkflowSlotDto[];
  onSlotChange: (slotRole: string, patch: Partial<StandardWorkflowSlotDto>) => void;
  orgGroupItems: StandardContentItemDto[];
  disabled?: boolean;
}

export function StandardWorkflowEditFormFields({
  typeKey,
  slots,
  onSlotChange,
  orgGroupItems,
  disabled = false,
}: StandardWorkflowEditFormFieldsProps) {
  const t = useTranslation();

  return (
    <div className="space-y-5">
      {slots.map((slot) => {
        const label = workflowSlotRoleLabel(slot.slotRole, t, typeKey);
        const hint = workflowSlotRoleHint(slot.slotRole, t, typeKey);
        return (
          <section key={slot.slotRole} className="space-y-3" aria-label={label}>
            <div>
              <h3 className="text-sm font-semibold text-[var(--text-primary)]">{label}</h3>
              {hint ? <p className="text-xs text-[var(--text-secondary)]">{hint}</p> : null}
            </div>

            <Select
              label={t('admin.standards.approverLabel')}
              value={slot.approverOrgGroupConfigKey}
              disabled={disabled}
              options={orgGroupItems.map((i) => ({ value: i.configKey, label: i.title }))}
              onChange={(e) =>
                onSlotChange(slot.slotRole, { approverOrgGroupConfigKey: e.target.value })
              }
              required
            />

            <div
              className={cn(
                'divide-y divide-[var(--border-color)]/60 overflow-hidden rounded-lg border border-[var(--border-color)]/60',
                disabled && 'pointer-events-none opacity-70',
              )}
            >
              <div className="px-3">
                <SwitchField
                  label={t('admin.standards.fields.autoApprove')}
                  description={t('admin.standards.fields.autoApproveHint')}
                  checked={slot.autoApprove}
                  onCheckedChange={(autoApprove) => onSlotChange(slot.slotRole, { autoApprove })}
                  disabled={disabled}
                />
              </div>
              <div className="px-3">
                <SwitchField
                  label={t('admin.standards.fields.selfApproveAllowed')}
                  description={t('admin.standards.fields.selfApproveAllowedHint')}
                  checked={slot.selfApproveAllowed}
                  onCheckedChange={(selfApproveAllowed) =>
                    onSlotChange(slot.slotRole, { selfApproveAllowed })
                  }
                  disabled={disabled}
                />
              </div>
            </div>
          </section>
        );
      })}
    </div>
  );
}
