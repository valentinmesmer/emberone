/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { createContext, useContext } from 'react';
import type { RealtimeTopicHandler } from './types';

export type RealtimeContextValue = {
  isEnabled: boolean;
  isConnected: boolean;
  subscribeTopic: (topic: string, handler: RealtimeTopicHandler) => () => void;
};

export const RealtimeContext = createContext<RealtimeContextValue | null>(null);

export function useRealtime(): RealtimeContextValue {
  const context = useContext(RealtimeContext);
  if (!context) {
    throw new Error('useRealtime must be used within RealtimeProvider');
  }
  return context;
}
