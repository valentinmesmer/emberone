/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export type OidcClientSettings = {
  authority: string;
  metadata: Record<string, unknown>;
  clientId: string;
  scope: string;
};
