/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Bell } from 'lucide-react';
import { usePathname } from 'next/navigation';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useTranslation } from '@/hooks/useTranslation';
import { useUIStore, useUnreadNotificationCount } from '@/lib/store';
import { cn } from '@/lib/utils/cn';

const CHROME_HIDDEN_PREFIXES = ['/auth', '/setup'];

function isChromeHidden(pathname: string): boolean {
  return CHROME_HIDDEN_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

export function NotificationBellButton() {
  const t = useTranslation();
  const pathname = usePathname();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const open = useUIStore((s) => s.notificationPanelOpen);
  const togglePanel = useUIStore((s) => s.toggleNotificationPanel);
  const setNavDrawerOpen = useUIStore((s) => s.setNavDrawerOpen);
  const { data } = useUnreadNotificationCount();

  if (!hasEmberOneUserId || isChromeHidden(pathname)) {
    return null;
  }

  const unreadCount = data?.count ?? 0;
  const badgeLabel =
    unreadCount > 0 ? t('notifications.bell.unread', { count: unreadCount }) : undefined;

  const handleClick = () => {
    if (!open) {
      setNavDrawerOpen(false);
    }
    togglePanel();
  };

  return (
    <button
      type="button"
      data-notification-bell
      onClick={handleClick}
      className={cn(
        'relative inline-flex min-h-11 min-w-11 touch-manipulation items-center justify-center rounded-lg',
        'text-[var(--text-primary)] hover:bg-[var(--hover-bg)] active:bg-[var(--hover-bg)]',
        'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)]',
        'focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--focus-ring-offset)]',
      )}
      aria-label={badgeLabel ?? t('notifications.bell.label')}
      aria-haspopup="dialog"
      aria-expanded={open}
    >
      <span className="relative">
        <Bell className="h-5 w-5" />
        {unreadCount > 0 ? (
          <span
            className="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-[var(--fire-red)] px-1 text-[10px] font-semibold leading-none text-white"
            aria-hidden
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        ) : null}
      </span>
    </button>
  );
}
