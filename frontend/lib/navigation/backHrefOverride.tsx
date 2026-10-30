/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import {
  createContext,
  type ReactNode,
  useCallback,
  useContext,
  useLayoutEffect,
  useMemo,
  useState,
} from 'react';

interface BackHrefOverrideValue {
  href: string | null;
  setHref: (href: string | null) => void;
}

const BackHrefOverrideContext = createContext<BackHrefOverrideValue | null>(null);

export function BackHrefOverrideProvider({ children }: { children: ReactNode }) {
  const [href, setHrefState] = useState<string | null>(null);
  const setHref = useCallback((next: string | null) => {
    setHrefState(next);
  }, []);

  const value = useMemo(() => ({ href, setHref }), [href, setHref]);

  return (
    <BackHrefOverrideContext.Provider value={value}>{children}</BackHrefOverrideContext.Provider>
  );
}

/** When set (e.g. admin org drill-down), overrides {@link useParentPath} for {@link BackButton}. */
export function useBackHrefOverride(): string | null {
  return useContext(BackHrefOverrideContext)?.href ?? null;
}

export function useSetBackHref(): (href: string | null) => void {
  const ctx = useContext(BackHrefOverrideContext);
  return ctx?.setHref ?? (() => {});
}

/**
 * Registers the single “one level up” target for the shell {@link BackButton} on admin org detail routes.
 */
export function useRegisterAdminOrgBackHref(
  tenantId: number,
  org: { parentOrganizationId?: number | null } | null,
  active: boolean,
) {
  const setHref = useSetBackHref();

  useLayoutEffect(() => {
    if (!active || !Number.isFinite(tenantId) || tenantId <= 0) {
      setHref(null);
      return;
    }
    const tenantDetailPath = `/admin/tenants/${tenantId}`;
    if (!org) {
      setHref(tenantDetailPath);
      return;
    }
    const up =
      org.parentOrganizationId != null && org.parentOrganizationId > 0
        ? `/admin/tenants/${tenantId}/organizations/${org.parentOrganizationId}`
        : tenantDetailPath;
    setHref(up);
    return () => setHref(null);
  }, [active, tenantId, org, setHref]);
}
