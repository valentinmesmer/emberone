/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { ensureUserManager } from '@/lib/auth/oidcConfig';

/**
 * Loaded inside a hidden iframe by oidc-client-ts during automatic silent token
 * renewal. Calls signinSilentCallback() which posts the new token back to the
 * parent frame - no navigation, no visible UI.
 */
export default function SilentCallbackPage() {
  useEffect(() => {
    ensureUserManager()
      .then((manager) => manager?.signinSilentCallback())
      .catch(() => {
        // Expected when the Authentik session has expired; the parent handles the renewal
        // failure and redirects the user to login if needed.
      });
  }, []);

  return null;
}
