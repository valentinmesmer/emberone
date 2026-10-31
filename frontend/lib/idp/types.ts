/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

export type OidcClientSettings = {
  authority: string;
  metadata: Record<string, unknown>;
  clientId: string;
  scope: string;
};
