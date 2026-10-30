/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { EntityAttachmentList } from '@/components/attachment';
import { appointmentAttachmentApi } from '@/lib/api/appointmentAttachment';
import { appointmentAttachmentKeys, appointmentKeys } from '@/lib/store';
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
  const queryClient = useQueryClient();
  const attachmentsQuery = useQuery<EntityAttachmentDto[]>({
    queryKey: appointmentAttachmentKeys.list(organizationId, appointmentId),
    queryFn: () => appointmentAttachmentApi.list(organizationId, appointmentId),
  });
  const attachments = attachmentsQuery.data ?? [];
  const loading = attachmentsQuery.isLoading || attachmentsQuery.isFetching;

  const invalidate = async () => {
    await queryClient.invalidateQueries({
      queryKey: appointmentAttachmentKeys.list(organizationId, appointmentId),
    });
    await queryClient.invalidateQueries({ queryKey: appointmentKeys.all });
  };

  const handleUpload = async (file: File) => {
    await appointmentAttachmentApi.upload(organizationId, appointmentId, file);
    await invalidate();
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
    await appointmentAttachmentApi.delete(organizationId, appointmentId, attachmentId);
    await invalidate();
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
