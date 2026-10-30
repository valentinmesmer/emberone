/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useCallback, useState } from 'react';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  readOnboardingAttachmentFileNames,
  readRecordAttachmentFileNamesFromDetail,
  uploadOnboardingPendingFiles,
  withOnboardingAttachmentFileNames,
} from '@/lib/onboarding/attachmentUtils';
import {
  useDeleteOnboardingRecordAttachment,
  useUploadOnboardingRecordAttachment,
} from '@/lib/store/queries/onboardingInvitation';
import type { OnboardingInvitationAccess } from '@/types/onboardingInvitation';

type OnboardingRecordAttachmentStepType = 'CERTIFICATIONS' | 'TRAINING_COURSES';

type OnboardingRecordAttachmentDraft = {
  recordKey: string;
  fileName?: string | null;
  fileNames?: string[];
  uploaded?: boolean;
};

interface UseOnboardingRecordAttachmentsOptions<T extends OnboardingRecordAttachmentDraft> {
  stepType: OnboardingRecordAttachmentStepType;
  access: OnboardingInvitationAccess;
  recordKey: string;
  draft: T;
  editable: boolean;
  onFileNamesChange: (fileNames: string[]) => void;
}

export function useOnboardingRecordAttachments<T extends OnboardingRecordAttachmentDraft>({
  stepType,
  access,
  recordKey,
  draft,
  editable,
  onFileNamesChange,
}: UseOnboardingRecordAttachmentsOptions<T>) {
  const t = useTranslation();
  const { showError } = useToast();
  const uploadAttachment = useUploadOnboardingRecordAttachment(access);
  const deleteAttachment = useDeleteOnboardingRecordAttachment(access);

  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [removingIndex, setRemovingIndex] = useState<number | null>(null);

  const handleRemoveStored = useCallback(
    async (index: number) => {
      if (!editable || removingIndex != null) return;
      setRemovingIndex(index);
      try {
        const detail = await deleteAttachment.mutateAsync({
          stepType,
          recordKey,
          attachmentIndex: index,
        });
        onFileNamesChange(readRecordAttachmentFileNamesFromDetail(detail, stepType, recordKey));
      } catch (error) {
        showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
      } finally {
        setRemovingIndex(null);
      }
    },
    [
      deleteAttachment,
      editable,
      onFileNamesChange,
      recordKey,
      removingIndex,
      showError,
      stepType,
      t,
    ],
  );

  const uploadPendingFiles = useCallback(async (): Promise<T | null> => {
    if (files.length === 0) return draft;
    setUploading(true);
    try {
      let detail = null;
      await uploadOnboardingPendingFiles(files, async (file) => {
        detail = await uploadAttachment.mutateAsync({
          stepType,
          recordKey,
          file,
        });
      });
      setFiles([]);
      if (!detail) return draft;
      const fileNames = readRecordAttachmentFileNamesFromDetail(detail, stepType, recordKey);
      return withOnboardingAttachmentFileNames(draft, fileNames);
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
      return null;
    } finally {
      setUploading(false);
    }
  }, [draft, files, recordKey, showError, stepType, t, uploadAttachment]);

  return {
    files,
    setFiles,
    uploading,
    removingIndex,
    handleRemoveStored,
    uploadPendingFiles,
    storedFileNames: readOnboardingAttachmentFileNames(draft),
  };
}
