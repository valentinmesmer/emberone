/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

const OPEN = 'OPEN';
const AWAITING_INPUT_CHANGE = 'AWAITING_INPUT_CHANGE';

/** Open or awaiting input change - still an active non-terminal instance. */
export function isWorkflowStatusActive(status: string | null | undefined): boolean {
  const u = (status ?? '').toUpperCase();
  return u === OPEN || u === AWAITING_INPUT_CHANGE;
}
