/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { ChevronLeft, X } from 'lucide-react';
import type React from 'react';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { IconButton } from '@/components/ui/icon-button';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  closeOnBackdropClick?: boolean;
  closeOnEscape?: boolean;
  className?: string;
  bodyClassName?: string;
  /** When set, shows a back control in the header (e.g. nested steps inside the modal). */
  onBack?: () => void;
}

const sizeClasses = {
  sm: 'max-w-md',
  md: 'max-w-lg',
  lg: 'max-w-2xl',
  xl: 'max-w-4xl',
  '2xl': 'max-w-6xl',
};

/** Avoid layout shift when locking body scroll: scrollbar disappears and content would jump sideways. */
let bodyScrollLockCount = 0;

function lockBodyScroll() {
  if (bodyScrollLockCount === 0) {
    const scrollbarWidth = window.innerWidth - document.documentElement.clientWidth;
    document.body.style.overflow = 'hidden';
    if (scrollbarWidth > 0) {
      document.body.style.paddingRight = `${scrollbarWidth}px`;
    }
  }
  bodyScrollLockCount += 1;
}

function unlockBodyScroll() {
  bodyScrollLockCount = Math.max(0, bodyScrollLockCount - 1);
  if (bodyScrollLockCount === 0) {
    document.body.style.overflow = '';
    document.body.style.paddingRight = '';
  }
}

export function Modal({
  isOpen,
  onClose,
  title,
  children,
  footer,
  size = 'lg',
  closeOnBackdropClick = true,
  closeOnEscape = true,
  className,
  bodyClassName,
  onBack,
}: ModalProps) {
  const t = useTranslation();

  useEffect(() => {
    if (!isOpen || !closeOnEscape) return;

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [isOpen, closeOnEscape, onClose]);

  useEffect(() => {
    if (!isOpen) return undefined;
    lockBodyScroll();
    return () => {
      unlockBodyScroll();
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent<HTMLDivElement>) => {
    if (closeOnBackdropClick && e.target === e.currentTarget) {
      onClose();
    }
  };

  const modalContent = (
    <div
      className="fixed inset-0 z-[100] flex items-stretch justify-center bg-black/50 backdrop-blur-sm p-0 sm:items-center sm:p-4"
      role="dialog"
      aria-modal="true"
      onClick={handleBackdropClick}
      onKeyDown={(e) => {
        if (e.key === 'Escape') onClose();
      }}
      style={{
        WebkitTransform: 'translateZ(0)',
        transform: 'translateZ(0)',
      }}
    >
      <div
        className={cn(
          /* No overflow-hidden: HoverHint tooltips in the footer must not be clipped horizontally or covered by the scroll layer. */
          'relative flex w-full flex-col bg-[var(--card-bg)] shadow-2xl',
          'max-sm:min-h-[100dvh] max-sm:max-h-[100dvh] max-sm:rounded-none',
          'sm:max-h-[90vh] sm:rounded-2xl',
          sizeClasses[size],
          className,
        )}
      >
        <div className="flex shrink-0 items-center justify-between gap-2 border-b border-[var(--border-color)] px-4 py-3 sm:px-6 sm:py-4">
          <div className="flex min-w-0 flex-1 items-center gap-1 sm:gap-2">
            {onBack ? (
              <IconButton
                icon={<ChevronLeft />}
                onClick={onBack}
                aria-label={t('common.back')}
                variant="ghost"
                size="md"
                className="shrink-0"
              />
            ) : null}
            {title ? (
              <h2
                className={cn(
                  'flex min-w-0 items-center gap-2 font-bold text-[var(--text-primary)]',
                  onBack ? 'truncate text-base sm:text-lg' : 'text-lg sm:text-2xl',
                )}
              >
                {title}
              </h2>
            ) : null}
          </div>
          <IconButton
            icon={<X />}
            onClick={onClose}
            aria-label={t('common.close')}
            variant="ghost"
            size="md"
            className="shrink-0"
          />
        </div>
        <div
          className={cn(
            'min-h-0 flex-1 overflow-y-auto overflow-x-hidden px-4 py-3 sm:px-6 sm:py-4',
            bodyClassName,
          )}
          style={{
            WebkitOverflowScrolling: 'touch',
            position: 'relative',
            zIndex: 1,
            scrollbarWidth: 'none',
            msOverflowStyle: 'none',
          }}
        >
          {children}
        </div>
        {footer && (
          <div
            className={cn(
              'relative z-[5] flex shrink-0 flex-col-reverse gap-2 border-t border-[var(--border-color)] px-4 py-3',
              'sm:flex-row sm:items-center sm:justify-end sm:px-6 sm:py-4',
              /* Raw Button children (HoverHint wrappers): full-width stacked on mobile, auto-width row on sm+ */
              '[&>*]:w-full sm:[&>*]:w-auto',
            )}
          >
            {footer}
          </div>
        )}
      </div>
    </div>
  );

  return createPortal(
    modalContent,
    typeof document !== 'undefined' ? document.body : ({} as HTMLElement),
  );
}
