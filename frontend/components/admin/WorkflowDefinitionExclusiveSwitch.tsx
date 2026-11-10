/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { SwitchField } from '@/components/ui/switch';
import { useTranslation } from '@/hooks/useTranslation';

export interface WorkflowDefinitionExclusiveSwitchProps {
  checked: boolean;
  onCheckedChange: (exclusive: boolean) => void;
}

/** "Only these orgs decide": a workflow-wide switch, not part of a single routing rule. */
export function WorkflowDefinitionExclusiveSwitch({
  checked,
  onCheckedChange,
}: WorkflowDefinitionExclusiveSwitchProps) {
  const t = useTranslation();
  return (
    <SwitchField
      label={t('admin.workflowDefinitions.staticExclusive')}
      description={t('admin.workflowDefinitions.staticExclusiveHint')}
      checked={checked}
      onCheckedChange={onCheckedChange}
    />
  );
}
