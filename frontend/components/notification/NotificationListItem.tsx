/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { AppLinkListAction } from '@/components/ui/app-link-list-action';
import { ListRow } from '@/components/ui/list';
import { getMyAreaNotificationDetailPath } from '@/lib/my-area/myAreaSections';
import { hasAppNavigationLink } from '@/lib/utils/appLink';
import type { NotificationDto } from '@/types/notification';

interface NotificationListItemProps {
  notification: NotificationDto;
  onSelect: (notification: NotificationDto) => void;
  onOpenLink?: (notification: NotificationDto) => void;
}

export function NotificationListItem({
  notification,
  onSelect,
  onOpenLink,
}: NotificationListItemProps) {
  const title = notification.title;
  const unread = notification.unread;
  const hasLink = hasAppNavigationLink(notification);

  return (
    <ListRow
      title={title}
      onClick={() => onSelect(notification)}
      detailHref={getMyAreaNotificationDetailPath(notification.id)}
      tone={unread ? 'active' : 'default'}
      accent={unread}
      compact
      actions={
        hasLink ? (
          <AppLinkListAction
            href={notification.navigationPath}
            link={notification}
            onNavigate={() => onOpenLink?.(notification)}
          />
        ) : undefined
      }
    />
  );
}
