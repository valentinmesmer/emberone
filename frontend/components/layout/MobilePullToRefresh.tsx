/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ArrowDown, Loader2 } from 'lucide-react';
import { usePathname } from 'next/navigation';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { useRefreshServerCache } from '@/lib/store/useRefreshServerCache';
import {
  movePullGesture,
  PULL_TOP_SETTLE_MS,
  type PullGesture,
  startPullGesture,
} from '@/lib/ui/pullToRefresh';

const MOBILE_QUERY = '(max-width: 1279px)';
const EDITABLE = 'input, textarea, select, [contenteditable]:not([contenteditable="false"])';

function isPageAvailable(): boolean {
  return (
    document.body.scrollTop <= 0 &&
    document.documentElement.scrollTop <= 0 &&
    getComputedStyle(document.body).overflowY !== 'hidden' &&
    !document.querySelector('[aria-modal="true"], dialog[open]') &&
    !document.activeElement?.matches(EDITABLE)
  );
}

/** Chrome, controls and nested scrolling never become page-refresh gestures. */
function canStartFrom(target: EventTarget | null): boolean {
  if (!(target instanceof Element) || !target.closest('[data-pull-to-refresh-content]'))
    return false;
  if (
    target.closest(
      `${EDITABLE}, button, a, [role="slider"], [role="combobox"], [role="menu"], [role="listbox"]`,
    )
  )
    return false;
  for (
    let element: Element | null = target;
    element && element !== document.body;
    element = element.parentElement
  ) {
    const style = getComputedStyle(element);
    if (
      (/(auto|scroll)/.test(style.overflowY) && element.scrollHeight > element.clientHeight) ||
      (/(auto|scroll)/.test(style.overflowX) && element.scrollWidth > element.clientWidth)
    )
      return false;
  }
  return true;
}

/** A deliberate page gesture; the mobile menu provides the non-gesture alternative. */
export function MobilePullToRefresh() {
  const t = useTranslation();
  const { isAuthenticated } = useAuth();
  const pathname = usePathname();
  const enabled = isAuthenticated && !/^\/(auth|setup)(\/|$)/.test(pathname);
  const { status, refresh, disabled } = useRefreshServerCache();
  const [pull, setPull] = useState<PullGesture | null>(null);

  useEffect(() => {
    if (!enabled || disabled) return;
    const mobile = window.matchMedia(MOBILE_QUERY);
    let gesture: PullGesture | null = null;
    let lastScrollAt = performance.now();

    const reset = () => {
      gesture = null;
      setPull(null);
    };
    const onScroll = () => {
      lastScrollAt = performance.now();
      reset();
    };
    const onTouchStart = (event: TouchEvent) => {
      reset();
      if (
        !mobile.matches ||
        event.touches.length !== 1 ||
        !isPageAvailable() ||
        performance.now() - lastScrollAt < PULL_TOP_SETTLE_MS ||
        !canStartFrom(event.target)
      )
        return;
      const touch = event.touches[0];
      gesture = startPullGesture(touch.clientX, touch.clientY);
    };
    const onTouchMove = (event: TouchEvent) => {
      if (!gesture) return;
      if (
        !mobile.matches ||
        event.touches.length !== 1 ||
        !isPageAvailable() ||
        !event.cancelable
      ) {
        reset();
        return;
      }
      const touch = event.touches[0];
      gesture = movePullGesture(gesture, touch.clientX, touch.clientY);
      setPull(gesture);
      if (gesture && gesture.phase !== 'pending') event.preventDefault();
    };
    const onTouchEnd = () => {
      const ready = gesture?.phase === 'ready' && mobile.matches && isPageAvailable();
      reset();
      if (ready) void refresh();
    };

    document.body.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('touchstart', onTouchStart, { passive: true });
    window.addEventListener('touchmove', onTouchMove, { passive: false });
    window.addEventListener('touchend', onTouchEnd);
    window.addEventListener('touchcancel', reset);
    mobile.addEventListener('change', reset);
    return () => {
      reset();
      document.body.removeEventListener('scroll', onScroll);
      window.removeEventListener('touchstart', onTouchStart);
      window.removeEventListener('touchmove', onTouchMove);
      window.removeEventListener('touchend', onTouchEnd);
      window.removeEventListener('touchcancel', reset);
      mobile.removeEventListener('change', reset);
    };
  }, [enabled, disabled, refresh]);

  const pulling = pull !== null && pull.phase !== 'pending';
  const ready = pull?.phase === 'ready';
  const refreshing = status === 'refreshing';
  const feedback = pulling
    ? t(ready ? 'common.pullToRefresh.release' : 'common.pullToRefresh.pullMore')
    : status !== 'idle'
      ? t(`common.pullToRefresh.${status}`)
      : '';

  const failed = status === 'error' || status === 'offline';
  const visible = pulling || refreshing || failed;
  const label = pulling || failed ? feedback : '';
  if (!enabled) return null;

  return (
    <div className="pointer-events-none fixed inset-x-3 top-[var(--app-header-offset)] z-40 flex justify-center xl:hidden">
      <div
        className={`inline-flex items-center justify-center border border-[var(--border-color)] bg-[var(--card-bg)] text-sm font-medium text-[var(--text-primary)] shadow-sm transition-opacity duration-150 motion-reduce:transition-none ${
          refreshing && !pulling && !failed
            ? 'rounded-full p-2'
            : 'max-w-sm gap-2 rounded-xl px-3 py-2'
        } ${visible ? 'opacity-100' : 'opacity-0'}`}
        style={{ transform: `translateY(${pulling ? Math.min(pull.offset, 40) : 12}px)` }}
        aria-hidden={!visible}
      >
        {pulling && (
          <ArrowDown
            className={`h-4 w-4 shrink-0 transition-transform motion-reduce:transition-none ${ready ? 'rotate-180' : ''}`}
            aria-hidden
          />
        )}
        {refreshing && (
          <Loader2
            className="h-4 w-4 shrink-0 animate-spin motion-reduce:animate-none"
            aria-hidden
          />
        )}
        {label ? <span>{label}</span> : null}
        {failed && (
          <Button
            type="button"
            variant="ghost"
            size="xs"
            disabled={disabled}
            onClick={() => void refresh()}
            className="pointer-events-auto min-h-11"
          >
            {t('common.pullToRefresh.retry')}
          </Button>
        )}
      </div>
      <span role="status" className="sr-only">
        {feedback}
      </span>
    </div>
  );
}
