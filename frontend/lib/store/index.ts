/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export { getRequestErrorMessage } from '@/lib/api/requestError';
export { invalidateServerCacheTopic, type ServerCacheTopic } from './cache-invalidation';
export { QueryProvider } from './providers';
export * from './queries/absences';
export * from './queries/adminCourseOfferingTypes';
export * from './queries/adminLeadershipGroup';
export * from './queries/adminLists';
export * from './queries/adminManagementAudience';
export * from './queries/appointments';
export * from './queries/certifications';
export * from './queries/courseOfferings';
export * from './queries/courseOfferings';
export * from './queries/drivingLicenses';
export * from './queries/emergencyContacts';
export * from './queries/eventAttendance';
export * from './queries/eventCertificationGrants';
export * from './queries/events';
export * from './queries/management';
export * from './queries/notificationPreferences';
export * from './queries/notifications';
export * from './queries/onboardingInvitation';
export * from './queries/organizationOpenWorkflows';
export * from './queries/organizations';
export * from './queries/organizationUserAbsences';
export * from './queries/organizationUserDocuments';
export * from './queries/organizationUserEmergencyContacts';
export * from './queries/organizationUserTimeEntries';
export * from './queries/organizationUserWorkflows';
export * from './queries/permissions';
export * from './queries/profileMedia';
export * from './queries/qualifications';
export * from './queries/ranks';
export * from './queries/timeEntries';
export * from './queries/todos';
export * from './queries/trainingCourses';
export * from './queries/userDocuments';
export * from './queries/userMembershipOverview';
export * from './queries/userOrganizationGroupMembership';
export * from './queries/userOrganizationMembership';
export * from './queries/users';
export * from './queries/workflows';
export { queryClient } from './query-client';
/**
 * Single entry for app server cache + UI store.
 *
 * - Data: React Query hooks (`use*` from this module)
 * - UI only: `useUIStore` (Zustand)
 * - Cross-cutting invalidation: `invalidateServerCacheTopic`, `queryClient`
 */
export { useUIStore } from './ui-store';
export { useAppMutation } from './useAppMutation';
export {
  flatMapPageContent,
  nextPageParamFromResponse,
  usePagedList,
} from './usePagedList';
export { useQueryErrorToast } from './useQueryErrorToast';
export { useRefreshServerCache } from './useRefreshServerCache';
