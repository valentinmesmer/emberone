/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { KeyboardEvent } from 'react';

const NAV_KEYS = ['ArrowRight', 'ArrowDown', 'ArrowLeft', 'ArrowUp', 'Home', 'End'];

/**
 * Keyboard handling of a WAI-ARIA radio group built from `role="radio"` buttons: arrow keys,
 * Home and End move focus and selection (roving tab index; the group owns one tab stop).
 */
export function handleRadioGroupKeyDown(event: KeyboardEvent<HTMLElement>): void {
  if (!NAV_KEYS.includes(event.key)) {
    return;
  }
  const items = Array.from(
    event.currentTarget.querySelectorAll<HTMLButtonElement>('[role="radio"]:not(:disabled)'),
  );
  const index = items.indexOf(document.activeElement as HTMLButtonElement);
  if (index < 0) {
    return;
  }
  event.preventDefault();
  let target: number;
  if (event.key === 'ArrowRight' || event.key === 'ArrowDown') {
    target = (index + 1) % items.length;
  } else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') {
    target = (index - 1 + items.length) % items.length;
  } else if (event.key === 'Home') {
    target = 0;
  } else {
    target = items.length - 1;
  }
  items[target]?.focus();
  items[target]?.click();
}
