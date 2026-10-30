/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Numeric ids match {@link com.emberone.permission.domain.PermissionType} (JWT / API JSON).
 */
export enum PermissionTypeEnum {
  ORGANIZATION_READ = 1,
  ORGANIZATION_GROUP_READ = 2,
  USER_READ = 3,
  USER_WRITE = 4,
  SERVICE_EVENT_READ = 5,
  SERVICE_EVENT_WRITE = 6,
  OPERATION_EVENT_READ = 7,
  OPERATION_EVENT_WRITE = 8,
  APPOINTMENT_READ = 9,
  APPOINTMENT_WRITE = 10,
  EVENT_ATTENDANCE_READ = 11,
  EVENT_ATTENDANCE_WRITE = 12,
  EMERGENCY_CONTACT_READ = 13,
  EMERGENCY_CONTACT_WRITE = 14,
  ABSENCE_READ = 15,
  ABSENCE_WRITE = 16,
  DOCUMENT_READ = 17,
  DOCUMENT_WRITE = 18,
  TIME_ENTRY_READ = 19,
  TIME_ENTRY_WRITE = 20,
  WORKFLOW_READ = 21,
  CERTIFICATION_READ = 22,
  CERTIFICATION_WRITE = 23,
  TRAINING_COURSE_READ = 24,
  TRAINING_COURSE_WRITE = 25,
  QUALIFICATION_READ = 26,
  QUALIFICATION_WRITE = 27,
  DRIVING_LICENSE_READ = 28,
  DRIVING_LICENSE_WRITE = 29,
  PROFILE_IMAGE_READ = 30,
  RANK_READ = 31,
  RANK_WRITE = 32,
  COURSE_OFFERING_READ = 33,
  COURSE_OFFERING_WRITE = 34,
  MANAGEMENT_ACCESS = 35,
}

export type SupportedLocale = 'de';

export type { FederalState } from '@/lib/federal-state';

export interface UserPermissionDto {
  permissions: Record<number, PermissionTypeEnum[]>;
  tenantLocale?: SupportedLocale;
}

export enum Module {
  CORE = 'CORE',
  SERVICE = 'SERVICE',
  OPERATIONS = 'OPERATIONS',
  APPOINTMENTS = 'APPOINTMENTS',
  COURSE_OFFERINGS = 'COURSE_OFFERINGS',
  MANAGEMENT = 'MANAGEMENT',
}

const MODULE_TOGGLEABLE: Record<Module, boolean> = {
  [Module.CORE]: false,
  [Module.SERVICE]: true,
  [Module.OPERATIONS]: true,
  [Module.APPOINTMENTS]: true,
  [Module.COURSE_OFFERINGS]: true,
  [Module.MANAGEMENT]: true,
};

export const TOGGLEABLE_MODULES: readonly Module[] = (Object.values(Module) as Module[]).filter(
  (module) => MODULE_TOGGLEABLE[module],
);

export const PERMISSION_MODULE_MAP: Record<PermissionTypeEnum, Module> = {
  [PermissionTypeEnum.ORGANIZATION_READ]: Module.CORE,
  [PermissionTypeEnum.ORGANIZATION_GROUP_READ]: Module.CORE,
  [PermissionTypeEnum.USER_READ]: Module.CORE,
  [PermissionTypeEnum.USER_WRITE]: Module.CORE,
  [PermissionTypeEnum.SERVICE_EVENT_READ]: Module.SERVICE,
  [PermissionTypeEnum.SERVICE_EVENT_WRITE]: Module.SERVICE,
  [PermissionTypeEnum.OPERATION_EVENT_READ]: Module.OPERATIONS,
  [PermissionTypeEnum.OPERATION_EVENT_WRITE]: Module.OPERATIONS,
  [PermissionTypeEnum.APPOINTMENT_READ]: Module.APPOINTMENTS,
  [PermissionTypeEnum.APPOINTMENT_WRITE]: Module.APPOINTMENTS,
  [PermissionTypeEnum.EVENT_ATTENDANCE_READ]: Module.APPOINTMENTS,
  [PermissionTypeEnum.EVENT_ATTENDANCE_WRITE]: Module.APPOINTMENTS,
  [PermissionTypeEnum.EMERGENCY_CONTACT_READ]: Module.CORE,
  [PermissionTypeEnum.EMERGENCY_CONTACT_WRITE]: Module.CORE,
  [PermissionTypeEnum.ABSENCE_READ]: Module.CORE,
  [PermissionTypeEnum.ABSENCE_WRITE]: Module.CORE,
  [PermissionTypeEnum.DOCUMENT_READ]: Module.CORE,
  [PermissionTypeEnum.DOCUMENT_WRITE]: Module.CORE,
  [PermissionTypeEnum.TIME_ENTRY_READ]: Module.CORE,
  [PermissionTypeEnum.TIME_ENTRY_WRITE]: Module.CORE,
  [PermissionTypeEnum.WORKFLOW_READ]: Module.CORE,
  [PermissionTypeEnum.CERTIFICATION_READ]: Module.CORE,
  [PermissionTypeEnum.CERTIFICATION_WRITE]: Module.CORE,
  [PermissionTypeEnum.TRAINING_COURSE_READ]: Module.CORE,
  [PermissionTypeEnum.TRAINING_COURSE_WRITE]: Module.CORE,
  [PermissionTypeEnum.QUALIFICATION_READ]: Module.CORE,
  [PermissionTypeEnum.QUALIFICATION_WRITE]: Module.CORE,
  [PermissionTypeEnum.DRIVING_LICENSE_READ]: Module.CORE,
  [PermissionTypeEnum.DRIVING_LICENSE_WRITE]: Module.CORE,
  [PermissionTypeEnum.PROFILE_IMAGE_READ]: Module.CORE,
  [PermissionTypeEnum.RANK_READ]: Module.CORE,
  [PermissionTypeEnum.RANK_WRITE]: Module.CORE,
  [PermissionTypeEnum.COURSE_OFFERING_READ]: Module.COURSE_OFFERINGS,
  [PermissionTypeEnum.COURSE_OFFERING_WRITE]: Module.COURSE_OFFERINGS,
  [PermissionTypeEnum.MANAGEMENT_ACCESS]: Module.MANAGEMENT,
};
