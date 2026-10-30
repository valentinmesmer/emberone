/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Building2, Home, Menu as MenuIcon, User } from 'lucide-react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useMemo } from 'react';
import { useAuth } from '@/hooks/useAuth';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useTranslation } from '@/hooks/useTranslation';
import { useWorkspaceOrganizations } from '@/hooks/useWorkspaceOrganizations';
import { MY_AREA_BASE_PATH } from '@/lib/my-area/myAreaSections';
import { useOpenTodoCount, useUIStore } from '@/lib/store';
import { getOrganizationWorkspaceEntryPath } from '@/lib/utils';
import { cn } from '@/lib/utils/cn';

/** Routes where the app chrome (drawer / bottom nav) must not appear. */
const CHROME_HIDDEN_PREFIXES = ['/auth', '/setup'];

function isChromeHidden(pathname: string): boolean {
  return CHROME_HIDDEN_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

const tabBase =
  'flex flex-1 flex-col items-center justify-center gap-0.5 rounded-none px-1 pt-1.5 text-[11px] font-medium touch-manipulation transition-[color,opacity] duration-200 ease-out [-webkit-tap-highlight-color:transparent] active:bg-transparent';

function TabIcon({ active, children }: { active: boolean; children: React.ReactNode }) {
  return (
    <span
      className={cn(
        'flex h-6 w-6 items-center justify-center transition-colors duration-200 ease-out [&>svg]:h-[22px] [&>svg]:w-[22px]',
        active ? 'text-[var(--fire-red)]' : 'text-[var(--text-secondary)]',
      )}
    >
      {children}
    </span>
  );
}

/**
 * Mobile-first bottom tab bar. Rendered once globally (AuthLayout) and hidden on
 * desktop (`xl:hidden`), where the sidebar takes over. Provides one-tap access to
 * the primary hubs; the bottom-nav menu tab opens the full navigation drawer.
 */
export function MobileBottomNav() {
  const t = useTranslation();
  const pathname = usePathname();
  const router = useRouter();
  const { isAuthenticated } = useAuth();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const { organizations: workspaceOrganizations } = useWorkspaceOrganizations();
  const navDrawerOpen = useUIStore((s) => s.navDrawerOpen);
  const toggleNavDrawer = useUIStore((s) => s.toggleNavDrawer);
  const preferredOrganizationId = useUIStore((s) => s.selectedOrganizationId);
  const { data: openTodoCount = 0 } = useOpenTodoCount({ enabled: hasEmberOneUserId });

  const mainOrgId = useMemo(() => {
    const memberships = workspaceOrganizations.filter((org) => org.source === 'membership');
    const main = memberships.find((org) => org.main) ?? memberships[0];
    return main?.id ?? null;
  }, [workspaceOrganizations]);

  const organizationTargetId = useMemo(() => {
    const workspaceOrgIds = new Set(workspaceOrganizations.map((org) => org.id));
    if (preferredOrganizationId != null && workspaceOrgIds.has(preferredOrganizationId)) {
      return preferredOrganizationId;
    }
    return mainOrgId;
  }, [workspaceOrganizations, preferredOrganizationId, mainOrgId]);

  if (!isAuthenticated || isChromeHidden(pathname)) {
    return null;
  }

  const isStartActive = pathname === '/';
  const isMyAreaActive =
    pathname === MY_AREA_BASE_PATH || pathname.startsWith(`${MY_AREA_BASE_PATH}/`);
  const isOrgActive = pathname.startsWith('/organizations');

  const inactive = 'text-[var(--text-secondary)] active:transition-none';
  const active = 'text-[var(--fire-red)]';

  const goToOrganization = () => {
    if (organizationTargetId == null) return;
    router.push(getOrganizationWorkspaceEntryPath(organizationTargetId));
  };

  return (
    <nav
      className={cn(
        'fixed inset-x-0 bottom-0 z-30 shadow-none xl:hidden',
        // The frosted background + top border live on the <nav> and the safe-area
        // padding sits *inside* it, so the bar extends down through the iOS
        // home-indicator area and stays grounded to the bottom edge - instead of
        // floating above a transparent strip. Full-width, no radius.
        'border-t border-[var(--border-color)]/35 pb-[env(safe-area-inset-bottom,0px)]',
        'bg-[var(--chrome-bg)]',
      )}
      aria-label={t('common.menu')}
    >
      <div className="flex h-16 w-full items-stretch">
        {/* Start */}
        <Link
          href="/"
          className={cn(tabBase, isStartActive ? active : inactive)}
          aria-current={isStartActive ? 'page' : undefined}
        >
          <TabIcon active={isStartActive}>
            <Home />
          </TabIcon>
          <span className="truncate">{t('home.navigation.bottomNav.start')}</span>
        </Link>

        {/* Mein Bereich */}
        {hasEmberOneUserId ? (
          <Link
            href={MY_AREA_BASE_PATH}
            className={cn(tabBase, isMyAreaActive ? active : inactive)}
            aria-current={isMyAreaActive ? 'page' : undefined}
          >
            <TabIcon active={isMyAreaActive}>
              <span className="relative">
                <User />
                {openTodoCount > 0 ? (
                  <span
                    className="absolute -right-1.5 -top-1.5 flex h-3.5 min-w-3.5 items-center justify-center rounded-full bg-[var(--fire-red)] px-0.5 text-[9px] font-semibold leading-none text-white"
                    aria-hidden
                  >
                    {openTodoCount > 99 ? '99+' : openTodoCount}
                  </span>
                ) : null}
              </span>
            </TabIcon>
            <span className="truncate">{t('home.navigation.bottomNav.myArea')}</span>
          </Link>
        ) : (
          <button
            type="button"
            disabled
            className={cn(tabBase, 'cursor-not-allowed text-[var(--text-secondary)]/45')}
            title={t('myArea.error.noEmployeeId')}
          >
            <TabIcon active={false}>
              <User />
            </TabIcon>
            <span className="truncate">{t('home.navigation.bottomNav.myArea')}</span>
          </button>
        )}

        {/* Organisation */}
        <button
          type="button"
          onClick={goToOrganization}
          disabled={organizationTargetId == null}
          className={cn(
            tabBase,
            organizationTargetId == null
              ? 'cursor-not-allowed text-[var(--text-secondary)]/45'
              : isOrgActive
                ? active
                : inactive,
          )}
          aria-current={isOrgActive ? 'page' : undefined}
        >
          <TabIcon active={isOrgActive && organizationTargetId != null}>
            <Building2 />
          </TabIcon>
          <span className="truncate">{t('home.navigation.bottomNav.organization')}</span>
        </button>

        {/* Menu */}
        <button
          type="button"
          onClick={toggleNavDrawer}
          className={cn(tabBase, navDrawerOpen ? active : inactive)}
          aria-haspopup="dialog"
          aria-expanded={navDrawerOpen}
          aria-label={t('common.menuOpen')}
        >
          <TabIcon active={navDrawerOpen}>
            <MenuIcon />
          </TabIcon>
          <span className="truncate">{t('common.menu')}</span>
        </button>
      </div>
    </nav>
  );
}
