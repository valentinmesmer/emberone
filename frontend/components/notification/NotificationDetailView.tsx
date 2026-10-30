/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useEffect } from 'react';
import { AppLinkNavigateButton } from '@/components/ui/app-link-navigate-button';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useTranslation } from '@/hooks/useTranslation';
import { useMarkNotificationRead, useNotification } from '@/lib/store';

export interface NotificationDetailViewProps {
  notificationId: number;
}

export function NotificationDetailView({ notificationId }: NotificationDetailViewProps) {
  const t = useTranslation();
  const { data: notification, isPending: isLoading } = useNotification(notificationId);
  const markRead = useMarkNotificationRead();
  const title = notification?.title ?? '';
  const body = notification?.body ?? '';
  const href = notification?.navigationPath ?? null;

  useEffect(() => {
    if (!notification?.unread) {
      return;
    }
    markRead.mutate(notification.id);
  }, [markRead, notification?.id, notification?.unread]);

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!notification) {
    return <NotFoundState message={t('myArea.notifications.detail.notFound')} />;
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <div className="mb-4">
          <p className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('myArea.notifications.title')}
          </p>
          <h1 className="mt-1 break-words text-lg font-semibold text-[var(--text-primary)]">
            {title}
          </h1>
        </div>

        <div className="space-y-4">
          {body ? (
            <FieldDisplay label={t('myArea.notifications.detail.message')} value={body} multiline />
          ) : null}
        </div>

        {href ? (
          <div className="mt-4 border-t border-[var(--border-color)] pt-4">
            <AppLinkNavigateButton
              href={href}
              link={notification}
              onNavigate={() => {
                if (notification.unread) {
                  markRead.mutate(notification.id);
                }
              }}
            />
          </div>
        ) : null}
      </Card>
    </div>
  );
}
