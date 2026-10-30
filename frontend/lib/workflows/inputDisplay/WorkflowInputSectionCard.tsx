/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ReactNode } from 'react';
import { Card } from '@/components/ui/card';

export interface WorkflowInputSectionCardProps {
  children: ReactNode;
}

export function WorkflowInputSectionCard({ children }: WorkflowInputSectionCardProps) {
  return <Card className="space-y-3 p-3 sm:p-4">{children}</Card>;
}
