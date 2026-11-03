/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { useSelf } from '@/lib/store';

/**
 * Current user's display names from the backend (`/api/v1/self`), not from the JWT/OIDC profile.
 */
export function useUserInfo() {
  const { data: self } = useSelf();

  return {
    firstName: self?.firstName ?? null,
    lastName: self?.lastName ?? null,
  };
}
