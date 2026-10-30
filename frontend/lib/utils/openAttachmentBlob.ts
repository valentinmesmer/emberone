/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { downloadBlob } from './downloadBlob';
import { isViewableAttachmentMediaType } from './viewableMediaTypes';

/**
 * Opens a downloaded blob in an already-opened tab for inline viewing.
 * Non-viewable types close the tab and trigger a download instead.
 */
export function openAttachmentBlob(blob: Blob, fileName: string, targetWindow: Window): void {
  if (!isViewableAttachmentMediaType(blob.type)) {
    try {
      targetWindow.close();
    } catch {
      // ignore
    }
    downloadBlob(blob, fileName);
    return;
  }
  const url = URL.createObjectURL(blob);
  if (targetWindow.closed) {
    URL.revokeObjectURL(url);
    throw new Error('Popup closed');
  }
  targetWindow.opener = null;
  targetWindow.location.href = url;
  window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
}
