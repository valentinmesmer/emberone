/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import type { CertificationEventGrantDto } from '@/lib/api/eventCertificationGrants';

export function eventHasCertificationGrant(
  grants: CertificationEventGrantDto[] | undefined,
): boolean {
  return (grants?.length ?? 0) > 0;
}
