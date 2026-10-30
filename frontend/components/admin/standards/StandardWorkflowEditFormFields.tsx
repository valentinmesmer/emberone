/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Select } from '@/components/ui/select';
import { SwitchField } from '@/components/ui/switch';
import { useTranslation } from '@/hooks/useTranslation';
import type { StandardContentItemDto } from '@/lib/api/standards';
import { cn } from '@/lib/utils/cn';

export interface StandardWorkflowEditFormFieldsProps {
  approverOrgGroupConfigKey: string;
  onApproverOrgGroupConfigKeyChange: (value: string) => void;
  orgGroupItems: StandardContentItemDto[];
  enabled: boolean;
  onEnabledChange: (enabled: boolean) => void;
  selfApproveAllowed: boolean;
  onSelfApproveAllowedChange: (allowed: boolean) => void;
  autoApprove: boolean;
  onAutoApproveChange: (autoApprove: boolean) => void;
  disabled?: boolean;
}

export function StandardWorkflowEditFormFields({
  approverOrgGroupConfigKey,
  onApproverOrgGroupConfigKeyChange,
  orgGroupItems,
  enabled,
  onEnabledChange,
  selfApproveAllowed,
  onSelfApproveAllowedChange,
  autoApprove,
  onAutoApproveChange,
  disabled = false,
}: StandardWorkflowEditFormFieldsProps) {
  const t = useTranslation();

  return (
    <div className="space-y-4">
      <div className="overflow-hidden rounded-lg border border-[var(--border-color)]/60 px-3">
        <SwitchField
          label={t('admin.standards.fields.enabled')}
          description={t('admin.standards.fields.enabledHint')}
          checked={enabled}
          onCheckedChange={onEnabledChange}
          disabled={disabled}
        />
      </div>

      <div className="space-y-1.5">
        <Select
          label={t('admin.standards.approverLabel')}
          value={approverOrgGroupConfigKey}
          disabled={disabled}
          options={orgGroupItems.map((i) => ({ value: i.configKey, label: i.title }))}
          onChange={(e) => onApproverOrgGroupConfigKeyChange(e.target.value)}
        />
        <p className="text-xs text-[var(--text-secondary)]">
          {t('admin.standards.approverGroupHint')}
        </p>
      </div>

      <div
        className={cn(
          'divide-y divide-[var(--border-color)]/60 overflow-hidden rounded-lg border border-[var(--border-color)]/60',
          disabled && 'pointer-events-none opacity-70',
        )}
      >
        <div className="px-3">
          <SwitchField
            label={t('admin.standards.fields.selfApproveAllowed')}
            description={t('admin.standards.fields.selfApproveAllowedHint')}
            checked={selfApproveAllowed}
            onCheckedChange={onSelfApproveAllowedChange}
            disabled={disabled}
          />
        </div>
        <div className="px-3">
          <SwitchField
            label={t('admin.standards.fields.autoApprove')}
            description={t('admin.standards.fields.autoApproveHint')}
            checked={autoApprove}
            onCheckedChange={onAutoApproveChange}
            disabled={disabled}
          />
        </div>
      </div>
    </div>
  );
}
