/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { getReadPermissionForEventType } from '@/lib/utils/event';
import { EventType } from '@/types/event';
import { PermissionTypeEnum } from '@/types/permission';

export const DEPARTMENT_SECTION = {
  APPOINTMENTS: 'appointments',
  SERVICE: 'service',
  OPERATION: 'operation',
} as const;

export type DepartmentSection = (typeof DEPARTMENT_SECTION)[keyof typeof DEPARTMENT_SECTION];

export const DEPARTMENT_EVENT_SECTIONS = [
  DEPARTMENT_SECTION.SERVICE,
  DEPARTMENT_SECTION.OPERATION,
] as const;

export type DepartmentEventSection = (typeof DEPARTMENT_EVENT_SECTIONS)[number];

const EVENT_TYPE_BY_SECTION: Record<DepartmentEventSection, EventType> = {
  [DEPARTMENT_SECTION.SERVICE]: EventType.SERVICE,
  [DEPARTMENT_SECTION.OPERATION]: EventType.OPERATION,
};

const SECTION_BY_EVENT_TYPE: Record<EventType, DepartmentEventSection> = {
  [EventType.SERVICE]: DEPARTMENT_SECTION.SERVICE,
  [EventType.OPERATION]: DEPARTMENT_SECTION.OPERATION,
};

const ALL_SECTIONS: DepartmentSection[] = [
  DEPARTMENT_SECTION.APPOINTMENTS,
  ...DEPARTMENT_EVENT_SECTIONS,
];

export function isDepartmentSection(value: string): value is DepartmentSection {
  return (ALL_SECTIONS as string[]).includes(value);
}

export function getEventTypeForSection(section: DepartmentSection): EventType | null {
  if (!isDepartmentEventSection(section)) {
    return null;
  }
  return EVENT_TYPE_BY_SECTION[section];
}

export function getEventTypeForDepartmentEventSection(section: DepartmentEventSection): EventType {
  return EVENT_TYPE_BY_SECTION[section];
}

export function isDepartmentEventSection(
  section: DepartmentSection,
): section is DepartmentEventSection {
  return (DEPARTMENT_EVENT_SECTIONS as readonly string[]).includes(section);
}

export function getSectionForEventType(eventType: EventType): DepartmentEventSection {
  return SECTION_BY_EVENT_TYPE[eventType];
}

export function getDepartmentSectionPath(
  organizationId: number,
  section: DepartmentSection,
): string {
  return `/organizations/${organizationId}/department/${section}`;
}

export function getAppointmentDetailPath(organizationId: number, appointmentId: number): string {
  return `/organizations/${organizationId}/department/${DEPARTMENT_SECTION.APPOINTMENTS}/${appointmentId}`;
}

export function getEventDetailPath(
  organizationId: number,
  section: DepartmentEventSection,
  eventId: number,
): string {
  return `/organizations/${organizationId}/department/${section}/${eventId}`;
}

export function getEventCertificationGrantDetailPath(
  organizationId: number,
  section: DepartmentEventSection,
  eventId: number,
  grantId: number,
): string {
  return `${getEventDetailPath(organizationId, section, eventId)}/certification-grants/${grantId}`;
}

export function getReadPermissionForSection(section: DepartmentSection): PermissionTypeEnum | null {
  if (section === DEPARTMENT_SECTION.APPOINTMENTS) {
    return PermissionTypeEnum.APPOINTMENT_READ;
  }
  const eventType = getEventTypeForSection(section);
  return eventType ? getReadPermissionForEventType(eventType) : null;
}

export function getFirstAccessibleSection(
  organizationId: number,
  hasPermission: (orgId: number, permission: PermissionTypeEnum) => boolean,
): DepartmentSection | null {
  for (const section of ALL_SECTIONS) {
    const permission = getReadPermissionForSection(section);
    if (permission && hasPermission(organizationId, permission)) {
      return section;
    }
  }
  return null;
}
