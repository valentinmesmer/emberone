/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getTokenGetter } from '@/lib/api/tokenGetter';
import { useAuth } from '@/lib/auth/AuthContext';
import { getRuntimeConfig } from '@/lib/runtimeConfig';
import { queryClient } from '@/lib/store/query-client';
import {
  REALTIME_INITIAL_RECONNECT_DELAY_MS,
  REALTIME_MAX_RECONNECT_DELAY_MS,
  REALTIME_TOKEN_EXPIRED_CLOSE_CODE,
  REALTIME_USER_NOTIFICATIONS_DESTINATION,
  REALTIME_USER_PERMISSIONS_DESTINATION,
  REALTIME_USER_TODOS_DESTINATION,
  REALTIME_USER_WORKFLOWS_DESTINATION,
} from './constants';
import { RealtimeContext, type RealtimeContextValue, useRealtime } from './context';
import { dispatchRealtimeEvent } from './eventDispatcher';
import { isRealtimeEnabled } from './isRealtimeEnabled';
import { withJitter } from './jitter';
import type { RealtimeEvent, RealtimeTopicHandler } from './types';
import { buildRealtimeWebSocketUrl } from './wsUrl';

export { type RealtimeContextValue, useRealtime };

function parseRealtimeEvent(message: IMessage): RealtimeEvent | null {
  try {
    return JSON.parse(message.body) as RealtimeEvent;
  } catch {
    console.warn('Ignoring malformed realtime message');
    return null;
  }
}

