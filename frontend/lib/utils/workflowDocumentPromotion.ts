/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type {
  WorkflowDocumentPromotionItemDto,
  WorkflowDocumentPromotionSelection,
  WorkflowMediaSource,
} from '@/types/workflow';

export function workflowPromotionKey(source: WorkflowMediaSource, attachmentId: number): string {
  return `${source}:${attachmentId}`;
}

export function buildWorkflowDocumentPromotionSelections(
  items: WorkflowDocumentPromotionItemDto[],
  selectedKeys: ReadonlySet<string>,
): WorkflowDocumentPromotionSelection[] {
  return items
    .filter((item) => selectedKeys.has(workflowPromotionKey(item.source, item.attachmentId)))
    .map((item) => ({
      attachmentId: item.attachmentId,
      source: item.source,
    }));
}

export function selectedKeysFromPreviewItems(
  items: WorkflowDocumentPromotionItemDto[],
): Set<string> {
  return new Set(
    items
      .filter((item) => item.selected)
      .map((item) => workflowPromotionKey(item.source, item.attachmentId)),
  );
}

export function selectedPromotionItems(
  items: WorkflowDocumentPromotionItemDto[],
  selectedKeys: ReadonlySet<string>,
): WorkflowDocumentPromotionItemDto[] {
  return items.filter((item) =>
    selectedKeys.has(workflowPromotionKey(item.source, item.attachmentId)),
  );
}
