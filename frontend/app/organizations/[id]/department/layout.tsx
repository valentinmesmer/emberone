/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { DepartmentLayout } from '@/components/department';

export default function OrganizationDepartmentLayout({ children }: { children: React.ReactNode }) {
  return <DepartmentLayout>{children}</DepartmentLayout>;
}
