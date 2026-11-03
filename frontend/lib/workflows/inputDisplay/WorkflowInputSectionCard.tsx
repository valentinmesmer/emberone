/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import type { ReactNode } from 'react';
import { Card } from '@/components/ui/card';

export interface WorkflowInputSectionCardProps {
  children: ReactNode;
}

export function WorkflowInputSectionCard({ children }: WorkflowInputSectionCardProps) {
  return <Card className="space-y-3 p-3 sm:p-4">{children}</Card>;
}
