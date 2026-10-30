/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
