/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect, useRef } from 'react';
import { queryClient } from '@/lib/store/query-client';
import { dispatchRealtimeEvent } from './eventDispatcher';
import { useRealtime } from './RealtimeContext';
import type { RealtimeEvent } from './types';

/**
 * Subscribes to a tenant or user STOMP topic for the lifetime of the calling component.
 * Thin events are dispatched through the central cache invalidation layer by default;
 * pass a custom handler to add page-local behaviour.
 */
export function useRealtimeTopic(
  topic: string | null | undefined,
  handler?: (event: RealtimeEvent) => void,
) {
  const { isEnabled, subscribeTopic } = useRealtime();
  const handlerRef = useRef(handler);

  useEffect(() => {
    handlerRef.current = handler;
  });

  useEffect(() => {
    if (!isEnabled || !topic) {
      return;
    }

    return subscribeTopic(topic, (event) => {
      dispatchRealtimeEvent(queryClient, event);
      handlerRef.current?.(event);
    });
  }, [isEnabled, subscribeTopic, topic]);
}
