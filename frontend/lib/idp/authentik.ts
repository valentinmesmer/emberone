/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { OidcClientSettings } from '@/lib/idp/types';
import type { PublicRuntimeConfig } from '@/lib/runtimeConfig';

function parseAuthentikIssuerUrl(raw: string): URL | null {
  const trimmed = raw.trim();
  if (!trimmed) {
    return null;
  }
  const withSlash = trimmed.endsWith('/') ? trimmed : `${trimmed}/`;
  try {
    const u = new URL(withSlash);
    if (u.protocol !== 'http:' && u.protocol !== 'https:') {
      return null;
    }
    return u;
  } catch {
    return null;
  }
}

/** Authentik OIDC paths; avoids browser fetch of /.well-known/openid-configuration (no CORS). */
function buildAuthentikOidcMetadata(parsed: URL) {
  const issuer = parsed.href.endsWith('/') ? parsed.href : `${parsed.href}/`;
  const { origin } = parsed;
  const oauthBase = `${origin}/application/o/`;

  return {
    issuer,
    authorization_endpoint: `${oauthBase}authorize/`,
    token_endpoint: `${oauthBase}token/`,
    userinfo_endpoint: `${oauthBase}userinfo/`,
    end_session_endpoint: `${issuer}end-session/`,
    jwks_uri: `${issuer}jwks/`,
    code_challenge_methods_supported: ['S256', 'plain'],
  };
}

export function authentikOidcSettings(cfg: PublicRuntimeConfig): OidcClientSettings | null {
  const issuerRaw = cfg.NEXT_PUBLIC_AUTHENTIK_ISSUER || '';
  const clientId = cfg.NEXT_PUBLIC_AUTHENTIK_CLIENT_ID || '';
  const scope = cfg.NEXT_PUBLIC_AUTHENTIK_SCOPE || '';

  const parsedIssuer = parseAuthentikIssuerUrl(issuerRaw);
  if (!parsedIssuer || !clientId || !scope) {
    return null;
  }

  const authority = parsedIssuer.href.replace(/\/$/, '') || parsedIssuer.href;

  return {
    authority,
    metadata: buildAuthentikOidcMetadata(parsedIssuer),
    clientId,
    scope,
  };
}

/** URL for changing the login password in Authentik (requires Authentik session cookie). */
export function authentikPasswordSettingsUrl(issuerRaw: string | undefined): string | null {
  const trimmed = (issuerRaw ?? '').trim();
  if (!trimmed) {
    return null;
  }
  try {
    const issuer = new URL(trimmed.endsWith('/') ? trimmed : `${trimmed}/`);
    if (issuer.protocol !== 'http:' && issuer.protocol !== 'https:') {
      return null;
    }
    return `${issuer.origin}/if/user/#/settings;page-password`;
  } catch {
    return null;
  }
}
