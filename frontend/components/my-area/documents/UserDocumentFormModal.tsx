/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useState } from 'react';
import { FileInput } from '@/components/ui/file-input';
import { Input } from '@/components/ui/input';
import { Modal } from '@/components/ui/modal';
import { ModalFooter } from '@/components/ui/modal-footer';
import { useTranslation } from '@/hooks/useTranslation';
import type { UploadUserDocumentParams, UserDocumentDto } from '@/types/userDocument';
import { DocumentTagInput } from './DocumentTagInput';

type FormValues = {
  file?: File;
  title: string;
  description: string;
  tags: string[];
};

interface UserDocumentFormModalProps {
  isOpen: boolean;
  mode: 'upload' | 'edit';
  initialDocument?: UserDocumentDto;
  tagSuggestions?: string[];
  isLoading?: boolean;
  onClose: () => void;
  onSubmit: (values: UploadUserDocumentParams) => Promise<void>;
}

function titleFromFileName(fileName: string): string {
  const lastDot = fileName.lastIndexOf('.');
  if (lastDot <= 0) return fileName;
  return fileName.slice(0, lastDot);
}

function buildInitialValues(
  mode: 'upload' | 'edit',
  initialDocument?: UserDocumentDto,
): FormValues {
  if (mode === 'edit' && initialDocument) {
    return {
      title: initialDocument.title,
      description: initialDocument.description ?? '',
      tags: [...initialDocument.tags],
    };
  }
  return { title: '', description: '', tags: [] };
}

function UserDocumentFormBody({
  mode,
  initialDocument,
  tagSuggestions = [],
  isLoading,
  onClose,
  onSubmit,
}: Omit<UserDocumentFormModalProps, 'isOpen'>) {
  const t = useTranslation();
  const [values, setValues] = useState<FormValues>(() => buildInitialValues(mode, initialDocument));

  const handleSubmit = async () => {
    if (mode === 'upload' && !values.file) {
      return;
    }
    await onSubmit({
      file: values.file as File,
      title: values.title.trim() || undefined,
      description: values.description.trim() || undefined,
      tags: values.tags,
    });
  };

  return (
    <div className="space-y-4">
      {mode === 'upload' ? (
        <FileInput
          label={t('myArea.documents.fileLabel')}
          files={values.file ? [values.file] : []}
          onChange={(selected) => {
            const file = selected[0];
            setValues((prev) => ({
              ...prev,
              file,
              title: prev.title || (file ? titleFromFileName(file.name) : ''),
            }));
          }}
          disabled={isLoading}
          required
        />
      ) : null}

      <Input
        label={t('myArea.documents.titleLabel')}
        value={values.title}
        onChange={(event) => setValues((prev) => ({ ...prev, title: event.target.value }))}
      />

      <Input
        label={t('myArea.documents.descriptionLabel')}
        value={values.description}
        onChange={(event) => setValues((prev) => ({ ...prev, description: event.target.value }))}
      />

      <DocumentTagInput
        tags={values.tags}
        suggestions={tagSuggestions}
        onChange={(tags) => setValues((prev) => ({ ...prev, tags }))}
        disabled={isLoading}
      />

      <div className="flex justify-end pt-2">
        <ModalFooter
          onCancel={onClose}
          onSave={() => void handleSubmit()}
          isLoading={isLoading}
          isValid={mode !== 'upload' || !!values.file}
          saveText={mode === 'upload' ? t('myArea.documents.upload') : t('common.save')}
        />
      </div>
    </div>
  );
}

export function UserDocumentFormModal({
  isOpen,
  mode,
  initialDocument,
  tagSuggestions = [],
  isLoading = false,
  onClose,
  onSubmit,
}: UserDocumentFormModalProps) {
  const t = useTranslation();
  const title =
    mode === 'upload' ? t('myArea.documents.uploadTitle') : t('myArea.documents.editTitle');
  const formKey = mode === 'edit' && initialDocument ? `edit-${initialDocument.id}` : 'upload';

  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title}>
      {isOpen ? (
        <UserDocumentFormBody
          key={formKey}
          mode={mode}
          initialDocument={initialDocument}
          tagSuggestions={tagSuggestions}
          isLoading={isLoading}
          onClose={onClose}
          onSubmit={onSubmit}
        />
      ) : null}
    </Modal>
  );
}
