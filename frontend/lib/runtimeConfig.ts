/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Public config for the browser. Docker: `public/runtime-config.json` at container start.
 * Local: `.env` - use literal `process.env.NEXT_PUBLIC_*` names so Next/Turbopack inlines them.
 */
export type PublicRuntimeConfig = {
  NEXT_PUBLIC_API_BASE_URL?: string;
  NEXT_PUBLIC_AUTHENTIK_ISSUER?: string;
  NEXT_PUBLIC_AUTHENTIK_CLIENT_ID?: string;
  NEXT_PUBLIC_AUTHENTIK_SCOPE?: string;
  NEXT_PUBLIC_GEOCODER_URL?: string;
  NEXT_PUBLIC_REALTIME_ENABLED?: string;
};

const RUNTIME_CONFIG_PATH = '/runtime-config.json';

const KEYS: (keyof PublicRuntimeConfig)[] = [
  'NEXT_PUBLIC_API_BASE_URL',
  'NEXT_PUBLIC_AUTHENTIK_ISSUER',
  'NEXT_PUBLIC_AUTHENTIK_CLIENT_ID',
  'NEXT_PUBLIC_AUTHENTIK_SCOPE',
  'NEXT_PUBLIC_GEOCODER_URL',
  'NEXT_PUBLIC_REALTIME_ENABLED',
];

/** Same payload as `public/runtime-config.json` (Docker) and the dev route handler. */
export function publicRuntimeConfigFromEnv(): PublicRuntimeConfig {
  const o: PublicRuntimeConfig = {};
  const api = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (api) o.NEXT_PUBLIC_API_BASE_URL = api;
  const issuer = process.env.NEXT_PUBLIC_AUTHENTIK_ISSUER;
  if (issuer) o.NEXT_PUBLIC_AUTHENTIK_ISSUER = issuer;
  const clientId = process.env.NEXT_PUBLIC_AUTHENTIK_CLIENT_ID;
  if (clientId) o.NEXT_PUBLIC_AUTHENTIK_CLIENT_ID = clientId;
  const scope = process.env.NEXT_PUBLIC_AUTHENTIK_SCOPE;
  if (scope) o.NEXT_PUBLIC_AUTHENTIK_SCOPE = scope;
  const geocoder = process.env.NEXT_PUBLIC_GEOCODER_URL;
  if (geocoder) o.NEXT_PUBLIC_GEOCODER_URL = geocoder;
  const realtime = process.env.NEXT_PUBLIC_REALTIME_ENABLED;
  if (realtime) o.NEXT_PUBLIC_REALTIME_ENABLED = realtime;
  return o;
}

let clientCache: PublicRuntimeConfig | null = null;
let clientLoadPromise: Promise<PublicRuntimeConfig> | null = null;

async function loadClientRuntimeConfig(): Promise<PublicRuntimeConfig> {
  const envFallback = publicRuntimeConfigFromEnv();
  try {
    const res = await fetch(RUNTIME_CONFIG_PATH, { cache: 'no-store' });
    if (!res.ok) return envFallback;
    const json = (await res.json()) as Record<string, unknown>;
    const merged: PublicRuntimeConfig = { ...envFallback };
    for (const k of KEYS) {
      const v = json[k];
      if (typeof v === 'string' && v.length > 0) merged[k] = v;
    }
    return merged;
  } catch {
    return envFallback;
  }
}

export function getRuntimeConfig(): Promise<PublicRuntimeConfig> {
  if (typeof window === 'undefined') {
    return Promise.resolve(publicRuntimeConfigFromEnv());
  }
  if (clientCache) return Promise.resolve(clientCache);
  if (!clientLoadPromise) {
    clientLoadPromise = loadClientRuntimeConfig().then((c) => {
      clientCache = c;
      return c;
    });
  }
  return clientLoadPromise;
}
