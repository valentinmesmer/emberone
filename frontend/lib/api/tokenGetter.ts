/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

let tokenGetter: (() => Promise<string | null>) | null = null;

/** Wired from `AuthProvider` so `ApiClient` can attach Bearer tokens without importing React modules. */
export function setTokenGetter(getter: () => Promise<string | null>) {
  tokenGetter = getter;
}

export function getTokenGetter(): (() => Promise<string | null>) | null {
  return tokenGetter;
}
