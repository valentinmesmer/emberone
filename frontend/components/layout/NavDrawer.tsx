/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { RefreshCw, X } from 'lucide-react';
import { usePathname } from 'next/navigation';
import { Suspense, useEffect, useRef } from 'react';
import { NavigationMenu } from '@/components/navigation/NavigationMenu';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useAuth } from '@/hooks/useAuth';
import { useOverlayPresence } from '@/hooks/useOverlayPresence';
import { useTranslation } from '@/hooks/useTranslation';
import { useUserInfo } from '@/hooks/useUserInfo';
import { useWorkspaceNavMenu } from '@/hooks/useWorkspaceNavMenu';
import { useSelf, useUIStore } from '@/lib/store';
import { useRefreshServerCache } from '@/lib/store/useRefreshServerCache';
import { cn } from '@/lib/utils/cn';

/** Routes where the app chrome (drawer / bottom nav) must not appear. */
const CHROME_HIDDEN_PREFIXES = ['/auth', '/setup'];

/** Slide duration; keep in sync with `duration-200` on the sheet. */
const SHEET_TRANSITION_MS = 200;

function isChromeHidden(pathname: string): boolean {
  return CHROME_HIDDEN_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

function NavDrawerMenu({ onNavigate }: { onNavigate: () => void }) {
  const { middleItems, middleItemGroups, selectedId, myAreaBadge, orgBadge, orgBadges } =
    useWorkspaceNavMenu();

  return (
    <NavigationMenu
      onNavigate={onNavigate}
      middleItems={middleItems}
      middleItemGroups={middleItemGroups}
      selectedId={selectedId}
      myAreaBadge={myAreaBadge}
      orgBadge={orgBadge}
      orgBadges={orgBadges}
    />
  );
}

function NavDrawerContent({ onNavigate }: { onNavigate: () => void }) {
  const t = useTranslation();
  const { refresh, disabled } = useRefreshServerCache();
  const { firstName, lastName } = useUserInfo();
  const { data: self } = useSelf();
  const displayName = [firstName, lastName].filter(Boolean).join(' ').trim() || t('common.menu');

  return (
    <>
      {/* `sticky` + own background + the top safe-area padding *inside* the row:
          this is the box iOS 26 Safari samples for the top bar while the drawer
          is open. A full-width fixed/sticky element taller than 10px wins over
          the viewport-sized sheet, which WebKit refuses to retint from. Same
          contract as `Header`. */}
      <div className="sticky top-0 flex shrink-0 items-center gap-3 border-b border-[var(--border-color)] bg-[var(--card-bg)] px-4 pt-[calc(env(safe-area-inset-top,0px)+0.875rem)] pb-3.5">
        <UserAvatar
          firstName={firstName ?? undefined}
          lastName={lastName ?? undefined}
          size="lg"
          showProfileImage
        />
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-semibold text-[var(--text-primary)]">{displayName}</p>
          {self?.personalNumber != null ? (
            <p className="truncate text-xs text-[var(--text-secondary)]">
              {t('myArea.fields.personalNumber')}: {self.personalNumber}
            </p>
          ) : null}
        </div>
        <button
          type="button"
          onClick={onNavigate}
          className="inline-flex min-h-11 min-w-11 shrink-0 touch-manipulation items-center justify-center rounded-md p-2 text-[var(--text-secondary)] hover:bg-[var(--hover-bg)] hover:text-[var(--text-primary)] active:bg-[var(--hover-bg)]"
          aria-label={t('common.menuClose')}
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* No bottom padding: the menu's sticky footer has to reach the viewport
          edge to be sampled for the bottom bar (its own `pb` keeps the safe
          area clear). */}
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden px-3 pt-3">
        <div className="mb-2 shrink-0 border-b border-[var(--border-color)] pb-2">
          <button
            type="button"
            disabled={disabled}
            onClick={() => {
              onNavigate();
              void refresh();
            }}
            className="flex w-full min-h-11 items-center gap-2.5 rounded-md px-2.5 py-2 text-left text-sm font-medium text-[var(--text-primary)] transition-colors duration-150 hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)] disabled:cursor-not-allowed disabled:opacity-50 sm:gap-3 sm:px-3"
          >
            <RefreshCw className="h-4 w-4 shrink-0 text-[var(--text-secondary)]" aria-hidden />
            <span className="min-w-0 flex-1">{t('common.pullToRefresh.action')}</span>
          </button>
        </div>
        <Suspense fallback={<NavigationMenu onNavigate={onNavigate} />}>
          <NavDrawerMenu onNavigate={onNavigate} />
        </Suspense>
      </div>
    </>
  );
}

