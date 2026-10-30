/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { X } from 'lucide-react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useCallback, useEffect, useRef } from 'react';
import { IconButton } from '@/components/ui/icon-button';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useNotificationPanelAnchor } from '@/hooks/useNotificationPanelAnchor';
import { useOverlayPresence } from '@/hooks/useOverlayPresence';
import { useTranslation } from '@/hooks/useTranslation';
import {
  getMyAreaNotificationDetailPath,
  getMyAreaNotificationsPath,
} from '@/lib/my-area/myAreaSections';
import { useMarkNotificationRead, useUIStore } from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import type { NotificationDto } from '@/types/notification';
import { NotificationList } from './NotificationList';
import { NotificationMarkAllReadButton } from './NotificationMarkAllReadButton';

const CHROME_HIDDEN_PREFIXES = ['/auth', '/setup'];

/** Slide/fade duration; keep in sync with `duration-200` on the panel. */
const PANEL_TRANSITION_MS = 200;

function isChromeHidden(pathname: string): boolean {
  return CHROME_HIDDEN_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

export function NotificationPanel() {
  const t = useTranslation();
  const router = useRouter();
  const pathname = usePathname();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const open = useUIStore((s) => s.notificationPanelOpen);
  const setOpen = useUIStore((s) => s.setNotificationPanelOpen);
  const rootRef = useRef<HTMLDivElement>(null);
  const panelRef = useRef<HTMLDivElement>(null);
  const markRead = useMarkNotificationRead();
  const { anchor: dropdownAnchor, isDropdown } = useNotificationPanelAnchor(open);
  const { present, entered } = useOverlayPresence(open, PANEL_TRANSITION_MS);

  const close = useCallback(() => setOpen(false), [setOpen]);

  useEffect(() => {
    setOpen(false);
  }, [setOpen]);

  useEffect(() => {
    if (!open) {
      return;
    }
    if (!isDropdown) {
      document.body.style.overflow = 'hidden';
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        close();
      }
    };
    document.addEventListener('keydown', onKeyDown);
    return () => {
      document.body.style.overflow = '';
      document.removeEventListener('keydown', onKeyDown);
    };
  }, [close, open, isDropdown]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const onPointerDown = (event: MouseEvent | TouchEvent) => {
      const target = event.target as Node | null;
      if (!target || panelRef.current?.contains(target)) {
        return;
      }
      const bellButton = (target as HTMLElement).closest?.('[data-notification-bell]');
      if (bellButton) {
        return;
      }
      close();
    };
    document.addEventListener('mousedown', onPointerDown);
    document.addEventListener('touchstart', onPointerDown);
    return () => {
      document.removeEventListener('mousedown', onPointerDown);
      document.removeEventListener('touchstart', onPointerDown);
    };
  }, [close, open]);

  // Blur focused descendants when closing so focus is not retained under inert.
  useEffect(() => {
    if (open) return;
    const active = document.activeElement;
    if (active instanceof HTMLElement && rootRef.current?.contains(active)) {
      active.blur();
    }
  }, [open]);

  const handleMarkRead = useCallback(
    (notification: NotificationDto) => {
      if (notification.unread) {
        markRead.mutate(notification.id);
      }
    },
    [markRead],
  );

  const handleSelect = useCallback(
    (notification: NotificationDto) => {
      handleMarkRead(notification);
      close();
      router.push(getMyAreaNotificationDetailPath(notification.id));
    },
    [close, handleMarkRead, router],
  );

  if (!hasEmberOneUserId || isChromeHidden(pathname)) {
    return null;
  }

  return (
    <div
      ref={rootRef}
      className={cn(
        'pointer-events-none fixed inset-0 z-[55]',
        // Leave the render tree once the panel finished closing - see
        // `useOverlayPresence`: a mounted viewport-sized `fixed` host freezes
        // the iOS 26 Safari bar tint at its page-load color.
        !present && 'hidden',
        open && 'pointer-events-auto',
      )}
      inert={!open ? true : undefined}
    >
      {open && isDropdown ? (
        <button
          type="button"
          className="absolute inset-0 bg-black/20"
          onClick={close}
          aria-label={t('common.close')}
        />
      ) : null}
      {/* Mobile-first: full-height sheet below md; md+ dropdown beside the bell */}
      <div
        ref={panelRef}
        role="dialog"
        aria-modal={open}
        aria-label={open ? t('notifications.title') : undefined}
        style={
          isDropdown && dropdownAnchor
            ? {
                top: dropdownAnchor.top,
                left: dropdownAnchor.left,
                width: dropdownAnchor.width,
                // Need a definite height: maxHeight alone lets flex-1 min-h-0 collapse the list to 0.
                height: dropdownAnchor.maxHeight,
                maxHeight: dropdownAnchor.maxHeight,
              }
            : undefined
        }
        className={cn(
          'absolute flex flex-col overflow-hidden bg-[var(--card-bg)] shadow-xl',
          !isDropdown &&
            'fixed inset-y-0 right-0 w-full max-w-none border-l border-[var(--border-color)] pt-[env(safe-area-inset-top,0px)] pb-[env(safe-area-inset-bottom,0px)]',
          'transition-[transform,opacity] duration-200 ease-out',
          isDropdown && 'fixed rounded-xl border border-[var(--border-color)]',
          isDropdown && !dropdownAnchor && 'invisible',
          entered
            ? isDropdown
              ? 'opacity-100'
              : 'translate-x-0'
            : isDropdown
              ? 'pointer-events-none opacity-0'
              : 'translate-x-full pointer-events-none',
        )}
      >
        <div className="flex shrink-0 items-center justify-between gap-3 border-b border-[var(--border-color)] px-4 py-3">
          <h2 className="min-w-0 truncate text-base font-semibold tracking-tight text-[var(--text-primary)]">
            {t('notifications.title')}
          </h2>
          <div className="flex shrink-0 items-center gap-0.5">
            <NotificationMarkAllReadButton enabled={open} iconOnly />
            <IconButton
              icon={<X />}
              variant="ghost"
              size="md"
              onClick={close}
              aria-label={t('common.close')}
              className="min-h-10 min-w-10 touch-manipulation"
            />
          </div>
        </div>

        <div className="min-h-0 flex-1 overflow-y-auto overscroll-contain">
          <NotificationList onSelect={handleSelect} onOpenLink={handleMarkRead} />
        </div>

        <div className="shrink-0 border-t border-[var(--border-color)] px-4 py-3">
          <Link
            href={getMyAreaNotificationsPath()}
            onClick={close}
            className="block text-center text-sm font-medium text-[var(--fire-red)] underline-offset-2 hover:underline"
          >
            {t('notifications.viewAll')}
          </Link>
        </div>
      </div>
    </div>
  );
}
