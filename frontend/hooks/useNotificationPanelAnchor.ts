/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useLayoutEffect, useState, useSyncExternalStore } from 'react';

const PANEL_WIDTH_PX = 352; // 22rem
const PANEL_MAX_HEIGHT_PX = 448; // 28rem
const VIEWPORT_MARGIN_PX = 16;
const BELL_GAP_PX = 8;

export type NotificationPanelAnchor = {
  top: number;
  left: number;
  width: number;
  maxHeight: number;
};

/** Tablet+ uses a dropdown; below this breakpoint the panel is a full-width sheet. */
const DROPDOWN_MEDIA = '(min-width: 768px)';

function getVisibleBellRect(): DOMRect | null {
  const buttons = document.querySelectorAll<HTMLElement>('button[data-notification-bell]');
  for (const el of buttons) {
    const rect = el.getBoundingClientRect();
    if (rect.width > 0 && rect.height > 0) {
      return rect;
    }
  }
  return null;
}

function computeAnchor(viewport: {
  width: number;
  height: number;
}): NotificationPanelAnchor | null {
  const bell = getVisibleBellRect();
  if (!bell) {
    return null;
  }

  const width = Math.min(PANEL_WIDTH_PX, viewport.width - 2 * VIEWPORT_MARGIN_PX);
  const minLeft = VIEWPORT_MARGIN_PX;
  const maxLeft = viewport.width - width - VIEWPORT_MARGIN_PX;

  const top = bell.bottom + BELL_GAP_PX;
  const maxHeight = Math.min(PANEL_MAX_HEIGHT_PX, viewport.height - top - VIEWPORT_MARGIN_PX);

  // Mobile-first placement: prefer opening to the right of the bell (sidebar).
  let left = bell.right + BELL_GAP_PX;

  if (left > maxLeft) {
    // Bell sits on the viewport edge (header): align the panel's right edge with the bell.
    left = bell.right - width;
  }

  left = Math.max(minLeft, Math.min(left, maxLeft));

  return {
    top,
    left,
    width,
    maxHeight: Math.max(160, maxHeight),
  };
}

function subscribeDropdownMq(onStoreChange: () => void) {
  const mq = window.matchMedia(DROPDOWN_MEDIA);
  mq.addEventListener('change', onStoreChange);
  return () => mq.removeEventListener('change', onStoreChange);
}

function getDropdownMqSnapshot() {
  return window.matchMedia(DROPDOWN_MEDIA).matches;
}

function getDropdownMqServerSnapshot() {
  return false;
}

/**
 * Positions the notification dropdown relative to the visible bell trigger (`md+`).
 * Returns `null` on mobile where the panel uses a full-height sheet.
 */
export function useNotificationPanelAnchor(open: boolean): {
  anchor: NotificationPanelAnchor | null;
  isDropdown: boolean;
} {
  const dropdownMqMatches = useSyncExternalStore(
    subscribeDropdownMq,
    getDropdownMqSnapshot,
    getDropdownMqServerSnapshot,
  );
  const isDropdown = open && dropdownMqMatches;
  const [anchor, setAnchor] = useState<NotificationPanelAnchor | null>(null);

  useLayoutEffect(() => {
    if (!isDropdown) {
      return;
    }

    const update = () => {
      setAnchor(
        computeAnchor({
          width: window.innerWidth,
          height: window.innerHeight,
        }),
      );
    };

    update();
    window.addEventListener('resize', update);
    window.addEventListener('scroll', update, true);

    return () => {
      window.removeEventListener('resize', update);
      window.removeEventListener('scroll', update, true);
    };
  }, [isDropdown]);

  return {
    anchor: isDropdown ? anchor : null,
    isDropdown,
  };
}
