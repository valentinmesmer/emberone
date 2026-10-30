/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Loader2 } from 'lucide-react';
import { SourceFooter } from '@/components/legal/SourceFooter';
import { useTranslation } from '@/hooks/useTranslation';

export interface LoadingStateProps {
  message?: string;
  subMessage?: string;
  size?: 'sm' | 'md' | 'lg';
  variant?: 'fullscreen' | 'embedded';
  /** When false, only text (e.g. static “not available” without a spinner). Default true. */
  showSpinner?: boolean;
}

export function LoadingState({
  message,
  subMessage,
  size = 'md',
  variant = 'fullscreen',
  showSpinner = true,
}: LoadingStateProps) {
  const t = useTranslation();

  const displayMessage = message ?? t('common.loading');
  const displaySubMessage = subMessage ?? t('common.wait');

  const sizeClasses = {
    sm: {
      spinner: 'h-5 w-5',
      message: 'text-sm',
      subMessage: 'text-xs',
      gap: 'space-y-3',
    },
    md: {
      spinner: 'h-8 w-8',
      message: 'text-base',
      subMessage: 'text-sm',
      gap: 'space-y-4',
    },
    lg: {
      spinner: 'h-12 w-12',
      message: 'text-lg',
      subMessage: 'text-base',
      gap: 'space-y-5',
    },
  };

  const classes = sizeClasses[size];

  const isFullscreen = variant === 'fullscreen';

  return (
    <div
      className={
        isFullscreen
          ? 'flex min-h-screen flex-col bg-[var(--page-bg)]'
          : 'flex min-h-[220px] items-center justify-center rounded-xl border border-[var(--border-color)] bg-[var(--card-bg)] p-4'
      }
    >
      <div
        className={
          isFullscreen
            ? `flex flex-1 flex-col items-center justify-center ${classes.gap} px-6`
            : `text-center ${classes.gap}`
        }
      >
        {showSpinner && (
          <div className="flex justify-center">
            <Loader2
              className={`${classes.spinner} text-[var(--fire-red)] animate-spin`}
              strokeWidth={2.5}
            />
          </div>
        )}

        {(displayMessage || displaySubMessage) && (
          <div className="space-y-2 text-center">
            {displayMessage && (
              <p className={`${classes.message} font-medium text-[var(--text-primary)]`}>
                {displayMessage}
              </p>
            )}
            {displaySubMessage && (
              <p className={`${classes.subMessage} text-[var(--text-secondary)]`}>
                {displaySubMessage}
              </p>
            )}
          </div>
        )}
      </div>
      {isFullscreen ? (
        <div className="shrink-0 px-6 pb-[max(1.25rem,env(safe-area-inset-bottom,0px))] text-center">
          <SourceFooter />
        </div>
      ) : null}
    </div>
  );
}
