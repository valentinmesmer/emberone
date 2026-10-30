/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Trash2 } from 'lucide-react';
import Image from 'next/image';
import { useState } from 'react';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FileInput } from '@/components/ui/file-input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useToast } from '@/components/ui/toast';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useTranslation } from '@/hooks/useTranslation';
import { useDeleteProfileImage, useUploadProfileImage } from '@/lib/store';
import { cn } from '@/lib/utils/cn';

const modalAvatarClassName = 'h-48 w-48 text-4xl';

interface EditProfileImageModalProps {
  isOpen: boolean;
  onClose: () => void;
  currentImageUrl: string | null;
  firstName?: string;
  lastName?: string;
  onImageUpdate?: () => void;
}

function EditProfileImageModalForm({
  onClose,
  currentImageUrl,
  firstName,
  lastName,
  onImageUpdate,
}: Omit<EditProfileImageModalProps, 'isOpen'>) {
  const t = useTranslation();
  const uploadProfileImage = useUploadProfileImage();
  const deleteProfileImage = useDeleteProfileImage();
  const { showError, showSuccess } = useToast();
  const saving = uploadProfileImage.isPending || deleteProfileImage.isPending;
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  const handleFileSelect = (files: File[]) => {
    const file = files[0];
    if (!file) {
      setSelectedFile(null);
      setPreviewUrl(null);
      return;
    }

    if (!file.type.startsWith('image/')) {
      showError(t('myArea.image.error.invalidType'));
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      showError(t('myArea.image.error.fileTooLarge'));
      return;
    }

    setSelectedFile(file);

    const reader = new FileReader();
    reader.onloadend = () => {
      setPreviewUrl(reader.result as string);
    };
    reader.readAsDataURL(file);
  };

  const handleUpload = async () => {
    if (!selectedFile) return;

    try {
      await uploadProfileImage.mutateAsync(selectedFile);
      showSuccess(t('myArea.image.success.uploaded'));
      if (onImageUpdate) {
        onImageUpdate();
      }
      onClose();
    } catch (err) {
      console.error('Error uploading image:', err);
      showError(err instanceof Error ? err.message : t('myArea.image.error.uploadFailed'));
    }
  };

  const handleDeleteClick = () => {
    setShowDeleteConfirm(true);
  };

  const handleDeleteConfirm = async () => {
    try {
      setShowDeleteConfirm(false);
      await deleteProfileImage.mutateAsync();
      showSuccess(t('myArea.image.success.deleted'));
      if (onImageUpdate) {
        onImageUpdate();
      }
      onClose();
    } catch (err) {
      console.error('Error deleting image:', err);
      showError(err instanceof Error ? err.message : t('myArea.image.error.deleteFailed'));
    }
  };

  const displayImageUrl = previewUrl || currentImageUrl;

  return (
    <Modal
      isOpen
      onClose={onClose}
      title={t('myArea.edit.image')}
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={handleUpload}
          isValid={!!selectedFile}
          isLoading={saving}
        />
      }
    >
      <div className="space-y-6">
        {/* Image Preview */}
        <div className="flex flex-col items-center justify-center">
          <div className="relative">
            {displayImageUrl ? (
              <Image
                src={displayImageUrl}
                alt={t('myArea.image.alt')}
                width={192}
                height={192}
                className="h-48 w-48 rounded-full object-cover"
                unoptimized
              />
            ) : (
              <UserAvatar
                firstName={firstName}
                lastName={lastName}
                className={modalAvatarClassName}
              />
            )}
            {previewUrl && (
              <div className="absolute inset-0 rounded-full bg-black/50 flex items-center justify-center">
                <span className="text-white text-sm font-medium px-3 py-1 bg-black/70 rounded-full">
                  {t('myArea.image.preview')}
                </span>
              </div>
            )}
            {currentImageUrl && (
              <button
                type="button"
                onClick={handleDeleteClick}
                disabled={saving}
                aria-label={t('myArea.image.delete')}
                title={t('myArea.image.delete')}
                className={cn(
                  'absolute bottom-1 right-1 z-10 flex h-9 w-9 items-center justify-center rounded-full',
                  'border border-[var(--border-color)] bg-[var(--card-bg)] text-[var(--fire-red)]',
                  'transition-colors duration-200',
                  'hover:bg-[var(--fire-red)] hover:text-white hover:border-[var(--fire-red)]',
                  'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-[var(--focus-ring)] focus-visible:ring-offset-2 focus-visible:ring-offset-[var(--card-bg)]',
                  'disabled:pointer-events-none disabled:opacity-50',
                )}
              >
                <Trash2 className="h-4 w-4" aria-hidden strokeWidth={2.25} />
              </button>
            )}
          </div>
        </div>

        <FileInput
          label={t('myArea.image.selectImage')}
          hint={t('myArea.image.fileRequirements')}
          accept="image/*"
          files={selectedFile ? [selectedFile] : []}
          onChange={handleFileSelect}
          chooseLabel={t('common.attachments.chooseFile')}
          disabled={saving}
        />
      </div>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDeleteConfirm}
        message={t('myArea.image.confirmDelete')}
        confirmText={t('myArea.image.delete')}
        cancelText={t('common.cancel')}
        variant="danger"
        isLoading={saving}
      />
    </Modal>
  );
}

export function EditProfileImageModal({
  isOpen,
  onClose,
  currentImageUrl,
  firstName,
  lastName,
  onImageUpdate,
}: EditProfileImageModalProps) {
  if (!isOpen) {
    return null;
  }

  return (
    <EditProfileImageModalForm
      key={currentImageUrl ?? 'no-image'}
      onClose={onClose}
      currentImageUrl={currentImageUrl}
      firstName={firstName}
      lastName={lastName}
      onImageUpdate={onImageUpdate}
    />
  );
}
