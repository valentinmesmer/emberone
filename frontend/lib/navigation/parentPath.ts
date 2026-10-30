/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { usePathname } from 'next/navigation';
import { useMemo } from 'react';
import {
  DEPARTMENT_SECTION,
  type DepartmentSection,
  getDepartmentSectionPath,
  isDepartmentEventSection,
  isDepartmentSection,
} from '@/lib/department/sections';
import {
  getMyAreaNotificationsPath,
  getMyAreaTodosPath,
  getMyAreaWorkflowDetailPath,
  getMyAreaWorkflowInboxPath,
  MY_AREA_BASE_PATH,
  MY_AREA_SECTIONS,
} from '@/lib/my-area/myAreaSections';
import { MY_AREA_SETTINGS_BASE_PATH } from '@/lib/my-area/settingsSections';
import {
  getOrganizationUserPath,
  getOrganizationUserSectionPath,
  type UserManagementSectionId,
} from '@/lib/navigation/userManagementSections';
import {
  getOrganizationGroupDetailPath,
  getOrganizationGroupsPath,
  getOrganizationOverviewPath,
  getOrganizationUsersPath,
} from '@/lib/utils/organization';

export const HOME_PATH = '/';

function normalizePathname(pathname: string): string {
  if (!pathname || pathname === '/') return HOME_PATH;
  return pathname.endsWith('/') ? pathname.slice(0, -1) : pathname;
}

/**
 * Resolves the parent route for hierarchical back navigation (not browser history).
 */
