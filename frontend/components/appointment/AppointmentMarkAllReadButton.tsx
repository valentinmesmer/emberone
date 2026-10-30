/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CheckCheck } from 'lucide-react';
import { Button, type ButtonProps } from '@/components/ui/button';
import { useTranslation } from '@/hooks/useTranslation';
import { useAppointmentUnreadCount, useMarkAllAppointmentsRead } from '@/lib/store';

interface AppointmentMarkAllReadButtonProps
  extends Pick<ButtonProps, 'variant' | 'size' | 'fullWidth' | 'className'> {
  organizationId: number;
  enabled?: boolean;
}

export function AppointmentMarkAllReadButton({
  organizationId,
  enabled = true,
  variant = 'outline',
  size = 'sm',
  fullWidth = false,
  className,
}: AppointmentMarkAllReadButtonProps) {
  const t = useTranslation();
  const { data: unreadCountData } = useAppointmentUnreadCount(organizationId, { enabled });
  const markAllRead = useMarkAllAppointmentsRead(organizationId);
  const unreadCount = unreadCountData?.count ?? 0;
  const label = markAllRead.isPending
    ? t('appointments.markAllReadLoading')
    : t('appointments.markAllRead');

  if (!enabled || (unreadCount === 0 && !markAllRead.isPending)) {
    return null;
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
