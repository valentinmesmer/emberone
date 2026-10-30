/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useTheme } from 'next-themes';
import { useEffect } from 'react';
import { useMounted } from '@/hooks/useMounted';

/** Fallbacks if `--chrome-bg` cannot be resolved - mirror `globals.css`. */
const CHROME_LIGHT_FALLBACK = '#fdfcfa';
const CHROME_DARK_FALLBACK = '#1e1f21';

/**
 * Resolve the effective chrome color from the stylesheet (`--chrome-bg` flips via
 * the `.dark` class), so theme colors live in exactly one place: `globals.css`.
 */
function chromeColor(resolvedTheme: string | undefined): string {
  const fallback = resolvedTheme === 'dark' ? CHROME_DARK_FALLBACK : CHROME_LIGHT_FALLBACK;
  const value = getComputedStyle(document.documentElement).getPropertyValue('--chrome-bg').trim();
  return value || fallback;
}

/**
 * Keeps `<meta name="theme-color">` in sync with the selected app theme.
 *
 * `layout.tsx` server-renders two media-scoped `theme-color` metas so the very
 * first paint matches the system color scheme. Once the user picks an explicit
 * theme that diverges from the system one, both metas are rewritten to the
 * chosen color - updating every meta rather than appending one keeps this
 * independent of document order (browsers honor the *first* matching meta).
 *
 * This covers Android Chrome, older iOS Safari and the installed PWA.
 *
 * iOS 26 Safari ignores `theme-color` entirely: it tints its bars from the
 * `background-color` of the top-most `position: fixed` / `sticky` element at
 * each viewport edge (`Header` and `MobileBottomNav` here), falling back to
 * `body`. Nothing to do from JS for that - those elements paint
 * `var(--chrome-bg)` and WebKit re-reads their color whenever it changes. What
 * does break it is a viewport-sized `fixed` element covering the edges, which
 * WebKit treats as a modal and refuses to retint from; see `useOverlayPresence`.
 *
 * `color-scheme` is handled by `globals.css` plus next-themes' own
 * `enableColorScheme`, so it is deliberately not touched here.
 */
export function ThemeChromeSync() {
  const { resolvedTheme } = useTheme();
  const mounted = useMounted();

  useEffect(() => {
    if (!mounted) return;

    // Read *after* next-themes applied the `.dark` class, so `--chrome-bg`
    // resolves to the theme that is actually active.
    const color = chromeColor(resolvedTheme);
    for (const meta of document.querySelectorAll<HTMLMetaElement>('meta[name="theme-color"]')) {
      meta.content = color;
    }
  }, [mounted, resolvedTheme]);

  return null;
}
