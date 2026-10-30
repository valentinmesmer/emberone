/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { UserCircle } from 'lucide-react';
import Image from 'next/image';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useAuth } from '@/lib/auth/AuthContext';
import { usePermissions } from '@/lib/permission/PermissionContext';
import {
  useAuthenticatedProfileImageObjectUrl,
  useOrgUserProfileImage,
  useSelfProfileImage,
} from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import { plainAvatarSurfaceClassName } from '@/lib/utils/plainIconSurface';
import { getUserIdFromToken } from '@/lib/utils/tokenUserId';
import { PermissionTypeEnum } from '@/types/permission';

function initials(first?: string, last?: string): string {
  const primary = first?.trim().charAt(0) ?? last?.trim().charAt(0) ?? '';
  return primary.toUpperCase();
}

const sizeClasses = {
  sm: 'h-7 w-7 text-xs',
  md: 'h-9 w-9 text-sm',
  lg: 'h-11 w-11 text-sm sm:h-14 sm:w-14 sm:text-base',
} as const;

export interface UserAvatarProps {
  firstName?: string;
  lastName?: string;
  size?: keyof typeof sizeClasses;
  className?: string;
  /** Fetch and show the signed-in user's own uploaded profile picture. Self only. */
  showProfileImage?: boolean;
  /** Organization context for another member's profile picture (`PROFILE_IMAGE_READ`). */
  organizationId?: number;
  /** Target user id when loading an org-scoped profile picture. */
  userId?: number;
}

/**
 * Small circular user avatar: outline initials, falling back to a
 * neutral user icon when no name is available.
 */
export function UserAvatar({
  firstName,
  lastName,
  size = 'sm',
  className,
  showProfileImage = false,
  organizationId,
  userId,
}: UserAvatarProps) {
  const { accessToken } = useAuth();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const { hasPermission } = usePermissions();
  const selfUserId = getUserIdFromToken(accessToken);
  const isSelfTarget = userId != null && selfUserId != null && String(userId) === selfUserId;

  const loadSelf = hasEmberOneUserId && (showProfileImage || isSelfTarget);
  const loadOrg =
    !loadSelf &&
    organizationId != null &&
    userId != null &&
    hasPermission(organizationId, PermissionTypeEnum.PROFILE_IMAGE_READ);

  const { data: selfDownloadUrl = null, isLoading: selfUrlLoading } = useSelfProfileImage(loadSelf);
  const { data: orgDownloadUrl = null, isLoading: orgUrlLoading } = useOrgUserProfileImage(
    organizationId,
    userId,
    loadOrg,
  );

  const {
    objectUrl: selfImageUrl,
    loading: selfBlobLoading,
    error: selfError,
  } = useAuthenticatedProfileImageObjectUrl(selfDownloadUrl, loadSelf);
  const {
    objectUrl: orgImageUrl,
    loading: orgBlobLoading,
    error: orgError,
  } = useAuthenticatedProfileImageObjectUrl(orgDownloadUrl, loadOrg);

  const imageUrl = loadSelf ? selfImageUrl : loadOrg ? orgImageUrl : null;
  const imageLoading = loadSelf
    ? selfUrlLoading || selfBlobLoading
    : loadOrg
      ? orgUrlLoading || orgBlobLoading
      : false;
  const imageError = loadSelf ? selfError : loadOrg ? orgError : false;
  const text = initials(firstName, lastName);

  if ((loadSelf || loadOrg) && imageLoading) {
    return (
      <span
        className={cn(
          'inline-block shrink-0 animate-pulse rounded-full bg-[var(--hover-bg)]',
          sizeClasses[size],
          className,
        )}
        aria-hidden
      />
    );
  }

  if ((loadSelf || loadOrg) && imageUrl && !imageError) {
    return (
      <Image
        src={imageUrl}
        alt=""
        width={96}
        height={96}
        className={cn('shrink-0 rounded-full object-cover', sizeClasses[size], className)}
        unoptimized
      />
    );
  }

  return (
    <span
      className={cn(
        plainAvatarSurfaceClassName,
        'shrink-0 rounded-full leading-none',
        sizeClasses[size],
        className,
      )}
      aria-hidden
    >
      {text || <UserCircle className="h-4 w-4" aria-hidden />}
    </span>
  );
}
