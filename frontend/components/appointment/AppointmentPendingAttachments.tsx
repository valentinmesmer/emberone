/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { PendingAttachments } from '@/components/attachment';

interface AppointmentPendingAttachmentsProps {
  files: File[];
  onChange: (files: File[]) => void;
}

export function AppointmentPendingAttachments({
  files,
  onChange,
}: AppointmentPendingAttachmentsProps) {
  return <PendingAttachments files={files} onChange={onChange} variant="section" />;
}
