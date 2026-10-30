/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useTranslation } from '@/hooks/useTranslation';
import type { WorkflowDto } from '@/types/workflow';
import { getWorkflowInputDisplayPlugin } from './registry';
import type { WorkflowInputSelfContext } from './types';

export interface WorkflowInputDisplayProps {
  workflow: WorkflowDto;
  self?: WorkflowInputSelfContext;
}

/**
 * Renders workflow-type-specific input data (payload fields) on the detail page.
 * Add a plugin in {@link ./registry.ts} for new workflow types.
 */
export function WorkflowInputDisplay({ workflow, self }: WorkflowInputDisplayProps) {
  const t = useTranslation();
  const plugin = getWorkflowInputDisplayPlugin(workflow.workflowTypeId);
  if (!plugin) return null;
  const payload = plugin.parsePayload(workflow.payload);
  if (payload == null) return null;
  return <>{plugin.render({ workflow, self, t }, payload)}</>;
}

export { workflowInputPayloadCountsAsProof } from './registry';
