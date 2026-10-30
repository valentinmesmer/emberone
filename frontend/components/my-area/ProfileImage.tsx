/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { Pencil } from 'lucide-react';
import Image from 'next/image';
import { useState } from 'react';
import { IconButton } from '@/components/ui/icon-button';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { useTranslation } from '@/hooks/useTranslation';
import {
  invalidateSelfProfileImage,
  useAuthenticatedProfileImageObjectUrl,
  useSelfProfileImage,
} from '@/lib/store';
import { cn } from '@/lib/utils/cn';
import { EditProfileImageModal } from './EditProfileImageModal';

const profileAvatarClassName = 'h-20 w-20 text-xl';

interface ProfileImageProps {
  firstName?: string;
  lastName?: string;
  onImageUpdate?: () => void;
}

export function ProfileImage({ firstName, lastName, onImageUpdate }: ProfileImageProps) {
  const t = useTranslation();
  const queryClient = useQueryClient();
  const hasEmberOneUserId = useHasEmberOneUserId();
  const { data: downloadUrl = null, isLoading: urlLoading } =
    useSelfProfileImage(hasEmberOneUserId);
  const { objectUrl: imageUrl = null, loading: blobLoading } =
    useAuthenticatedProfileImageObjectUrl(downloadUrl, hasEmberOneUserId);
  const isLoading = urlLoading || blobLoading;
  const [isModalOpen, setIsModalOpen] = useState(false);

  const handleImageUpdate = () => {
    invalidateSelfProfileImage(queryClient);
    if (onImageUpdate) {
      onImageUpdate();
    }
  };

  return (
    <>
      <div className="relative flex-shrink-0">
        <div className="relative">
          {isLoading ? (
            <div
              className={cn(
                'animate-pulse rounded-full bg-[var(--hover-bg)]',
                profileAvatarClassName,
              )}
              aria-hidden
            />
          ) : imageUrl ? (
            <Image
              src={imageUrl}
              alt={t('myArea.image.alt')}
              width={80}
              height={80}
              className="h-20 w-20 rounded-full object-cover"
              onError={() => invalidateSelfProfileImage(queryClient)}
              unoptimized
            />
          ) : (
            <UserAvatar
              firstName={firstName}
              lastName={lastName}
              className={profileAvatarClassName}
            />
          )}

          {/* Edit button overlay */}
          <div className="absolute -top-1 -right-1">
            <IconButton
              variant="action"
              icon={<Pencil className="h-4 w-4" aria-hidden />}
              onClick={() => setIsModalOpen(true)}
              disabled={isLoading || !hasEmberOneUserId}
              aria-label={t('myArea.image.edit')}
              title={t('myArea.image.edit')}
            />
          </div>
        </div>
      </div>

      <EditProfileImageModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        currentImageUrl={imageUrl}
        firstName={firstName}
        lastName={lastName}
        onImageUpdate={handleImageUpdate}
      />
    </>
  );
}
