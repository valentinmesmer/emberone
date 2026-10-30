/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { authentikOidcSettings, authentikPasswordSettingsUrl } from '@/lib/idp/authentik';
import type { OidcClientSettings } from '@/lib/idp/types';
import type { PublicRuntimeConfig } from '@/lib/runtimeConfig';

export function resolveOidcClientSettings(cfg: PublicRuntimeConfig): OidcClientSettings | null {
  return authentikOidcSettings(cfg);
}

export function resolvePasswordSettingsUrl(cfg: PublicRuntimeConfig): string | null {
  return authentikPasswordSettingsUrl(cfg.NEXT_PUBLIC_AUTHENTIK_ISSUER);
}
