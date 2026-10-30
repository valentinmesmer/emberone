/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { AlertCircle, AlertTriangle, Bell, CheckCircle2, X } from 'lucide-react';
import type React from 'react';
import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { MutationErrorToastBridge } from '@/lib/store/MutationErrorToastBridge';
import { cn } from '@/lib/utils/cn';

export type ToastType = 'success' | 'error' | 'info' | 'warning';

export interface Toast {
  id: string;
  message: string;
  type: ToastType;
  duration?: number;
}

interface ToastContextType {
  showToast: (message: string, type?: ToastType, duration?: number) => void;
  showSuccess: (message: string, duration?: number) => void;
  showError: (message: string, duration?: number) => void;
  showInfo: (message: string, duration?: number) => void;
  showWarning: (message: string, duration?: number) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

/** Max queued toasts (including the one currently visible). */
const MAX_QUEUED_TOASTS = 6;

/** Default auto-dismiss duration per toast level (ms). */
export const TOAST_DURATION_MS: Record<ToastType, number> = {
  success: 1500,
  info: 3000,
  warning: 4000,
  error: 5000,
};

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) {
    throw new Error('useToast must be used within ToastProvider');
  }
  return context;
}

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [queue, setQueue] = useState<Toast[]>([]);

  const removeToast = useCallback((id: string) => {
    setQueue((prev) => prev.filter((toast) => toast.id !== id));
  }, []);

  const showToast = useCallback((message: string, type: ToastType = 'info', duration?: number) => {
    const id = Math.random().toString(36).substring(2, 9);
    const resolvedDuration = duration ?? TOAST_DURATION_MS[type];
    const newToast: Toast = { id, message, type, duration: resolvedDuration };

    setQueue((prev) => {
      const next = [...prev, newToast];
      return next.length > MAX_QUEUED_TOASTS ? next.slice(next.length - MAX_QUEUED_TOASTS) : next;
    });
  }, []);

  const showSuccess = useCallback(
    (message: string, duration?: number) => showToast(message, 'success', duration),
    [showToast],
  );

  const showError = useCallback(
    (message: string, duration?: number) => showToast(message, 'error', duration),
    [showToast],
  );

  const showInfo = useCallback(
    (message: string, duration?: number) => showToast(message, 'info', duration),
    [showToast],
  );

  const showWarning = useCallback(
    (message: string, duration?: number) => showToast(message, 'warning', duration),
    [showToast],
  );

  const current = queue[0] ?? null;

  return (
    <ToastContext.Provider value={{ showToast, showSuccess, showError, showInfo, showWarning }}>
      {children}
      <MutationErrorToastBridge />
      <ToastContainer toast={current} onRemove={removeToast} />
    </ToastContext.Provider>
  );
}

interface ToastContainerProps {
  toast: Toast | null;
  onRemove: (id: string) => void;
}

function ToastContainer({ toast, onRemove }: ToastContainerProps) {
  if (!toast) return null;

  return (
    <div
      className={cn(
        'pointer-events-none fixed z-[200]',
        // Mobile-first: full width above bottom nav
        'inset-x-3 bottom-[var(--app-mobile-bottom-safe)]',
        // Desktop: bottom-right
        'xl:inset-x-auto xl:bottom-6 xl:right-6 xl:w-[22rem]',
      )}
      aria-live="polite"
      aria-relevant="additions"
    >
      <ToastItem key={toast.id} toast={toast} onRemove={onRemove} />
    </div>
  );
}

interface ToastItemProps {
  toast: Toast;
  onRemove: (id: string) => void;
}

const styleMap: Record<ToastType, string> = {
  success:
    'border-[var(--toast-success-border)] bg-[var(--toast-success-bg)] text-[var(--toast-success-text)]',
  error:
    'border-[var(--toast-error-border)] bg-[var(--toast-error-bg)] text-[var(--toast-error-text)]',
  info: 'border-[var(--toast-info-border)] bg-[var(--toast-info-bg)] text-[var(--toast-info-text)]',
  warning:
    'border-[var(--toast-warning-border)] bg-[var(--toast-warning-bg)] text-[var(--toast-warning-text)]',
};

const progressTrackMap: Record<ToastType, string> = {
  success: 'bg-[var(--toast-success-text)]/15',
  error: 'bg-[var(--toast-error-text)]/15',
  info: 'bg-[var(--toast-info-text)]/15',
  warning: 'bg-[var(--toast-warning-text)]/15',
};

