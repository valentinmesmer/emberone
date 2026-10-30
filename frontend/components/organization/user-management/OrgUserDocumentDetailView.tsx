/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Download, Eye, FolderOpen, Pencil } from 'lucide-react';
import { useState } from 'react';
import { UserDocumentFormModal } from '@/components/my-area/documents/UserDocumentFormModal';
import { AppLinkNavigateButton } from '@/components/ui/app-link-navigate-button';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { DetailCardAction, DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { FormLabel } from '@/components/ui/form-label';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useViewAttachment } from '@/hooks/useViewAttachment';
import { userDocumentApi } from '@/lib/api/userDocument';
import { collectUserDocumentTags } from '@/lib/my-area/userDocumentTags';
import { usePermissions } from '@/lib/permission/PermissionContext';
import { useOrganizationUserDocuments, useUpdateOrganizationUserDocument } from '@/lib/store';
import { downloadBlob } from '@/lib/utils/downloadBlob';
import { resolveGatedAppNavigationHref } from '@/lib/utils/sourceLink';
import { formatFileSize, formatUserDocumentTag } from '@/lib/utils/userDocumentFormat';
import { isViewableAttachmentMediaType } from '@/lib/utils/viewableMediaTypes';

export interface OrgUserDocumentDetailViewProps {
  organizationId: number;
  userId: number;
  documentId: number;
  canWrite: boolean;
}

export function OrgUserDocumentDetailView({
  organizationId,
  userId,
  documentId,
  canWrite,
}: OrgUserDocumentDetailViewProps) {
  const t = useTranslation();
  const { hasPermission } = usePermissions();
  const { showError, showSuccess } = useToast();
  const { data: documents = [], isLoading } = useOrganizationUserDocuments(organizationId, userId);
  const navigationContext = { scope: 'org' as const, organizationId, userId };
  const updateDocument = useUpdateOrganizationUserDocument(organizationId, userId);

  const [isEditOpen, setIsEditOpen] = useState(false);
  const [downloading, setDownloading] = useState(false);
  const { view, viewingKey } = useViewAttachment();

  const doc = documents.find((d) => d.id === documentId);
  const viewing = viewingKey === String(documentId);
  const availableTags = collectUserDocumentTags(documents);

  const handleDownload = async () => {
    if (!doc) return;
    setDownloading(true);
    try {
      const { blob, fileName } = await userDocumentApi.downloadForOrganizationUser(
        organizationId,
        userId,
        doc.id,
        doc.fileName,
      );
      downloadBlob(blob, fileName);
    } catch (err) {
      showError(err instanceof Error ? err.message : t('myArea.documents.downloadFailed'));
    } finally {
      setDownloading(false);
    }
  };

  const handleView = async () => {
    if (!doc) return;
    await view(String(doc.id), (targetWindow) =>
      userDocumentApi.viewForOrganizationUser(
        organizationId,
        userId,
        doc.id,
        targetWindow,
        doc.fileName,
      ),
    );
  };

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!doc) {
    return <NotFoundState message={t('myArea.documents.error.notFound')} />;
  }

  return (
    <div className="w-full space-y-4">
      <Card className="p-3 sm:p-4">
        <DetailCardHeader
          className="mb-4"
          eyebrow={
            <span className="inline-flex items-center gap-2 normal-case">
              <FolderOpen className="h-4 w-4 shrink-0" aria-hidden />
              {t('userManagement.sections.documents')}
            </span>
          }
          title={doc.title}
          actions={
            <>
              {isViewableAttachmentMediaType(doc.contentType) ? (
                <DetailCardAction
                  label={t('myArea.documents.view')}
                  icon={<Eye className="h-4 w-4" aria-hidden />}
                  onClick={() => void handleView()}
                  disabled={viewing}
                />
              ) : null}
              <DetailCardAction
                label={t('common.attachments.download')}
                icon={<Download className="h-4 w-4" aria-hidden />}
                onClick={() => void handleDownload()}
                disabled={downloading}
              />
              {canWrite ? (
                <DetailCardAction
                  label={t('myArea.documents.editTitle')}
                  icon={<Pencil className="h-4 w-4" aria-hidden />}
                  onClick={() => setIsEditOpen(true)}
                />
              ) : null}
            </>
          }
        />

        <div className="space-y-4">
          {doc.description?.trim() ? (
            <FieldDisplay label={t('myArea.documents.descriptionLabel')} value={doc.description} />
          ) : null}

          <div>
            <FieldDisplay label={t('myArea.documents.fileLabel')} value={doc.fileName} />
            <p className="mt-0.5 text-xs tabular-nums text-[var(--text-secondary)]">
              {formatFileSize(doc.sizeBytes)}
            </p>
          </div>

          {doc.tags.length > 0 ? (
            <div>
              <FormLabel>{t('myArea.documents.tagsLabel')}</FormLabel>
              <div className="mt-1 flex flex-wrap gap-1">
                {doc.tags.map((tag) => (
                  <Badge key={tag} tone="neutral">
                    {formatUserDocumentTag(tag, t)}
                  </Badge>
                ))}
              </div>
            </div>
          ) : null}
        </div>

        {doc.links.some(
          (link) => resolveGatedAppNavigationHref(link, navigationContext, hasPermission) != null,
        ) ? (
          <div className="mt-4 border-t border-[var(--border-color)] pt-4">
            <div className="flex flex-wrap gap-2">
              {doc.links.map((link) => {
                const href = resolveGatedAppNavigationHref(link, navigationContext, hasPermission);
                return href ? (
                  <AppLinkNavigateButton key={link.id} href={href} link={link} />
                ) : null;
              })}
            </div>
          </div>
        ) : null}
      </Card>

      {canWrite ? (
        <UserDocumentFormModal
          isOpen={isEditOpen}
          mode="edit"
          initialDocument={doc}
          tagSuggestions={availableTags}
          onClose={() => setIsEditOpen(false)}
          isLoading={updateDocument.isPending}
          onSubmit={async (values) => {
            await updateDocument.mutateAsync({
              documentId: doc.id,
              data: {
                title: values.title,
                description: values.description,
                tags: values.tags,
              },
            });
            showSuccess(t('myArea.documents.updateSuccess'));
            setIsEditOpen(false);
          }}
        />
      ) : null}
    </div>
  );
}
