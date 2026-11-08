/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/**
 * Deep link that opens a list page's create dialog (e.g. from the help chat). The page still checks
 * its own write permission; the parameter only saves the click on the create button.
 */
export const CREATE_INTENT_PARAM = 'create';

export function withCreateIntent(href: string): string {
  return `${href}?${CREATE_INTENT_PARAM}=1`;
}
