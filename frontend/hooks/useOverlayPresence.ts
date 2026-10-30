/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useState } from 'react';

/**
 * Presence + enter state for an overlay host that animates in and out.
 *
 * `present` must gate `display`, not just opacity/transform. iOS 26 Safari tints
 * its top and bottom bars from the top-most `position: fixed` / `sticky` element
 * at each viewport edge, and WebKit deliberately *keeps the previous bar color*
 * whenever that element is viewport-sized (it treats it as a modal that should
 * not repaint the browser chrome - `preferExistingColor` in
 * `LocalFrameView::fixedContainerEdges`). A closed-but-still-mounted overlay host
 * at `inset-0` counts even while fully transparent: it has a child, so WebKit
 * does not consider it hidden. The bars then stay frozen at whatever color they
 * had on page load, which is why a theme switch only showed up after a reload.
 * Taking the host out of the render tree hands the edges back to the real chrome
 * (`Header` / `MobileBottomNav`), whose background color follows the theme.
 *
 * `entered` turns true one painted frame after the host became `present`, so the
 * enter transition has a starting style to animate from, and drops back to false
 * the moment `open` does, so the exit transition runs before the host is hidden.
 *
 * @param open   whether the overlay should be shown
 * @param exitMs exit transition duration; keep in sync with the CSS duration
 */
export function useOverlayPresence(
  open: boolean,
  exitMs: number,
): { present: boolean; entered: boolean } {
  const [enterFramePainted, setEnterFramePainted] = useState(open);
  const [exitFinished, setExitFinished] = useState(!open);

  useEffect(() => {
    if (open) {
      const frame = requestAnimationFrame(() => {
        setEnterFramePainted(true);
        setExitFinished(false);
      });
      return () => cancelAnimationFrame(frame);
    }
    const timeout = window.setTimeout(() => {
      setEnterFramePainted(false);
      setExitFinished(true);
    }, exitMs);
    return () => window.clearTimeout(timeout);
  }, [open, exitMs]);

  return { present: open || !exitFinished, entered: open && enterFramePainted };
}
