/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/** Decode JWT payload (no signature verification; UI hints only). */
export function parseJwtPayload(accessToken: string): Record<string, unknown> | null {
  try {
    const parts = accessToken.split('.');
    if (parts.length < 2) {
      return null;
    }
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
    const json = atob(padded);
    return JSON.parse(json) as Record<string, unknown>;
  } catch {
    return null;
  }
}

/** Matches backend {@link com.emberone.platform.security.EmberOneJwtClaims#TENANT_ID}. */
export function readTenantIdFromToken(
  accessToken: string | null,
  profile: Record<string, unknown> | undefined,
): number | null {
  const fromProfile = profile?.tenantId;
  if (typeof fromProfile === 'number' && Number.isFinite(fromProfile)) {
    return fromProfile;
  }
  if (typeof fromProfile === 'string' && fromProfile.trim()) {
    const n = parseInt(fromProfile.trim(), 10);
    if (!Number.isNaN(n)) {
      return n;
    }
  }
  if (!accessToken) {
    return null;
  }
  const payload = parseJwtPayload(accessToken);
  const fromJwt = payload?.tenantId;
  if (typeof fromJwt === 'number' && Number.isFinite(fromJwt)) {
    return fromJwt;
  }
  if (typeof fromJwt === 'string' && fromJwt.trim()) {
    const n = parseInt(String(fromJwt).trim(), 10);
    if (!Number.isNaN(n)) {
      return n;
    }
  }
  return null;
}

export function readAppRoleFromToken(
  accessToken: string | null,
  profile: Record<string, unknown> | undefined,
): string | null {
  const fromProfile = profile?.app_role;
  if (typeof fromProfile === 'string' && fromProfile.trim()) {
    return fromProfile.trim();
  }
  if (!accessToken) {
    return null;
  }
  const payload = parseJwtPayload(accessToken);
  const fromJwt = payload?.app_role;
  if (typeof fromJwt === 'string' && fromJwt.trim()) {
    return fromJwt.trim();
  }
  return null;
}
