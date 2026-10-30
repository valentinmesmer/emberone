/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { authorizedFetch } from '@/lib/api/authorizedFetch';
import { getRuntimeConfig } from '@/lib/runtimeConfig';
import type { EntityAttachmentDto } from '@/types/attachment';

async function apiBase(): Promise<string> {
  const cfg = await getRuntimeConfig();
  return cfg.NEXT_PUBLIC_API_BASE_URL || '';
}

function attachmentsUrl(
  organizationId: number,
  appointmentId: number,
  suffix = '',
): Promise<string> {
  return apiBase().then(
    (base) =>
      `${base}/api/v1/auth/organizations/${organizationId}/appointments/${appointmentId}/attachments${suffix}`,
  );
}

export const appointmentAttachmentApi = {
  async list(organizationId: number, appointmentId: number): Promise<EntityAttachmentDto[]> {
    const response = await authorizedFetch(await attachmentsUrl(organizationId, appointmentId));
    if (!response.ok) throw new Error('Failed to load attachments');
    return response.json();
  },

  async upload(
    organizationId: number,
    appointmentId: number,
    file: File,
  ): Promise<EntityAttachmentDto> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await authorizedFetch(await attachmentsUrl(organizationId, appointmentId), {
      method: 'POST',
      body: formData,
    });
    if (!response.ok) throw new Error('Failed to upload attachment');
    return response.json();
  },

  async download(
    organizationId: number,
    appointmentId: number,
    attachmentId: number,
  ): Promise<Blob> {
    const response = await authorizedFetch(
      await attachmentsUrl(organizationId, appointmentId, `/${attachmentId}/download`),
    );
    if (!response.ok) throw new Error('Failed to download attachment');
    return response.blob();
  },

  async delete(organizationId: number, appointmentId: number, attachmentId: number): Promise<void> {
    const response = await authorizedFetch(
      await attachmentsUrl(organizationId, appointmentId, `/${attachmentId}`),
      { method: 'DELETE' },
    );
    if (!response.ok) throw new Error('Failed to delete attachment');
  },
};
