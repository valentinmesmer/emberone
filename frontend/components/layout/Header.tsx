/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Menu as MenuIcon } from 'lucide-react';
import { usePathname, useRouter } from 'next/navigation';
import { Brand } from '@/components/layout/Brand';
import { NotificationBellButton } from '@/components/notification/NotificationBellButton';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { useUIStore } from '@/lib/store';

export function Header() {
  const { isAuthenticated } = useAuth();
  const pathname = usePathname();
  const router = useRouter();
  const t = useTranslation();
  const toggleNavDrawer = useUIStore((s) => s.toggleNavDrawer);
  const navDrawerOpen = useUIStore((s) => s.navDrawerOpen);
  /** Hamburger toggles the global mobile drawer; hidden on auth/setup and when logged out. */
  const showMenuButton =
    isAuthenticated && !pathname.startsWith('/auth') && !pathname.startsWith('/setup');
  /** Persistent sidebar on `xl+` replaces the top header for authenticated app routes. */
  const hideOnDesktopSidebar = showMenuButton;

  const handleLogoClick = () => {
    router.push('/');
  };

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 border-b border-[var(--border-color)] bg-[var(--chrome-bg)] pt-[env(safe-area-inset-top,0px)] shadow-sm${hideOnDesktopSidebar ? ' xl:hidden' : ''}`}
    >
      <div className="w-full max-w-none px-3 sm:px-4 md:px-4 lg:px-6">
        <div className="flex h-12 sm:h-14 items-center justify-between gap-3">
          {/* Left: mobile menu trigger + app branding (admin / theme / logout live in the nav menu). */}
          <div className="flex min-w-0 flex-1 items-center gap-1.5 sm:gap-2">
            {showMenuButton && (
              <button
                type="button"
                onClick={toggleNavDrawer}
                className="flex items-center justify-center w-9 h-9 -ml-1.5 rounded-lg hover:bg-[var(--hover-bg)] transition-colors xl:hidden focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]"
                aria-label={t('common.menuOpen')}
                aria-haspopup="dialog"
                aria-expanded={navDrawerOpen}
              >
                <MenuIcon className="h-5 w-5 text-[var(--text-primary)]" />
              </button>
            )}
            <Brand variant="compact" showSubtitle={false} onClick={handleLogoClick} />
          </div>
          <div className="flex shrink-0 items-center">
            <NotificationBellButton />
          </div>
        </div>
      </div>
    </header>
  );
}