export function resolveParentPath(pathname: string): string | null {
  const path = normalizePathname(pathname);

  if (path === HOME_PATH) {
    return null;
  }

  const myAreaAbsenceDetail = path.match(/^\/my-area\/absence\/(\d+)$/);
  if (myAreaAbsenceDetail) {
    return `${MY_AREA_BASE_PATH}/absence`;
  }
  const myAreaTimeEntryDetail = path.match(/^\/my-area\/time-entries\/(\d+)$/);
  if (myAreaTimeEntryDetail) {
    return `${MY_AREA_BASE_PATH}/time-entries`;
  }
  const myAreaEmergencyContactDetail = path.match(/^\/my-area\/emergency-contact\/(\d+)$/);
  if (myAreaEmergencyContactDetail) {
    return `${MY_AREA_BASE_PATH}/emergency-contact`;
  }
  const myAreaDocumentDetail = path.match(/^\/my-area\/documents\/(\d+)$/);
  if (myAreaDocumentDetail) {
    return `${MY_AREA_BASE_PATH}/documents`;
  }
  const myAreaCertificationRecordDetail = path.match(
    /^\/my-area\/certifications\/(\d+)\/records\/(\d+)$/,
  );
  if (myAreaCertificationRecordDetail) {
    return `${MY_AREA_BASE_PATH}/certifications/${myAreaCertificationRecordDetail[1]}`;
  }
  const myAreaCertificationDetail = path.match(/^\/my-area\/certifications\/(\d+)$/);
  if (myAreaCertificationDetail) {
    return `${MY_AREA_BASE_PATH}/certifications`;
  }

  const myAreaTrainingCourseRecordDetail = path.match(/^\/my-area\/training-courses\/(\d+)$/);
  if (myAreaTrainingCourseRecordDetail) {
    return `${MY_AREA_BASE_PATH}/training-courses`;
  }

  const myAreaWorkflowApprovalDetail = path.match(
    /^\/my-area\/workflows\/(\d+)\/approvals\/(\d+)$/,
  );
  if (myAreaWorkflowApprovalDetail) {
    return getMyAreaWorkflowDetailPath(Number(myAreaWorkflowApprovalDetail[1]));
  }

  const myAreaWorkflowDetail = path.match(/^\/my-area\/workflows\/(\d+)$/);
  if (myAreaWorkflowDetail) {
    return getMyAreaWorkflowInboxPath();
  }

  const myAreaNotificationDetail = path.match(/^\/my-area\/notifications\/(\d+)$/);
  if (myAreaNotificationDetail) {
    return getMyAreaNotificationsPath();
  }

  const myAreaTodoDetail = path.match(/^\/my-area\/todos\/(\d+)$/);
  if (myAreaTodoDetail) {
    return getMyAreaTodosPath();
  }

  if (path.startsWith(`${MY_AREA_SETTINGS_BASE_PATH}/`)) {
    return MY_AREA_SETTINGS_BASE_PATH;
  }

  if (
    MY_AREA_SECTIONS.some((section) => path === section.path || path.startsWith(`${section.path}/`))
  ) {
    return MY_AREA_BASE_PATH;
  }

  if (path === '/my-area') {
    return HOME_PATH;
  }

  const appointmentDetailMatch = path.match(
    /^\/organizations\/(\d+)\/department\/appointments\/(\d+)$/,
  );
  if (appointmentDetailMatch) {
    const orgId = Number(appointmentDetailMatch[1]);
    return getDepartmentSectionPath(orgId, DEPARTMENT_SECTION.APPOINTMENTS);
  }

  const departmentEventDetailMatch = path.match(
    /^\/organizations\/(\d+)\/department\/([^/]+)\/(\d+)$/,
  );
  if (departmentEventDetailMatch) {
    const orgId = Number(departmentEventDetailMatch[1]);
    const section = departmentEventDetailMatch[2] as DepartmentSection;
    if (isDepartmentEventSection(section)) {
      return getDepartmentSectionPath(orgId, section);
    }
  }

  const departmentSectionMatch = path.match(/^\/organizations\/(\d+)\/department\/([^/]+)$/);
  if (departmentSectionMatch) {
    const section = departmentSectionMatch[2];
    if (isDepartmentSection(section)) {
      return getOrganizationOverviewPath(Number(departmentSectionMatch[1]));
    }
  }

  const departmentIndexMatch = path.match(/^\/organizations\/(\d+)\/department$/);
  if (departmentIndexMatch) {
    return getOrganizationOverviewPath(Number(departmentIndexMatch[1]));
  }

  const orgUserDetailMatch = path.match(/^\/organizations\/(\d+)\/users\/(\d+)$/);
  if (orgUserDetailMatch) {
    return `/organizations/${orgUserDetailMatch[1]}/users`;
  }

  const orgUserCertificationAssignmentDetail = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/certifications\/assignments\/(\d+)$/,
  );
  if (orgUserCertificationAssignmentDetail) {
    return getOrganizationUserSectionPath(
      Number(orgUserCertificationAssignmentDetail[1]),
      Number(orgUserCertificationAssignmentDetail[2]),
      'certifications',
    );
  }

  const orgUserCertificationRecordDetail = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/certifications\/(\d+)\/records\/(\d+)$/,
  );
  if (orgUserCertificationRecordDetail) {
    return `/organizations/${orgUserCertificationRecordDetail[1]}/users/${orgUserCertificationRecordDetail[2]}/certifications/${orgUserCertificationRecordDetail[3]}`;
  }

  const orgUserCertificationDetail = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/certifications\/(\d+)$/,
  );
  if (orgUserCertificationDetail) {
    return getOrganizationUserSectionPath(
      Number(orgUserCertificationDetail[1]),
      Number(orgUserCertificationDetail[2]),
      'certifications',
    );
  }

  const orgUserCertificationsSection = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/certifications$/,
  );
  if (orgUserCertificationsSection) {
    return getOrganizationUserPath(
      Number(orgUserCertificationsSection[1]),
      Number(orgUserCertificationsSection[2]),
    );
  }

  const orgUserTrainingCourseRecordDetail = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/training-courses\/(\d+)$/,
  );
  if (orgUserTrainingCourseRecordDetail) {
    return getOrganizationUserSectionPath(
      Number(orgUserTrainingCourseRecordDetail[1]),
      Number(orgUserTrainingCourseRecordDetail[2]),
      'training-courses',
    );
  }

  const orgUserQualificationDetail = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/qualifications\/(\d+)$/,
  );
  if (orgUserQualificationDetail) {
    return getOrganizationUserSectionPath(
      Number(orgUserQualificationDetail[1]),
      Number(orgUserQualificationDetail[2]),
      'qualifications',
    );
  }

  const orgUserQualificationsSection = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/qualifications$/,
  );
  if (orgUserQualificationsSection) {
    return getOrganizationUserPath(
      Number(orgUserQualificationsSection[1]),
      Number(orgUserQualificationsSection[2]),
    );
  }

  const orgUserTrainingCoursesSection = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/training-courses$/,
  );
  if (orgUserTrainingCoursesSection) {
    return getOrganizationUserPath(
      Number(orgUserTrainingCoursesSection[1]),
      Number(orgUserTrainingCoursesSection[2]),
    );
  }

  const orgUserDrivingLicensesSection = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/driving-licenses$/,
  );
  if (orgUserDrivingLicensesSection) {
    return getOrganizationUserPath(
      Number(orgUserDrivingLicensesSection[1]),
      Number(orgUserDrivingLicensesSection[2]),
    );
  }

  const orgUserSectionDetailMatch = path.match(
    /^\/organizations\/(\d+)\/users\/(\d+)\/(master-data|memberships|emergency-contacts|documents|workflows|absences|time-entries)(?:\/(\d+))?$/,
  );
  if (orgUserSectionDetailMatch) {
    const orgId = Number(orgUserSectionDetailMatch[1]);
    const userId = Number(orgUserSectionDetailMatch[2]);
    const section = orgUserSectionDetailMatch[3];
    if (orgUserSectionDetailMatch[4]) {
      return getOrganizationUserSectionPath(orgId, userId, section as UserManagementSectionId);
    }
    return getOrganizationUserPath(orgId, userId);
  }

  const orgGroupMembershipUnderGroupMatch = path.match(
    /^\/organizations\/(\d+)\/groups\/(\d+)\/memberships\/(\d+)$/,
  );
  if (orgGroupMembershipUnderGroupMatch) {
    return getOrganizationGroupDetailPath(
      Number(orgGroupMembershipUnderGroupMatch[1]),
      Number(orgGroupMembershipUnderGroupMatch[2]),
    );
  }

  const orgGroupDetailMatch = path.match(/^\/organizations\/(\d+)\/groups\/(\d+)$/);
  if (orgGroupDetailMatch) {
    return getOrganizationGroupsPath(Number(orgGroupDetailMatch[1]));
  }

  const orgGroupMembershipDetailMatch = path.match(
    /^\/organizations\/(\d+)\/group-memberships\/(\d+)$/,
  );
  if (orgGroupMembershipDetailMatch) {
    return getOrganizationUsersPath(Number(orgGroupMembershipDetailMatch[1]));
  }

  const orgGroupsMatch = path.match(/^\/organizations\/(\d+)\/groups$/);
  if (orgGroupsMatch) {
    return getOrganizationOverviewPath(Number(orgGroupsMatch[1]));
  }

  const orgSectionIndexMatch = path.match(
    /^\/organizations\/(\d+)\/(users|absences|groups|workflows|certifications|qualifications|ranks)$/,
  );
  if (orgSectionIndexMatch) {
    return getOrganizationOverviewPath(Number(orgSectionIndexMatch[1]));
  }

  // Organization overview hub itself returns to the global dashboard.
  const orgOverviewMatch = path.match(/^\/organizations\/(\d+)$/);
  if (orgOverviewMatch) {
    return HOME_PATH;
  }

  const adminTenantOrgTree = path.match(/^\/admin\/tenants\/(\d+)\/organizations\/tree$/);
  if (adminTenantOrgTree) {
    return `/admin/tenants/${adminTenantOrgTree[1]}`;
  }

  const adminTenantOrgIndex = path.match(/^\/admin\/tenants\/(\d+)\/organizations$/);
  if (adminTenantOrgIndex) {
    return `/admin/tenants/${adminTenantOrgIndex[1]}`;
  }

  // Admin: org detail → tenant page (root org) or parent org; {@link useRegisterAdminOrgBackHref} refines this.
  const adminTenantOrgDetail = path.match(/^\/admin\/tenants\/(\d+)\/organizations\/(\d+)$/);
  if (adminTenantOrgDetail) {
    return `/admin/tenants/${adminTenantOrgDetail[1]}`;
  }

  const adminStandardItemDetail = path.match(
    /^\/admin\/tenants\/(\d+)\/standards\/(\d+)\/(organization-groups|permission-groups|workflows|onboarding-steps)\/[^/]+$/,
  );
  if (adminStandardItemDetail) {
    return `/admin/tenants/${adminStandardItemDetail[1]}/standards/${adminStandardItemDetail[2]}`;
  }

  const adminStandardDetail = path.match(/^\/admin\/tenants\/(\d+)\/standards\/(\d+)$/);
  if (adminStandardDetail) {
    return `/admin/tenants/${adminStandardDetail[1]}/standards`;
  }

  const adminStandardsList = path.match(/^\/admin\/tenants\/(\d+)\/standards$/);
  if (adminStandardsList) {
    return `/admin/tenants/${adminStandardsList[1]}`;
  }

  const adminTenantStandards = path.match(/^\/admin\/tenants\/(\d+)\/tenant-standards$/);
  if (adminTenantStandards) {
    return `/admin/tenants/${adminTenantStandards[1]}`;
  }

  const adminOrganizationTypeDetail = path.match(
    /^\/admin\/tenants\/(\d+)\/tenant-standards\/organization-types\/(\d+)$/,
  );
  if (adminOrganizationTypeDetail) {
    return `/admin/tenants/${adminOrganizationTypeDetail[1]}/tenant-standards`;
  }

  const adminTenantCertificationDetail = path.match(
    /^\/admin\/tenants\/(\d+)\/tenant-standards\/certifications\/[^/]+$/,
  );
  if (adminTenantCertificationDetail) {
    return `/admin/tenants/${adminTenantCertificationDetail[1]}/tenant-standards`;
  }

  const adminTenantTrainingCourseDetail = path.match(
    /^\/admin\/tenants\/(\d+)\/tenant-standards\/training-courses\/[^/]+$/,
  );
  if (adminTenantTrainingCourseDetail) {
    return `/admin/tenants/${adminTenantTrainingCourseDetail[1]}/tenant-standards`;
  }

  const adminTenantWorkflowDetail = path.match(/^\/admin\/tenants\/(\d+)\/workflows\/(\d+)$/);
  if (adminTenantWorkflowDetail) {
    return `/admin/tenants/${adminTenantWorkflowDetail[1]}/tenant-standards`;
  }

  const adminOrgWorkflowDetail = path.match(
    /^\/admin\/tenants\/(\d+)\/organizations\/(\d+)\/workflows\/(\d+)$/,
  );
  if (adminOrgWorkflowDetail) {
    return `/admin/tenants/${adminOrgWorkflowDetail[1]}/organizations/${adminOrgWorkflowDetail[2]}`;
  }

  const segments = path.split('/').filter(Boolean);
  if (segments.length <= 1) {
    return HOME_PATH;
  }

  return `/${segments.slice(0, -1).join('/')}`;
}

export function useParentPath(): string | null {
  const pathname = usePathname();

  return useMemo(() => resolveParentPath(pathname), [pathname]);
}
