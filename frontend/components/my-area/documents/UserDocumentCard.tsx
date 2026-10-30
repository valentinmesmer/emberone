/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Pencil } from 'lucide-react';
import { AppLinkListAction, AppLinkListIndicator } from '@/components/ui/app-link-list-action';
import { IconButton } from '@/components/ui/icon-button';
import { ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { type AppNavigationContext, hasDocumentLinks } from '@/lib/utils/appLink';
import { getFirstGatedNavigableDocumentLink } from '@/lib/utils/sourceLink';
import { formatFileSize } from '@/lib/utils/userDocumentFormat';
import type { UserDocumentDto } from '@/types/userDocument';

interface UserDocumentCardProps {
  doc: UserDocumentDto;
  href: string;
  canWrite: boolean;
  isUnread?: boolean;
  onEdit?: () => void;
  navigationContext?: AppNavigationContext;
}

export function UserDocumentCard({
  doc,
  href,
  canWrite,
  isUnread = false,
  onEdit,
  navigationContext = { scope: 'self' },
}: UserDocumentCardProps) {
  const t = useTranslation();
  const { hasPermission } = usePermissions();
  const hasLink = hasDocumentLinks(doc);
  const navigable = getFirstGatedNavigableDocumentLink(doc.links, navigationContext, hasPermission);
  const linkLabel = t('myArea.documents.linksLabel');

  return (
    <ListRow
      href={href}
      tone={isUnread ? 'active' : 'default'}
      accent={isUnread}
      title={doc.title}
      subtitle={
        <span className="flex min-w-0 flex-wrap items-center gap-x-1.5 gap-y-0.5">
          <span className="truncate">{doc.fileName}</span>
          <span className="shrink-0 text-[var(--text-secondary)]/50" aria-hidden>
            ·
          </span>
          <span className="shrink-0 tabular-nums">{formatFileSize(doc.sizeBytes)}</span>
        </span>
      }
      actions={
        <>
          {navigable ? (
            <AppLinkListAction href={navigable.href} link={navigable.link} />
          ) : hasLink ? (
            <AppLinkListIndicator ariaLabel={linkLabel} />
          ) : null}
          {canWrite ? (
            <IconButton
              variant="action"
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={onEdit}
              aria-label={t('common.edit')}
              title={t('common.edit')}
            />
          ) : null}
        </>
      }
    />
  );
}
