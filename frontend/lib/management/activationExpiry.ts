/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export type ManagementActivationExpiredEvent = {
  activationId: number;
  managingOrganizationId: number;
  targetOrganizationId: number;
};

type ManagementActivationExpiredListener = (event: ManagementActivationExpiredEvent) => void;

const listeners = new Set<ManagementActivationExpiredListener>();

export function onManagementActivationExpired(
  listener: ManagementActivationExpiredListener,
): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function emitManagementActivationExpired(event: ManagementActivationExpiredEvent): void {
  listeners.forEach((listener) => {
    listener(event);
  });
}
