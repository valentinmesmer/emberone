/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { EntityAttachmentList } from '@/components/attachment';
import { appointmentAttachmentApi } from '@/lib/api/appointmentAttachment';
import {
  useAppointmentAttachments,
  useDeleteAppointmentAttachment,
  useUploadAppointmentAttachment,
} from '@/lib/store';
import { downloadBlob } from '@/lib/utils/downloadBlob';
import type { EntityAttachmentDto } from '@/types/attachment';

interface AppointmentAttachmentsProps {
  organizationId: number;
  appointmentId: number;
  canManage?: boolean;
  compact?: boolean;
}

export function AppointmentAttachments({
  organizationId,
  appointmentId,
  canManage = false,
  compact = false,
}: AppointmentAttachmentsProps) {
  const attachmentsQuery = useAppointmentAttachments(organizationId, appointmentId);
  const uploadAttachment = useUploadAppointmentAttachment(organizationId, appointmentId);
  const deleteAttachment = useDeleteAppointmentAttachment(organizationId, appointmentId);
  const attachments = attachmentsQuery.data ?? [];
  const loading = attachmentsQuery.isLoading || attachmentsQuery.isFetching;

  const handleUpload = async (file: File) => {
    await uploadAttachment.mutateAsync(file);
  };

  const handleDownload = async (attachment: EntityAttachmentDto) => {
    const blob = await appointmentAttachmentApi.download(
      organizationId,
      appointmentId,
      attachment.id,
    );
    downloadBlob(blob, attachment.fileName);
  };

  const handleDelete = async (attachmentId: number) => {
    await deleteAttachment.mutateAsync(attachmentId);
  };

  return (
    <EntityAttachmentList
      attachments={attachments}
      loading={loading}
      canManage={canManage}
      compact={compact}
      onUpload={handleUpload}
      onDownload={handleDownload}
      onDelete={handleDelete}
    />
  );
}