/**
 * Mobile navigation menu, shown as a full-screen sheet. Lives once globally
 * (rendered by AuthLayout) and is controlled via the UI store so both the header
 * hamburger and the bottom-nav menu tab can open it. Desktop (`lg+`) uses the
 * persistent sidebar instead.
 *
 * Full-screen on purpose (no partial drawer + dim scrim): iOS 26 Safari tints its
 * top/bottom bars from the page itself, so a half-dimmed layout always produced a
 * visible seam against the bars. With the sheet covering everything in
 * `--card-bg` (== `--chrome-bg`), the bars blend with the menu in both themes.
 *
 * The sheet's own top row and the menu's footer are `sticky` so that *they* are
 * what Safari samples at each edge while the drawer is open - WebKit will not
 * retint from a viewport-sized container, which the sheet itself is.
 */
export function NavDrawer() {
  const t = useTranslation();
  const pathname = usePathname();
  const { isAuthenticated } = useAuth();
  const open = useUIStore((s) => s.navDrawerOpen);
  const setOpen = useUIStore((s) => s.setNavDrawerOpen);
  const { present, entered } = useOverlayPresence(open, SHEET_TRANSITION_MS);
  const rootRef = useRef<HTMLDivElement>(null);

  const close = () => setOpen(false);

  // Close on route change so the drawer never lingers after navigating.
  useEffect(() => {
    setOpen(false);
  }, [setOpen]);

  // Esc-to-close + body scroll lock while open.
  useEffect(() => {
    if (!open) return;
    document.body.style.overflow = 'hidden';
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') setOpen(false);
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = '';
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [open, setOpen]);

  // Blur focused descendants before the closed overlay becomes non-interactive,
  // otherwise Chrome warns about focus retained under aria-hidden/inert.
  useEffect(() => {
    if (open) return;
    const active = document.activeElement;
    if (active instanceof HTMLElement && rootRef.current?.contains(active)) {
      active.blur();
    }
  }, [open]);

  if (!isAuthenticated || isChromeHidden(pathname)) {
    return null;
  }

  return (
    <div
      ref={rootRef}
      className={cn(
        'pointer-events-none fixed inset-0 z-[60] xl:hidden',
        // Leave the render tree once the slide-out finished. A mounted
        // viewport-sized `fixed` host sits on top of `Header` /
        // `MobileBottomNav` at both viewport edges, and iOS 26 Safari then
        // keeps its previous bar tint instead of following the theme (see the
        // component doc above `useOverlayPresence`).
        !present && 'hidden',
        open && 'pointer-events-auto',
      )}
      // `inert` (not aria-hidden): hides from a11y and blocks focus while closed.
      inert={!open ? true : undefined}
    >
      {/* Full-screen sheet */}
      <div
        role="dialog"
        aria-modal={open}
        aria-label={open ? t('common.menu') : undefined}
        className={cn(
          'absolute inset-0 flex h-dvh w-full flex-col bg-[var(--card-bg)]',
          'transition-transform duration-200 ease-out',
          entered ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        {present && <NavDrawerContent onNavigate={close} />}
      </div>
    </div>
  );
}
