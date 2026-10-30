/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { FileText } from 'lucide-react';
import { useMemo } from 'react';
import { Badge } from '@/components/ui/badge';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import {
  selectedPromotionItems,
  workflowPromotionKey,
} from '@/lib/utils/workflowDocumentPromotion';
import type { WorkflowDocumentPromotionItemDto } from '@/types/workflow';

interface WorkflowApproveConfirmDialogProps {
  isOpen: boolean;
  isLoading: boolean;
  items: WorkflowDocumentPromotionItemDto[];
  selectedKeys: ReadonlySet<string>;
  onClose: () => void;
  onConfirm: () => void;
}

export function WorkflowApproveConfirmDialog({
  isOpen,
  isLoading,
  items,
  selectedKeys,
  onClose,
  onConfirm,
}: WorkflowApproveConfirmDialogProps) {
  const t = useTranslation();

  const selectedItems = useMemo(
    () => selectedPromotionItems(items, selectedKeys),
    [items, selectedKeys],
  );

  if (items.length === 0) {
    return (
      <ConfirmDialog
        isOpen={isOpen}
        onClose={onClose}
        onConfirm={onConfirm}
        title={t('myArea.workflowDetail.approveConfirmTitle')}
        message={t('myArea.workflowDetail.approveConfirmMessage')}
        confirmText={t('myArea.workflowDetail.approve')}
        isLoading={isLoading}
      />
    );
  }

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title={t('myArea.workflowDetail.approveDocumentConfirmTitle')}
      size="md"
      footer={
        <ModalFooter
          onCancel={onClose}
          onSave={onConfirm}
          isLoading={isLoading}
          saveText={t('myArea.workflowDetail.approveDocumentConfirm')}
        />
      }
    >
      <p className="mb-4 text-sm text-[var(--text-secondary)]">
        {t('myArea.workflowDetail.approveDocumentConfirmIntroReadOnly')}
      </p>

      {selectedItems.length === 0 ? (
        <p className="text-sm text-[var(--text-secondary)]">
          {t('myArea.workflowDetail.approveDocumentNoneSelected')}
        </p>
      ) : (
        <ul className="space-y-2">
          {selectedItems.map((item) => {
            const key = workflowPromotionKey(item.source, item.attachmentId);
            const sourceLabel =
              item.source === 'WORKFLOW'
                ? t('myArea.workflowDetail.approveDocumentSourceWorkflow')
                : t('myArea.workflowDetail.approveDocumentSourceComment');

            return (
              <li
                key={key}
                className="rounded-md border border-[var(--toast-success-border)] bg-[var(--status-success-surface)] p-3 text-sm"
              >
                <div className="space-y-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <FileText
                      className="h-3.5 w-3.5 shrink-0 text-[var(--text-secondary)]"
                      aria-hidden
                    />
                    <span className="font-medium text-[var(--text-primary)]">
                      {item.defaultTitle}
                    </span>
                    <Badge tone="neutral" className="py-px text-[10px]">
                      {sourceLabel}
                    </Badge>
                  </div>
                  <p className="truncate text-xs text-[var(--text-secondary)]">
                    {item.attachment.fileName}
                  </p>
                  {item.defaultTags.length > 0 ? (
                    <div className="flex flex-wrap gap-1 pt-0.5">
                      {item.defaultTags.map((tag) => (
                        <Badge key={tag} tone="neutral" className="py-px text-[10px]">
                          {tag}
                        </Badge>
                      ))}
                    </div>
                  ) : null}
                </div>
              </li>
            );
          })}
        </ul>
      )}
    </Modal>
  );
}
