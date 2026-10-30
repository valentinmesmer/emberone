/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ReactNode } from 'react';
import type { WorkflowDto } from '@/types/workflow';

export type WorkflowInputTranslateFn = (
  key: string,
  params?: Record<string, string | number>,
) => string;

/** Profile snapshot for resolving “before” values on open workflows. */
export interface WorkflowInputSelfContext {
  id: number;
  firstName?: string | null;
  lastName?: string | null;
  contactInformation?: {
    street?: string;
    city?: string;
    postalCode?: string;
    country?: string;
  };
}

export interface WorkflowInputDisplayContext {
  workflow: WorkflowDto;
  self?: WorkflowInputSelfContext;
  t: WorkflowInputTranslateFn;
}

/**
 * One plugin per {@link WorkflowDto.workflowTypeId}. Register in {@link ./registry.ts}.
 */
export interface WorkflowInputDisplayPlugin<P = unknown> {
  workflowTypeId: number;
  parsePayload(raw: string | null | undefined): P | null;
  /**
   * When true (default), parsed payload counts as “proof context” for the media section
   * (show proofs block even before attachments load).
   */
  treatParsedAsProof?: boolean;
  render(ctx: WorkflowInputDisplayContext, payload: P): ReactNode;
}
