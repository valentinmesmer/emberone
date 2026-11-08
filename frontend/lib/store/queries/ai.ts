/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useQuery } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import { aiApi } from '@/lib/api/ai';
import { useAppMutation } from '@/lib/store/useAppMutation';
import type { AiExtractionTarget, AiProofType, HelpChatMessageDto } from '@/types/ai';

export const aiKeys = {
  all: ['ai'] as const,
  capabilities: () => [...aiKeys.all, 'capabilities'] as const,
};

/** Which AI functions the backend offers to this user; everything is off by default. */
export function useAiCapabilities() {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: aiKeys.capabilities(),
    queryFn: () => aiApi.getCapabilities(),
    enabled: hasEmberOneUserId,
    staleTime: 5 * 60 * 1000,
    // A failing capabilities call must never break the app: AI simply stays hidden.
    retry: false,
  });
}

/** Whether "fill from document" may be offered for a proof type. */
export function useProofExtractionAvailable(proofType: AiProofType): boolean {
  const { data } = useAiCapabilities();
  const type = data?.documentExtraction.types[proofType];
  return Boolean(data?.documentExtraction.available && type?.available);
}

export function useHelpChat() {
  return useAppMutation({
    mutationFn: (messages: HelpChatMessageDto[]) => aiApi.helpChat(messages),
    // The chat shows its own inline error; the answer is never cached.
    meta: { showErrorToast: false },
  });
}

export function useExtractProofFromDocument(proofType: AiProofType, target?: AiExtractionTarget) {
  return useAppMutation({
    mutationFn: (variables: { files: File[]; selectionId?: number | null }) =>
      aiApi.extractFromDocument(proofType, variables.files, variables.selectionId, target),
  });
}
