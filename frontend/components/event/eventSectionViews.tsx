/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { ComponentType } from 'react';
import { DEPARTMENT_SECTION, type DepartmentEventSection } from '@/lib/department/sections';
import { OperationEventsView } from './OperationEventsView';
import { ServiceEventsView } from './ServiceEventsView';

export interface EventSectionViewProps {
  organizationId: number;
}

export const EVENT_SECTION_VIEWS: Record<
  DepartmentEventSection,
  ComponentType<EventSectionViewProps>
> = {
  [DEPARTMENT_SECTION.SERVICE]: ServiceEventsView,
  [DEPARTMENT_SECTION.OPERATION]: OperationEventsView,
};
