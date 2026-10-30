/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Filled success CTA (e.g. appointment response “yes”, workflow approve). */
export const statusSuccessSolidButtonClass =
  'border-[var(--status-success-solid)] bg-[var(--status-success-solid)] hover:bg-[var(--status-success-solid-hover)] text-white';

/** Filled danger CTA with matching border (toggle / paired action rows). */
export const statusDangerSolidButtonClass =
  'border-[var(--status-danger-solid)] bg-[var(--status-danger-solid)] hover:bg-[var(--status-danger-solid-hover)] text-white';

/** Filled danger CTA for modals (no extra border). */
export const statusDangerSolidCtaClass =
  'bg-[var(--status-danger-solid)] hover:bg-[var(--status-danger-solid-hover)] text-white';
