/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Session-scoped reminder that the user still has open onboarding(s).
 * Shown once per browser session after login; dismiss resets only when the tab/session ends.
 */

const STORAGE_KEY = 'emberone.onboardingReminder.dismissed';

export function wasOnboardingReminderDismissedThisSession(): boolean {
  if (typeof window === 'undefined') return true;
  try {
    return sessionStorage.getItem(STORAGE_KEY) === '1';
  } catch {
    return false;
  }
}

export function dismissOnboardingReminderForSession(): void {
  if (typeof window === 'undefined') return;
  try {
    sessionStorage.setItem(STORAGE_KEY, '1');
  } catch {
    // ignore quota / private-mode failures
  }
}
