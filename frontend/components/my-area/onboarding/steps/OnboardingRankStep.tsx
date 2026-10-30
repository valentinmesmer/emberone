/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { forwardRef, useCallback, useImperativeHandle, useMemo, useState } from 'react';
import { PendingAttachments } from '@/components/attachment';
import { Select } from '@/components/ui/select';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import type { TenantRankDto } from '@/lib/api/tenantRanks';
import {
  hasOnboardingAttachments,
  readOnboardingAttachmentFileNames,
  uploadOnboardingPendingFiles,
} from '@/lib/onboarding/attachmentUtils';
import { readNoneDeclared, readOnboardingRankId } from '@/lib/onboarding/stepPayloads';
import {
  useDeleteOnboardingRankAttachment,
  useUploadOnboardingRankAttachment,
} from '@/lib/store/queries/onboardingInvitation';
import type { OnboardingInvitationAccess } from '@/types/onboardingInvitation';
import type { OnboardingStepHandle, OnboardingStepProps } from './onboardingStepHandle';
import { payloadsEqual } from './onboardingStepHandle';

interface Props extends OnboardingStepProps {
  access: OnboardingInvitationAccess;
  ranks: TenantRankDto[];
}

const SKIP_VALUE = '';

const buildPayload = (
  tenantRankId: number | null,
  rankMeta: Record<string, unknown> | undefined,
) => ({
  noneDeclared: tenantRankId == null,
  tenantRankId,
  rank: tenantRankId == null ? undefined : rankMeta,
});

export const OnboardingRankStep = forwardRef<OnboardingStepHandle, Props>(
  function OnboardingRankStep({ access, payload, ranks, editable, isSaving }, ref) {
    const t = useTranslation();
    const { showError } = useToast();
    const uploadAttachment = useUploadOnboardingRankAttachment(access);
    const deleteAttachment = useDeleteOnboardingRankAttachment(access);

    const initialRankId = readOnboardingRankId(payload);
    const [tenantRankId, setTenantRankId] = useState(() =>
      initialRankId != null ? String(initialRankId) : SKIP_VALUE,
    );
    const [files, setFiles] = useState<File[]>([]);
    const [uploading, setUploading] = useState(false);
    const [removingIndex, setRemovingIndex] = useState<number | null>(null);

    const rankRecord =
      payload.rank && typeof payload.rank === 'object'
        ? (payload.rank as Record<string, unknown>)
        : undefined;
    const storedFileNames = readOnboardingAttachmentFileNames(rankRecord ?? {});

    const savedSnapshot = useMemo(() => {
      const savedRankId = readOnboardingRankId(payload);
      const savedNone = readNoneDeclared(payload) || savedRankId == null;
      return buildPayload(savedNone ? null : savedRankId, rankRecord);
    }, [payload, rankRecord]);

    const parsedRankId = useMemo(() => {
      if (tenantRankId === SKIP_VALUE) return null;
      const parsed = Number.parseInt(tenantRankId, 10);
      return Number.isFinite(parsed) ? parsed : null;
    }, [tenantRankId]);

    const uploadPendingFiles = useCallback(async () => {
      if (files.length === 0) return true;
      setUploading(true);
      try {
        await uploadOnboardingPendingFiles(files, async (file) => {
          await uploadAttachment.mutateAsync(file);
        });
        setFiles([]);
        return true;
      } catch (error) {
        showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
        return false;
      } finally {
        setUploading(false);
      }
    }, [files, showError, t, uploadAttachment]);

    useImperativeHandle(
      ref,
      () => ({
        getPayload: () =>
          buildPayload(
            parsedRankId,
            parsedRankId == null ? undefined : { attachments: rankRecord?.attachments ?? [] },
          ),
        hasChanges: () =>
          !payloadsEqual(savedSnapshot, buildPayload(parsedRankId, rankRecord)) || files.length > 0,
        prepareSave: uploadPendingFiles,
      }),
      [files.length, parsedRankId, rankRecord, savedSnapshot, uploadPendingFiles],
    );

    const rankOptions = [
      { value: SKIP_VALUE, label: t('onboarding.rank.skip') },
      ...ranks.map((rank) => ({ value: String(rank.id), label: rank.name })),
    ];

    const handleRemoveStored = useCallback(
      async (index: number) => {
        if (!editable || removingIndex != null) return;
        setRemovingIndex(index);
        try {
          await deleteAttachment.mutateAsync(index);
        } catch (error) {
          showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
        } finally {
          setRemovingIndex(null);
        }
      },
      [deleteAttachment, editable, removingIndex, showError, t],
    );

    const showProof = parsedRankId != null;
    const hasAttachment = hasOnboardingAttachments(storedFileNames, files);

    return (
      <div className="space-y-4">
        <p className="text-sm text-[var(--text-secondary)]">{t('onboarding.rank.intro')}</p>
        <Select
          label={t('onboarding.rank.select')}
          value={tenantRankId}
          onChange={(e) => setTenantRankId(e.target.value)}
          options={rankOptions}
          disabled={!editable || isSaving || uploading}
        />
        {showProof ? (
          <PendingAttachments
            variant="field"
            label={t('onboarding.rank.proofLabel')}
            hint={t('onboarding.rank.proofOptional')}
            files={files}
            onChange={setFiles}
            multiple={false}
            disabled={!editable || isSaving || uploading}
            storedFileNames={storedFileNames}
            onRemoveStored={handleRemoveStored}
            optionalLabel={hasAttachment ? undefined : t('common.optional')}
          />
        ) : null}
      </div>
    );
  },
);
