/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

'use client';

import { DepartmentLayout } from '@/components/department';

export default function OrganizationDepartmentLayout({ children }: { children: React.ReactNode }) {
  return <DepartmentLayout>{children}</DepartmentLayout>;
}
