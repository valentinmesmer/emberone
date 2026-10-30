/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CheckCheck } from 'lucide-react';
import { Button, type ButtonProps } from '@/components/ui/button';
import { IconButton } from '@/components/ui/icon-button';
import { useTranslation } from '@/hooks/useTranslation';
import { useMarkAllNotificationsRead, useUnreadNotificationCount } from '@/lib/store';
import { cn } from '@/lib/utils/cn';

interface NotificationMarkAllReadButtonProps
  extends Pick<ButtonProps, 'variant' | 'size' | 'fullWidth' | 'className'> {
  enabled?: boolean;
  /** Icon-only control for tight headers (e.g. notification panel). */
  iconOnly?: boolean;
}

export function NotificationMarkAllReadButton({
  enabled = true,
  variant = 'outline',
  size = 'sm',
  fullWidth = false,
  className,
  iconOnly = false,
}: NotificationMarkAllReadButtonProps) {
  const t = useTranslation();
  const { data: unreadCountData } = useUnreadNotificationCount({ enabled });
  const markAllRead = useMarkAllNotificationsRead();
  const unreadCount = unreadCountData?.count ?? 0;
  const label = markAllRead.isPending
    ? t('notifications.markAllReadLoading')
    : t('notifications.markAllRead');

  if (!enabled || (unreadCount === 0 && !markAllRead.isPending)) {
    return null;
  }

  if (iconOnly) {
    return (
      <IconButton
        icon={<CheckCheck className="h-4 w-4" aria-hidden />}
        variant="ghost"
        size="md"
        onClick={() => markAllRead.mutate()}
        disabled={markAllRead.isPending}
        aria-label={label}
        title={label}
        className={cn('min-h-9 min-w-9 touch-manipulation', className)}
      />
    );
  }

  return (
    <Button
      type="button"
      variant={variant}
      size={size}
      fullWidth={fullWidth}
      className={className}
      icon={<CheckCheck className="h-3.5 w-3.5 shrink-0" aria-hidden />}
      onClick={() => markAllRead.mutate()}
      isLoading={markAllRead.isPending}
      aria-label={label}
      title={label}
    >
      {label}
    </Button>
  );
}
