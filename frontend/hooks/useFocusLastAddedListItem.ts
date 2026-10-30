/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useLayoutEffect, useRef, type RefObject } from 'react';

/**
 * After items are appended to a list container, scroll the last item into view.
 * Deferred so the native file picker can close before scrolling.
 */
export function useFocusLastAddedListItem<T extends HTMLElement>(
  itemCount: number,
): RefObject<T | null> {
  const containerRef = useRef<T>(null);
  const prevCountRef = useRef(itemCount);

  useLayoutEffect(() => {
    const prevCount = prevCountRef.current;
    prevCountRef.current = itemCount;

    if (itemCount <= prevCount || !containerRef.current) return;

    const lastItem = containerRef.current.lastElementChild as HTMLElement | null;
    if (!lastItem) return;

    let cancelled = false;
    const timer = window.setTimeout(() => {
      if (cancelled) return;
      lastItem.scrollIntoView({ block: 'center', behavior: 'auto' });
    }, 100);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [itemCount]);

  return containerRef;
}
