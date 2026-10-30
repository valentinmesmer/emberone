/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import {
  forwardRef,
  useCallback,
  useEffect,
  useId,
  useLayoutEffect,
  useRef,
  useState,
} from 'react';
import { createPortal } from 'react-dom';
import { cn } from '@/lib/utils/cn';

const shapeClass = {
  'rounded-xl': 'rounded-xl',
  'rounded-lg': 'rounded-lg',
  'rounded-full': 'rounded-full',
  none: 'rounded-none',
} as const;

export type HoverHintShape = keyof typeof shapeClass;

/** `top`: bubble above the trigger (default). `bottom`: below - e.g. fixed header at viewport top. */
export type HoverHintSide = 'top' | 'bottom';

export interface HoverHintProps {
  /**
   * Tooltip copy. When empty, children are returned unchanged (no wrapper).
   * Prefer this over native `title` so hints are theme-aware and work on disabled controls.
   */
  hint: string | undefined;
  children: React.ReactNode;
  /**
   * When true, a transparent layer sits above the child so hover works on `disabled`
   * controls (native `title` does not show there).
   */
  capturePointer?: boolean;
  shape?: HoverHintShape;
  /** Where the bubble appears relative to the trigger. */
  side?: HoverHintSide;
  className?: string;
}

const HINT_GAP_PX = 8;
const VIEW_MARGIN_PX = 12;
/** Above modal (z-[100]) and toasts (z-[200]); portaled so not clipped by overflow. */
const HINT_Z = 10050;

function clampTooltipPosition(args: {
  anchorX: number;
  anchorTop: number;
  side: HoverHintSide;
  tipWidth: number;
  tipHeight: number;
}): { left: number; top: number } {
  const { anchorX, anchorTop, side, tipWidth, tipHeight } = args;
  const vw = typeof window !== 'undefined' ? window.innerWidth : 0;
  const vh = typeof window !== 'undefined' ? window.innerHeight : 0;
  const halfW = tipWidth / 2;
  const minLeft = VIEW_MARGIN_PX + halfW;
  const maxLeft = vw - VIEW_MARGIN_PX - halfW;
  let left = anchorX;
  if (vw > 0 && tipWidth > 0) {
    if (minLeft > maxLeft) {
      left = vw / 2;
    } else {
      left = Math.min(Math.max(anchorX, minLeft), maxLeft);
    }
  }

  let top = anchorTop;
  if (vh > 0 && tipHeight > 0) {
    if (side === 'top') {
      // `top` CSS + translateY(-100% - 2px): visual top ≈ anchorTop - tipHeight - 2
      const bubbleTop = anchorTop - tipHeight - 2;
      if (bubbleTop < VIEW_MARGIN_PX) {
        top = anchorTop + (VIEW_MARGIN_PX - bubbleTop);
      }
    } else {
      // translateY(2px): visual bottom ≈ anchorTop + 2 + tipHeight
      const bubbleBottom = anchorTop + 2 + tipHeight;
      if (bubbleBottom > vh - VIEW_MARGIN_PX) {
        top = anchorTop - (bubbleBottom - (vh - VIEW_MARGIN_PX));
      }
    }
  }

  return { left, top };
}

/**
 * Reusable hover hint bubble (pointer hover only - avoids stuck bubbles on touch after focus).
 * Renders the bubble in a portal so it is not clipped by ancestor `overflow` and stacks above app chrome.
 * Does not set native `title` - strip `title` on the wrapped control to avoid a second browser tooltip.
 */