export function RealtimeProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading: authLoading } = useAuth();
  const [isEnabled, setIsEnabled] = useState(false);
  const [clientConnected, setClientConnected] = useState(false);
  const shouldConnect = isEnabled && !authLoading && isAuthenticated;
  const isConnected = shouldConnect && clientConnected;
  const clientRef = useRef<Client | null>(null);
  const topicHandlersRef = useRef(new Map<string, Set<RealtimeTopicHandler>>());
  const topicSubscriptionsRef = useRef(new Map<string, StompSubscription>());
  const reconnectDelayRef = useRef(REALTIME_INITIAL_RECONNECT_DELAY_MS);

  useEffect(() => {
    let cancelled = false;
    void getRuntimeConfig().then((config) => {
      if (!cancelled) {
        setIsEnabled(isRealtimeEnabled(config));
      }
    });
    return () => {
      cancelled = true;
    };
  }, []);

  const handleRealtimeEvent = useCallback((event: RealtimeEvent) => {
    dispatchRealtimeEvent(queryClient, event);
  }, []);

  const ensureTopicSubscription = useCallback((topic: string) => {
    const client = clientRef.current;
    if (!client?.connected || topicSubscriptionsRef.current.has(topic)) {
      return;
    }
    const subscription = client.subscribe(topic, (message) => {
      const event = parseRealtimeEvent(message);
      if (!event) {
        return;
      }
      const handlers = topicHandlersRef.current.get(topic);
      handlers?.forEach((handler) => {
        handler(event);
      });
    });
    topicSubscriptionsRef.current.set(topic, subscription);
  }, []);

  const releaseTopicSubscription = useCallback((topic: string) => {
    const handlers = topicHandlersRef.current.get(topic);
    if (handlers && handlers.size > 0) {
      return;
    }
    const subscription = topicSubscriptionsRef.current.get(topic);
    if (subscription && clientRef.current?.connected) {
      subscription.unsubscribe();
    }
    topicSubscriptionsRef.current.delete(topic);
  }, []);

  const subscribeTopic = useCallback(
    (topic: string, handler: RealtimeTopicHandler) => {
      const handlers = topicHandlersRef.current.get(topic) ?? new Set<RealtimeTopicHandler>();
      handlers.add(handler);
      topicHandlersRef.current.set(topic, handlers);
      ensureTopicSubscription(topic);

      return () => {
        const currentHandlers = topicHandlersRef.current.get(topic);
        if (!currentHandlers) {
          return;
        }
        currentHandlers.delete(handler);
        if (currentHandlers.size === 0) {
          topicHandlersRef.current.delete(topic);
          releaseTopicSubscription(topic);
        }
      };
    },
    [ensureTopicSubscription, releaseTopicSubscription],
  );

  const subscribeUserQueues = useCallback(
    (client: Client) => {
      const onUserEvent = (message: IMessage) => {
        const event = parseRealtimeEvent(message);
        if (event) {
          handleRealtimeEvent(event);
        }
      };
      client.subscribe(REALTIME_USER_NOTIFICATIONS_DESTINATION, onUserEvent);
      client.subscribe(REALTIME_USER_TODOS_DESTINATION, onUserEvent);
      client.subscribe(REALTIME_USER_PERMISSIONS_DESTINATION, onUserEvent);
      client.subscribe(REALTIME_USER_WORKFLOWS_DESTINATION, onUserEvent);
    },
    [handleRealtimeEvent],
  );

  const resubscribeTopics = useCallback(() => {
    if (clientRef.current?.connected) {
      topicSubscriptionsRef.current.forEach((subscription) => {
        subscription.unsubscribe();
      });
    }
    topicSubscriptionsRef.current.clear();
    for (const topic of topicHandlersRef.current.keys()) {
      ensureTopicSubscription(topic);
    }
  }, [ensureTopicSubscription]);

  useEffect(() => {
    const topicSubscriptions = topicSubscriptionsRef.current;

    if (!shouldConnect) {
      clientRef.current?.deactivate();
      clientRef.current = null;
      topicSubscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      topicSubscriptions.clear();
      return;
    }

    let disposed = false;

    const connect = async () => {
      const config = await getRuntimeConfig();
      if (!isRealtimeEnabled(config) || disposed) {
        return;
      }
      const apiBaseUrl = config.NEXT_PUBLIC_API_BASE_URL;
      if (!apiBaseUrl) {
        return;
      }

      const client = new Client({
        brokerURL: buildRealtimeWebSocketUrl(apiBaseUrl),
        reconnectDelay: reconnectDelayRef.current,
        heartbeatIncoming: 10_000,
        heartbeatOutgoing: 10_000,
        beforeConnect: async () => {
          const tokenGetter = getTokenGetter();
          const token = tokenGetter ? await tokenGetter() : null;
          if (!token) {
            throw new Error('No access token for realtime connection');
          }
          client.connectHeaders = { Authorization: `Bearer ${token}` };
        },
        onConnect: () => {
          reconnectDelayRef.current = withJitter(REALTIME_INITIAL_RECONNECT_DELAY_MS);
          client.reconnectDelay = reconnectDelayRef.current;
          setClientConnected(true);
          subscribeUserQueues(client);
          resubscribeTopics();
        },
        onDisconnect: () => {
          setClientConnected(false);
        },
        onWebSocketClose: (event) => {
          setClientConnected(false);
          if (event.code === REALTIME_TOKEN_EXPIRED_CLOSE_CODE) {
            reconnectDelayRef.current = withJitter(REALTIME_INITIAL_RECONNECT_DELAY_MS);
            client.reconnectDelay = reconnectDelayRef.current;
            return;
          }
          reconnectDelayRef.current = Math.min(
            withJitter(reconnectDelayRef.current * 2),
            REALTIME_MAX_RECONNECT_DELAY_MS,
          );
          client.reconnectDelay = reconnectDelayRef.current;
        },
        onStompError: () => {
          setClientConnected(false);
        },
      });

      clientRef.current = client;
      client.activate();
    };

    void connect();

    return () => {
      disposed = true;
      clientRef.current?.deactivate();
      clientRef.current = null;
      topicSubscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      topicSubscriptions.clear();
    };
  }, [resubscribeTopics, shouldConnect, subscribeUserQueues]);

  const value = useMemo<RealtimeContextValue>(
    () => ({
      isEnabled,
      isConnected,
      subscribeTopic,
    }),
    [isConnected, isEnabled, subscribeTopic],
  );

  return <RealtimeContext.Provider value={value}>{children}</RealtimeContext.Provider>;
}
