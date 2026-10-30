/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useHasEmberOneUserId } from '@/hooks/useHasEmberOneUserId';
import {
  type CreateDrivingLicenseRequest,
  drivingLicensesApi,
  type UpdateDrivingLicenseRequest,
} from '@/lib/api/drivingLicenses';
import { organizationUserDocumentKeys } from '@/lib/store/queries/organizationUserDocuments';
import { invalidateQualificationStatusForOrganization } from '@/lib/store/queries/qualifications';

export const drivingLicenseKeys = {
  all: ['drivingLicenses'] as const,
  myLicense: () => [...drivingLicenseKeys.all, 'my-license'] as const,
  myAvailableDefinitions: () => [...drivingLicenseKeys.all, 'my-available-definitions'] as const,
  userLicense: (orgId: number, userId: number) =>
    [...drivingLicenseKeys.all, 'user-license', orgId, userId] as const,
  pendingWorkflow: (orgId: number, userId: number) =>
    [...drivingLicenseKeys.all, 'pending-workflow', orgId, userId] as const,
  catalog: (orgId: number, userId: number) =>
    [...drivingLicenseKeys.all, 'catalog', orgId, userId] as const,
};

export function useMyDrivingLicense(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: drivingLicenseKeys.myLicense(),
    queryFn: () => drivingLicensesApi.getMyLicense(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 60 * 1000,
  });
}

export function useMyAvailableDrivingLicenseDefinitions(enabled = true) {
  const hasEmberOneUserId = useHasEmberOneUserId();
  return useQuery({
    queryKey: drivingLicenseKeys.myAvailableDefinitions(),
    queryFn: () => drivingLicensesApi.getMyAvailableDefinitions(),
    enabled: hasEmberOneUserId && enabled,
    staleTime: 5 * 60 * 1000,
  });
}

export function useUserDrivingLicense(organizationId: number, userId: number, enabled = true) {
  return useQuery({
    queryKey: drivingLicenseKeys.userLicense(organizationId, userId),
    queryFn: () => drivingLicensesApi.getUserLicense(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useUserDrivingLicensePendingWorkflow(
  organizationId: number,
  userId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: drivingLicenseKeys.pendingWorkflow(organizationId, userId),
    queryFn: () => drivingLicensesApi.getPendingWorkflow(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 60 * 1000,
  });
}

export function useOrgDrivingLicenseCatalog(
  organizationId: number,
  userId: number,
  enabled = true,
) {
  return useQuery({
    queryKey: drivingLicenseKeys.catalog(organizationId, userId),
    queryFn: () => drivingLicensesApi.getAvailableDefinitions(organizationId, userId),
    enabled: enabled && organizationId > 0 && userId > 0,
    staleTime: 5 * 60 * 1000,
  });
}

function useInvalidateUserDrivingLicense(organizationId: number, userId: number) {
  const queryClient = useQueryClient();
  return () => {
    invalidateQualificationStatusForOrganization(queryClient, organizationId);
    return Promise.all([
      queryClient.invalidateQueries({ queryKey: drivingLicenseKeys.myLicense() }),
      queryClient.invalidateQueries({
        queryKey: drivingLicenseKeys.userLicense(organizationId, userId),
      }),
      queryClient.invalidateQueries({
        queryKey: drivingLicenseKeys.pendingWorkflow(organizationId, userId),
      }),
      queryClient.invalidateQueries({
        queryKey: drivingLicenseKeys.catalog(organizationId, userId),
      }),
      queryClient.invalidateQueries({
        queryKey: organizationUserDocumentKeys.listRoot(organizationId, userId),
      }),
    ]);
  };
}

export function useCreateDrivingLicense(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserDrivingLicense(organizationId, userId);
  return useMutation({
    mutationFn: (data: CreateDrivingLicenseRequest) =>
      drivingLicensesApi.createLicense(organizationId, userId, data),
    onSuccess: () => void invalidate(),
  });
}

export function useUpdateDrivingLicense(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserDrivingLicense(organizationId, userId);
  return useMutation({
    mutationFn: (data: UpdateDrivingLicenseRequest) =>
      drivingLicensesApi.updateLicense(organizationId, userId, data),
    onSuccess: () => void invalidate(),
  });
}

export function useDeleteDrivingLicense(organizationId: number, userId: number) {
  const invalidate = useInvalidateUserDrivingLicense(organizationId, userId);
  return useMutation({
    mutationFn: () => drivingLicensesApi.deleteLicense(organizationId, userId),
    onSuccess: () => void invalidate(),
  });
}