export const HoverHint = forwardRef<HTMLSpanElement, HoverHintProps>(function HoverHint(
  { hint, children, capturePointer = false, shape = 'rounded-xl', side = 'top', className },
  forwardedRef,
) {
  const text = hint?.trim();
  const rootRef = useRef<HTMLSpanElement>(null);
  const tooltipRef = useRef<HTMLSpanElement>(null);
  const anchorRef = useRef({ x: 0, y: 0 });
  const tooltipId = useId();
  const [open, setOpen] = useState(false);
  const [coords, setCoords] = useState<{ left: number; top: number } | null>(null);
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  useEffect(() => {
    const element = rootRef.current;
    if (!element || !text) return;
    const show = () => setOpen(true);
    const hide = () => setOpen(false);
    element.addEventListener('mouseenter', show);
    element.addEventListener('mouseleave', hide);
    return () => {
      element.removeEventListener('mouseenter', show);
      element.removeEventListener('mouseleave', hide);
    };
  }, [text]);

  const setRefs = useCallback(
    (el: HTMLSpanElement | null) => {
      rootRef.current = el;
      if (typeof forwardedRef === 'function') {
        forwardedRef(el);
      } else if (forwardedRef) {
        (forwardedRef as React.MutableRefObject<HTMLSpanElement | null>).current = el;
      }
    },
    [forwardedRef],
  );

  const updatePosition = useCallback(() => {
    const el = rootRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const ax = rect.left + rect.width / 2;
    if (side === 'bottom') {
      const ay = rect.bottom + HINT_GAP_PX;
      anchorRef.current = { x: ax, y: ay };
      setCoords({ left: ax, top: ay });
    } else {
      const ay = rect.top - HINT_GAP_PX;
      anchorRef.current = { x: ax, y: ay };
      setCoords({ left: ax, top: ay });
    }
  }, [side]);

  useLayoutEffect(() => {
    if (!open || !text) {
      setCoords(null);
      return;
    }
    updatePosition();
    const el = rootRef.current;
    const ro = typeof ResizeObserver !== 'undefined' ? new ResizeObserver(updatePosition) : null;
    if (el && ro) ro.observe(el);
    window.addEventListener('scroll', updatePosition, true);
    window.addEventListener('resize', updatePosition);
    return () => {
      ro?.disconnect();
      window.removeEventListener('scroll', updatePosition, true);
      window.removeEventListener('resize', updatePosition);
    };
  }, [open, text, updatePosition]);

  useLayoutEffect(() => {
    if (!open || !text || !coords || !tooltipRef.current) return;
    const tip = tooltipRef.current;
    const box = tip.getBoundingClientRect();
    const { x: anchorX, y: anchorTop } = anchorRef.current;
    const next = clampTooltipPosition({
      anchorX,
      anchorTop,
      side,
      tipWidth: box.width,
      tipHeight: box.height,
    });
    if (Math.abs(next.left - coords.left) > 0.5 || Math.abs(next.top - coords.top) > 0.5) {
      setCoords(next);
    }
  }, [open, text, coords, side]);

  const wrapperBase = 'relative inline-flex max-w-full align-middle';

  // Without a hint there is no tooltip, but callers often still pass `className` for layout
  // (e.g. SwitchField uses flex). Returning a bare fragment drops those classes and breaks flex.
  if (!text) {
    if (!className) return <>{children}</>;
    return (
      <span ref={setRefs} className={cn(wrapperBase, className)}>
        {children}
      </span>
    );
  }

  const radius = shapeClass[shape];

  const tooltipNode =
    mounted && open && coords && typeof document !== 'undefined' ? (
      <span
        ref={tooltipRef}
        id={tooltipId}
        role="tooltip"
        style={{
          boxShadow: 'var(--hint-shadow)',
          position: 'fixed',
          left: coords.left,
          top: coords.top,
          zIndex: HINT_Z,
          transform:
            side === 'bottom' ? 'translate(-50%, 2px)' : 'translate(-50%, calc(-100% - 2px))',
        }}
        className={cn(
          'pointer-events-none w-max max-w-[min(280px,calc(100vw-2rem))]',
          'rounded-lg border border-[var(--hint-border)] bg-[var(--hint-surface)] px-3 py-2 text-center text-[13px] font-medium leading-snug text-[var(--hint-ink)] whitespace-normal',
          'opacity-100 transition-opacity duration-150 ease-out',
        )}
      >
        {text}
      </span>
    ) : null;

  return (
    <>
      <span
        ref={setRefs}
        className={cn(wrapperBase, className)}
        aria-describedby={open ? tooltipId : undefined}
      >
        {children}
        {capturePointer ? (
          <span
            className={cn('pointer-events-auto absolute inset-0 z-[1] cursor-not-allowed', radius)}
            aria-hidden
          />
        ) : null}
      </span>
      {tooltipNode ? createPortal(tooltipNode, document.body) : null}
    </>
  );
});

HoverHint.displayName = 'HoverHint';
