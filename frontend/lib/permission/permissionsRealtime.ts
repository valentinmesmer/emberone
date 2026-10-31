/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

type PermissionsChangedListener = () => void;

const listeners = new Set<PermissionsChangedListener>();

export function subscribePermissionsChanged(listener: PermissionsChangedListener): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function notifyPermissionsChanged(): void {
  listeners.forEach((listener) => {
    listener();
  });
}
