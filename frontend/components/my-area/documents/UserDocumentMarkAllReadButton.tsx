/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CheckCheck } from 'lucide-react';
import { Button, type ButtonProps } from '@/components/ui/button';
import { useTranslation } from '@/hooks/useTranslation';
import { useMarkAllDocumentsViewed, useUnreadDocumentCount } from '@/lib/store';

interface UserDocumentMarkAllReadButtonProps
  extends Pick<ButtonProps, 'variant' | 'size' | 'fullWidth' | 'className'> {
  enabled?: boolean;
}

export function UserDocumentMarkAllReadButton({
  enabled = true,
  variant = 'outline',
  size = 'sm',
  fullWidth = false,
  className,
}: UserDocumentMarkAllReadButtonProps) {
  const t = useTranslation();
  const { data: unreadCountData } = useUnreadDocumentCount({ enabled });
  const markAllViewed = useMarkAllDocumentsViewed();
  const unreadCount = unreadCountData?.count ?? 0;
  const label = markAllViewed.isPending
    ? t('myArea.documents.markAllReadLoading')
    : t('myArea.documents.markAllRead');

  if (!enabled || (unreadCount === 0 && !markAllViewed.isPending)) {
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
      onClick={() => markAllViewed.mutate()}
      isLoading={markAllViewed.isPending}
      aria-label={label}
      title={label}
    >
      {label}
    </Button>
  );
}
