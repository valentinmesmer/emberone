/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import { AuthProviderWrapper } from '@/components/auth/AuthProvider';

/**
 * Auth stays in the server render. A `next/dynamic` + `ssr: false` wrapper bails
 * the whole tree out to client rendering, so the first HTML is an empty document.
 * With the dark theme applied before paint that looks like a black page until a
 * later reload fills it in. OIDC itself is still imported only in the browser
 * (`ensureUserManager`).
 */
export function ClientAuthProvider({ children }: { children: ReactNode }) {
  return <AuthProviderWrapper>{children}</AuthProviderWrapper>;
}
