/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Download, Share, X } from 'lucide-react';
import { usePathname } from 'next/navigation';
import { startTransition, useEffect, useRef, useState } from 'react';
import { Button } from '@/components/ui/button';
import { IconButton } from '@/components/ui/icon-button';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import {
  isIosSafari,
  isMobileViewport,
  isPwaDisplayMode,
  markPwaInstalled,
  PWA_INSTALL_SHOW_DELAY_MS,
  recordPwaInstallVisit,
  shouldOfferPwaInstall,
  snoozePwaInstallPrompt,
} from '@/lib/pwa/installPrompt';
import { cn } from '@/lib/utils/cn';

const CHROME_HIDDEN_PREFIXES = ['/auth', '/setup', '/~offline'] as const;

type BeforeInstallPromptEvent = Event & {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
};

function isChromeHidden(pathname: string): boolean {
  return CHROME_HIDDEN_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

/**
 * Soft “install app” hint for mobile browsers.
 * Shows only after a few visit days, with a long snooze - never on first paint.
 * Once shown, stays until the user dismisses or installs (navigation does not hide it).
 */
export function PwaInstallPrompt() {
  const t = useTranslation();
  const pathname = usePathname();
  const { isAuthenticated } = useAuth();
  const deferredPromptRef = useRef<BeforeInstallPromptEvent | null>(null);
  const delayElapsedRef = useRef(false);
  const dismissedRef = useRef(false);
  const [visible, setVisible] = useState(false);
  const [mode, setMode] = useState<'chromium' | 'ios' | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      deferredPromptRef.current = null;
      delayElapsedRef.current = false;
      dismissedRef.current = false;
      startTransition(() => {
        setVisible(false);
        setMode(null);
      });
      return;
    }

    if (dismissedRef.current) return;
    if (isPwaDisplayMode()) {
      markPwaInstalled();
      return;
    }
    if (!isMobileViewport()) return;

    const prefs = recordPwaInstallVisit();
    if (!shouldOfferPwaInstall(prefs)) return;

    const ios = isIosSafari();
    if (ios) {
      startTransition(() => setMode('ios'));
    }

    const tryShow = () => {
      if (dismissedRef.current) return;
      if (!delayElapsedRef.current) return;
      if (isPwaDisplayMode() || !isMobileViewport()) return;
      if (ios || deferredPromptRef.current) setVisible(true);
    };

    const onBeforeInstall = (event: Event) => {
      event.preventDefault();
      deferredPromptRef.current = event as BeforeInstallPromptEvent;
      setMode('chromium');
      tryShow();
    };

    window.addEventListener('beforeinstallprompt', onBeforeInstall);

    const delayId = window.setTimeout(() => {
      delayElapsedRef.current = true;
      tryShow();
    }, PWA_INSTALL_SHOW_DELAY_MS);

    return () => {
      window.clearTimeout(delayId);
      window.removeEventListener('beforeinstallprompt', onBeforeInstall);
    };
  }, [isAuthenticated]);

  const dismiss = () => {
    dismissedRef.current = true;
    snoozePwaInstallPrompt();
    setVisible(false);
  };

  const install = async () => {
    const promptEvent = deferredPromptRef.current;
    if (!promptEvent) return;
    try {
      await promptEvent.prompt();
      const choice = await promptEvent.userChoice;
      deferredPromptRef.current = null;
      dismissedRef.current = true;
      if (choice.outcome === 'accepted') {
        markPwaInstalled();
      } else {
        snoozePwaInstallPrompt();
      }
      setVisible(false);
    } catch {
      dismissedRef.current = true;
      snoozePwaInstallPrompt();
      setVisible(false);
    }
  };

  // Hide on auth/setup chrome without clearing session state - returns when leaving those routes.
  if (!visible || !mode || isChromeHidden(pathname)) return null;

  const Icon = mode === 'ios' ? Share : Download;

  return (
    <aside
      role="status"
      aria-live="polite"
      className={cn(
        'fixed inset-x-3 z-40 flex items-center gap-3 xl:hidden',
        // Sit just above the mobile bottom chrome (tabs + SourceFooter strip).
        'bottom-[var(--app-mobile-bottom-safe)]',
        'rounded-2xl border border-[var(--border-color)] bg-[var(--card-bg)] px-3 py-2.5 shadow-[var(--interactive-shadow-strong)]',
      )}
    >
      <div
        className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-[var(--surface-muted)] text-[var(--text-secondary)]"
        aria-hidden
      >
        <Icon className="h-4 w-4" />
      </div>

      <div className="min-w-0 flex-1">
        <p className="truncate text-sm font-medium text-[var(--text-primary)]">
          {t('pwa.install.title')}
        </p>
        <p className="truncate text-xs text-[var(--text-secondary)]">
          {mode === 'ios' ? t('pwa.install.iosSteps') : t('pwa.install.description')}
        </p>
      </div>

      {mode === 'chromium' ? (
        <Button type="button" variant="primary" size="sm" onClick={() => void install()}>
          {t('pwa.install.action')}
        </Button>
      ) : null}

      <IconButton
        type="button"
        variant="ghost"
        size="sm"
        aria-label={t('pwa.install.dismiss')}
        icon={<X className="h-4 w-4" />}
        onClick={dismiss}
      />
    </aside>
  );
}
