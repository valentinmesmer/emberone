/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Soft-prompt preferences for installing EmberOne as a PWA.
 * Designed to stay out of the way: rare, dismissible, long cooldown.
 */

const STORAGE_KEY = 'emberone.pwa.install';

/** Soft dismiss (“Später”) - don’t ask again for a while. */
export const PWA_INSTALL_SNOOZE_MS = 30 * 24 * 60 * 60 * 1000; // 1 month

/** Require this many distinct calendar days of use before prompting. */
export const PWA_INSTALL_MIN_VISIT_DAYS = 2;

/** Wait after mount so the prompt never greets the user on first paint. */
export const PWA_INSTALL_SHOW_DELAY_MS = 12_000;

export type PwaInstallPrefs = {
  /** Unix ms - hide prompt until this time. */
  snoozedUntil: number;
  /** Distinct YYYY-MM-DD visit days (capped). */
  visitDays: string[];
  /** User completed install or marked as installed. */
  installed: boolean;
};

const EMPTY_PREFS: PwaInstallPrefs = {
  snoozedUntil: 0,
  visitDays: [],
  installed: false,
};

function todayKey(now = new Date()): string {
  return now.toISOString().slice(0, 10);
}

export function readPwaInstallPrefs(): PwaInstallPrefs {
  if (typeof window === 'undefined') return { ...EMPTY_PREFS };
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (!raw) return { ...EMPTY_PREFS };
    const parsed = JSON.parse(raw) as Partial<PwaInstallPrefs>;
    return {
      snoozedUntil: typeof parsed.snoozedUntil === 'number' ? parsed.snoozedUntil : 0,
      visitDays: Array.isArray(parsed.visitDays)
        ? parsed.visitDays.filter((d): d is string => typeof d === 'string').slice(-14)
        : [],
      installed: parsed.installed === true,
    };
  } catch {
    return { ...EMPTY_PREFS };
  }
}

export function writePwaInstallPrefs(prefs: PwaInstallPrefs): void {
  if (typeof window === 'undefined') return;
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(prefs));
  } catch {
    // Quota / private mode - ignore; prompt may reappear, but never crash.
  }
}

/** Record today’s visit once and return the updated prefs. */
export function recordPwaInstallVisit(now = Date.now()): PwaInstallPrefs {
  const prefs = readPwaInstallPrefs();
  const day = todayKey(new Date(now));
  if (!prefs.visitDays.includes(day)) {
    prefs.visitDays = [...prefs.visitDays, day].slice(-14);
    writePwaInstallPrefs(prefs);
  }
  return prefs;
}

export function snoozePwaInstallPrompt(ms = PWA_INSTALL_SNOOZE_MS, now = Date.now()): void {
  const prefs = readPwaInstallPrefs();
  prefs.snoozedUntil = now + ms;
  writePwaInstallPrefs(prefs);
}

export function markPwaInstalled(): void {
  const prefs = readPwaInstallPrefs();
  prefs.installed = true;
  writePwaInstallPrefs(prefs);
}

export function shouldOfferPwaInstall(prefs: PwaInstallPrefs, now = Date.now()): boolean {
  if (prefs.installed) return false;
  if (prefs.snoozedUntil > now) return false;
  return prefs.visitDays.length >= PWA_INSTALL_MIN_VISIT_DAYS;
}

export function isPwaDisplayMode(): boolean {
  if (typeof window === 'undefined') return false;
  const mq = window.matchMedia('(display-mode: standalone), (display-mode: fullscreen)');
  const iosStandalone =
    'standalone' in window.navigator &&
    Boolean((window.navigator as Navigator & { standalone?: boolean }).standalone);
  return mq.matches || iosStandalone;
}

export function isIosSafari(): boolean {
  if (typeof window === 'undefined') return false;
  const ua = window.navigator.userAgent;
  const iOS =
    /iPad|iPhone|iPod/.test(ua) ||
    (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
  // Chrome/Firefox/Edge on iOS ship their own UA tokens - only Safari gets A2HS guidance.
  const isOtherBrowser = /CriOS|FxiOS|EdgiOS|OPiOS|Chrome|Android/.test(ua);
  return iOS && /Safari/i.test(ua) && !isOtherBrowser;
}

export function isMobileViewport(): boolean {
  if (typeof window === 'undefined') return false;
  // Align with Tailwind `xl` (1280) where bottom nav disappears.
  return window.matchMedia('(max-width: 1279px)').matches;
}