const progressFillMap: Record<ToastType, string> = {
  success: 'bg-[var(--toast-success-text)]/55',
  error: 'bg-[var(--toast-error-text)]/55',
  info: 'bg-[var(--toast-info-text)]/55',
  warning: 'bg-[var(--toast-warning-text)]/55',
};

const iconMap: Record<ToastType, React.ReactNode> = {
  success: <CheckCircle2 className="h-4 w-4" strokeWidth={2} />,
  error: <AlertCircle className="h-4 w-4" strokeWidth={2} />,
  info: <Bell className="h-4 w-4" strokeWidth={2} />,
  warning: <AlertTriangle className="h-4 w-4" strokeWidth={2} />,
};

function ToastItem({ toast, onRemove }: ToastItemProps) {
  const duration = toast.duration && toast.duration > 0 ? toast.duration : 0;
  const [phase, setPhase] = useState<'enter' | 'shown' | 'exit'>('enter');
  const [paused, setPaused] = useState(false);
  const [remainingMs, setRemainingMs] = useState(duration);
  const remainingRef = useRef(duration);
  const lastTickRef = useRef<number | null>(null);
  const removedRef = useRef(false);
  const t = useTranslation();

  const finishRemove = useCallback(() => {
    if (removedRef.current) return;
    removedRef.current = true;
    onRemove(toast.id);
  }, [onRemove, toast.id]);

  const startExit = useCallback(() => {
    setPhase('exit');
  }, []);

  useEffect(() => {
    const enterId = requestAnimationFrame(() => setPhase('shown'));
    return () => cancelAnimationFrame(enterId);
  }, []);

  useEffect(() => {
    if (phase !== 'exit') return;
    const timeoutId = window.setTimeout(finishRemove, 180);
    return () => window.clearTimeout(timeoutId);
  }, [phase, finishRemove]);

  useEffect(() => {
    if (duration <= 0 || paused || phase === 'exit') {
      lastTickRef.current = null;
      return;
    }

    let rafId = 0;
    const tick = (now: number) => {
      if (lastTickRef.current == null) {
        lastTickRef.current = now;
      }
      const delta = now - lastTickRef.current;
      lastTickRef.current = now;
      remainingRef.current = Math.max(0, remainingRef.current - delta);
      setRemainingMs(remainingRef.current);

      if (remainingRef.current <= 0) {
        startExit();
        return;
      }
      rafId = requestAnimationFrame(tick);
    };

    rafId = requestAnimationFrame(tick);
    return () => {
      cancelAnimationFrame(rafId);
      lastTickRef.current = null;
    };
  }, [paused, duration, phase, startExit]);

  const progress = duration > 0 ? Math.max(0, Math.min(1, remainingMs / duration)) : 0;

  return (
    <div
      className={cn(
        'pointer-events-auto w-full overflow-hidden rounded-xl border shadow-[var(--interactive-shadow)]',
        'transition-all duration-200 ease-out',
        styleMap[toast.type],
        phase === 'enter' && 'translate-y-2 opacity-0',
        phase === 'shown' && 'translate-y-0 opacity-100',
        phase === 'exit' && 'translate-y-1 opacity-0',
      )}
      role="alert"
      onPointerEnter={() => setPaused(true)}
      onPointerLeave={() => setPaused(false)}
    >
      <div className="flex w-full items-center gap-2.5 px-3 py-2.5">
        <div className="shrink-0 opacity-90">{iconMap[toast.type]}</div>
        <p className="min-w-0 flex-1 text-sm font-medium leading-snug break-words">
          {toast.message}
        </p>
        <button
          type="button"
          onClick={startExit}
          className="-mr-0.5 shrink-0 rounded-md p-1 opacity-60 transition-opacity hover:opacity-100 active:opacity-100"
          aria-label={t('common.close')}
        >
          <X className="h-3.5 w-3.5" />
        </button>
      </div>
      {duration > 0 ? (
        <div className={cn('h-0.5 w-full', progressTrackMap[toast.type])} aria-hidden>
          <div
            className={cn('h-full origin-left', progressFillMap[toast.type])}
            style={{ width: `${progress * 100}%` }}
          />
        </div>
      ) : null}
    </div>
  );
}
