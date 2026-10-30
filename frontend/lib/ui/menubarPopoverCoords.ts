/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export const MENUBAR_POPOVER_MAX_WIDTH_PX = 26 * 16;
export const MENUBAR_POPOVER_VIEWPORT_MARGIN_PX = 12;

export type MenubarPopoverAnchor = Pick<DOMRect, 'left' | 'right' | 'bottom'>;

/** Fixed-position coords for a menubar filter popover under its trigger button. */
export function menubarPopoverCoords(
  anchor: MenubarPopoverAnchor,
  viewportWidth: number,
  popoverWidth?: number,
): { top: number; left: number } {
  const margin = MENUBAR_POPOVER_VIEWPORT_MARGIN_PX;
  const maxWidth = Math.min(MENUBAR_POPOVER_MAX_WIDTH_PX, viewportWidth - margin * 2);
  let left = anchor.left;

  if (popoverWidth != null && popoverWidth > 0) {
    const width = Math.min(popoverWidth, maxWidth);
    if (left + width > viewportWidth - margin) {
      left = anchor.right - width;
    }
    left = Math.max(margin, Math.min(left, viewportWidth - margin - width));
  } else {
    left = Math.max(margin, left);
  }

  return { top: anchor.bottom + 4, left };
}
