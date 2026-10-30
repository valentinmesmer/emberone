/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useEffect, useRef, useState } from 'react';

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

function idFromY(list: Element, clientY: number): number | null {
  let closestId: number | null = null;
  let closest = Infinity;
  list.querySelectorAll('[data-contact-id]').forEach((node) => {
    const id = Number(node.getAttribute('data-contact-id'));
    if (!Number.isFinite(id)) return;
    const rect = node.getBoundingClientRect();
    const dist = Math.abs(clientY - (rect.top + rect.height / 2));
    if (dist < closest) {
      closest = dist;
      closestId = id;
    }
  });
  return closestId;
}

function moveId(ids: number[], fromId: number, toId: number): number[] | null {
  if (fromId === toId) return null;
  const from = ids.indexOf(fromId);
  const to = ids.indexOf(toId);
  if (from < 0 || to < 0) return null;
  const next = [...ids];
  const [moved] = next.splice(from, 1);
  next.splice(to, 0, moved);
  return next;
}

export function useEmergencyContactReorder({
  contacts,
  disabled = false,
  onReorder,
}: {
  contacts: { id: number }[];
  disabled?: boolean;
  onReorder: (orderedIds: number[]) => void | Promise<void>;
}) {
  const [draggedId, setDraggedId] = useState<number | null>(null);
  const [dragOverId, setDragOverId] = useState<number | null>(null);

  const dragRef = useRef<{
    id: number;
    pointerId: number;
    startX: number;
    startY: number;
    list: Element;
    passedThreshold: boolean;
  } | null>(null);

  const contactsRef = useRef(contacts);
  const disabledRef = useRef(disabled);
  const onReorderRef = useRef(onReorder);
  const detachRef = useRef<(() => void) | null>(null);

  useEffect(() => {
    contactsRef.current = contacts;
    disabledRef.current = disabled;
    onReorderRef.current = onReorder;
  });

  useEffect(() => () => detachRef.current?.(), []);

  const stopDrag = useCallback(() => {
    dragRef.current = null;
    setDraggedId(null);
    setDragOverId(null);
  }, []);

  const onHandlePointerDown = useCallback(
    (contactId: number, event: React.PointerEvent<HTMLButtonElement>) => {
      if (disabledRef.current || event.button !== 0) return;
      const handle = event.currentTarget;
      const list = handle.closest('ul');
      if (!list) return;

      event.stopPropagation();
      detachRef.current?.();

      dragRef.current = {
        id: contactId,
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
          setDraggedId(drag.id);
        }
        const overId = idFromY(drag.list, ev.clientY);
        setDragOverId(overId !== null && overId !== drag.id ? overId : null);
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
        const overId = idFromY(drag.list, ev.clientY);
        const ordered =
          overId !== null
            ? moveId(
                contactsRef.current.map((c) => c.id),
                drag.id,
                overId,
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
    draggedId,
    dragOverId,
    isDragging: draggedId !== null,
    onHandlePointerDown,
  };
}
