/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export interface EntityAttachmentDto {
  id: number;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: number;
}
