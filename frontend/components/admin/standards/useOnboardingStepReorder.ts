/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { OnboardingStepType } from '@/lib/api/standards';

const DRAG_THRESHOLD_PX = 8;
const CLICK_SUPPRESS_MS = 500;

function suppressNextClick() {
  const prevent = (event: Event) => {
    event.preventDefault();
    event.stopPropagation();
  };
  document.addEventListener('click', prevent, true);
  window.setTimeout(() => {
    document.removeEventListener('click', prevent, true);
  }, CLICK_SUPPRESS_MS);
}

function stepTypeFromY(list: Element, clientY: number): OnboardingStepType | null {
  let closestType: OnboardingStepType | null = null;
  let closest = Infinity;
  list.querySelectorAll('[data-step-type]').forEach((node) => {
    const stepType = node.getAttribute('data-step-type');
    if (!stepType) return;
    const rect = node.getBoundingClientRect();
    const dist = Math.abs(clientY - (rect.top + rect.height / 2));
    if (dist < closest) {
      closest = dist;
      closestType = stepType as OnboardingStepType;
    }
  });
  return closestType;
}

function moveStepType(
  types: OnboardingStepType[],
  fromType: OnboardingStepType,
  toType: OnboardingStepType,
): OnboardingStepType[] | null {
  if (fromType === toType) return null;
  const from = types.indexOf(fromType);
  const to = types.indexOf(toType);
  if (from < 0 || to < 0) return null;
  const next = [...types];
  const [moved] = next.splice(from, 1);
  next.splice(to, 0, moved);
  return next;
}

export function useOnboardingStepReorder({
  steps,
  disabled = false,
  onReorder,
}: {
  steps: { stepType: OnboardingStepType }[];
  disabled?: boolean;
  onReorder: (orderedTypes: OnboardingStepType[]) => void | Promise<void>;
}) {
  const [draggedType, setDraggedType] = useState<OnboardingStepType | null>(null);
  const [dragOverType, setDragOverType] = useState<OnboardingStepType | null>(null);

  const dragRef = useRef<{
    stepType: OnboardingStepType;
    pointerId: number;
    startX: number;
    startY: number;
    list: Element;
    passedThreshold: boolean;
  } | null>(null);

  const stepsRef = useRef(steps);
  const disabledRef = useRef(disabled);
  const onReorderRef = useRef(onReorder);
  const detachRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    stepsRef.current = steps;
    disabledRef.current = disabled;
    onReorderRef.current = onReorder;
  });

  useEffect(() => () => detachRef.current?.(), []);

  const stopDrag = useCallback(() => {
    dragRef.current = null;
    setDraggedType(null);
    setDragOverType(null);
  }, []);

  const onHandlePointerDown = useCallback(
    (stepType: OnboardingStepType, event: React.PointerEvent<HTMLButtonElement>) => {
      if (disabledRef.current || event.button !== 0) return;
      const handle = event.currentTarget;
      const list = handle.closest('ul');
      if (!list) return;

      event.stopPropagation();
      detachRef.current?.();

      dragRef.current = {
        stepType,
        pointerId: event.pointerId,
        startX: event.clientX,
        startY: event.clientY,
        list,
        passedThreshold: false,
      };

      try {
        handle.setPointerCapture(event.pointerId);
      } catch {
        /* Pointer capture is optional; window listeners still drive the gesture. */
      }

      const preventScroll = (touchEvent: TouchEvent) => {
        if (dragRef.current) {
          touchEvent.preventDefault();
        }
      };

      const onMove = (ev: PointerEvent) => {
        const drag = dragRef.current;
        if (!drag || ev.pointerId !== drag.pointerId) return;
        const dx = ev.clientX - drag.startX;
        const dy = ev.clientY - drag.startY;
        if (!drag.passedThreshold) {
          if (Math.hypot(dx, dy) < DRAG_THRESHOLD_PX) return;
          drag.passedThreshold = true;
          setDraggedType(drag.stepType);
        }
        const overType = stepTypeFromY(drag.list, ev.clientY);
        setDragOverType(overType !== null && overType !== drag.stepType ? overType : null);
      };

      const onUp = (ev: PointerEvent) => {
        const drag = dragRef.current;
        if (!drag || ev.pointerId !== drag.pointerId) return;

        detachRef.current?.();

        if (handle.hasPointerCapture(ev.pointerId)) {
          handle.releasePointerCapture(ev.pointerId);
        }

        if (ev.type === 'pointercancel' || !drag.passedThreshold) {
          stopDrag();
          return;
        }

        suppressNextClick();
        const overType = stepTypeFromY(drag.list, ev.clientY);
        const ordered =
          overType !== null
            ? moveStepType(
                stepsRef.current.map((step) => step.stepType),
                drag.stepType,
                overType,
              )
            : null;
        stopDrag();
        if (ordered) {
          void onReorderRef.current(ordered);
        }
      };

      const detach = () => {
        window.removeEventListener('pointermove', onMove);
        window.removeEventListener('pointerup', onUp);
        window.removeEventListener('pointercancel', onUp);
        window.removeEventListener('touchmove', preventScroll);
        detachRef.current = null;
      };
      detachRef.current = detach;

      window.addEventListener('pointermove', onMove);
      window.addEventListener('pointerup', onUp);
      window.addEventListener('pointercancel', onUp);
      window.addEventListener('touchmove', preventScroll, { passive: false });
    },
    [stopDrag],
  );

  return {
    draggedType,
    dragOverType,
    isDragging: draggedType !== null,
    onHandlePointerDown,
  };
}
