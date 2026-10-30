/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { apiClient } from '@/lib/api/client';

export interface DrivingLicenseDefinitionDto {
  id: number;
  name: string;
  active: boolean;
  configKey: string;
}

export interface DrivingLicenseClassDto {
  tenantDrivingLicenseDefinitionId: number;
  className: string | null;
  abbreviation: string | null;
  definitionArchived: boolean;
  grantDate: string | null;
  expirationDate: string | null;
}

export interface DrivingLicenseDto {
  id: number;
  userId: number;
  issueDate: string;
  documentExpirationDate: string | null;
  licenseNumber: string | null;
  issuingAuthority: string | null;
  userDocumentIds: number[];
  classes: DrivingLicenseClassDto[];
  notes: string | null;
  appLinkType: string | null;
  appLinkId: number | null;
  navigationPath: string | null;
}

export interface DrivingLicenseClassInput {
  tenantDrivingLicenseDefinitionId: number;
  grantDate?: string | null;
  expirationDate?: string | null;
}

export interface OrgDrivingLicenseCatalogDto {
  organizationId: number;
  definitions: DrivingLicenseDefinitionDto[];
}

export interface CreateDrivingLicenseRequest {
  issueDate: string;
  documentExpirationDate?: string | null;
  licenseNumber: string;
  issuingAuthority: string;
  classes: DrivingLicenseClassInput[];
  userDocumentIds?: number[];
  notes?: string | null;
}

export interface UpdateDrivingLicenseRequest {
  issueDate: string;
  documentExpirationDate?: string | null;
  licenseNumber: string;
  issuingAuthority: string;
  classes: DrivingLicenseClassInput[];
  userDocumentIds?: number[];
  notes?: string | null;
}

export interface DrivingLicensePendingWorkflowDto {
  workflowId: number;
  issueDate: string;
  documentExpirationDate: string | null;
  licenseNumber: string | null;
  issuingAuthority: string | null;
  classes: DrivingLicenseClassInput[];
}

const selfBase = '/api/v1/self/driving-license';
const orgBase = (organizationId: number, userId: number) =>
  `/api/v1/auth/organizations/${organizationId}/users/${userId}/driving-license`;

export const drivingLicensesApi = {
  getMyLicense(): Promise<DrivingLicenseDto | null> {
    return apiClient.get<DrivingLicenseDto | null>(selfBase);
  },

  getMyAvailableDefinitions(): Promise<DrivingLicenseDefinitionDto[]> {
    return apiClient.get<DrivingLicenseDefinitionDto[]>(`${selfBase}/available-definitions`);
  },

  getUserLicense(organizationId: number, userId: number): Promise<DrivingLicenseDto | null> {
    return apiClient.get<DrivingLicenseDto | null>(orgBase(organizationId, userId));
  },

  getPendingWorkflow(
    organizationId: number,
    userId: number,
  ): Promise<DrivingLicensePendingWorkflowDto | null> {
    return apiClient.get<DrivingLicensePendingWorkflowDto | null>(
      `${orgBase(organizationId, userId)}/pending-workflow`,
    );
  },

  createLicense(
    organizationId: number,
    userId: number,
    data: CreateDrivingLicenseRequest,
  ): Promise<DrivingLicenseDto> {
    return apiClient.post<DrivingLicenseDto>(orgBase(organizationId, userId), data);
  },

  updateLicense(
    organizationId: number,
    userId: number,
    data: UpdateDrivingLicenseRequest,
  ): Promise<DrivingLicenseDto> {
    return apiClient.put<DrivingLicenseDto>(orgBase(organizationId, userId), data);
  },

  deleteLicense(organizationId: number, userId: number): Promise<void> {
    return apiClient.delete<void>(orgBase(organizationId, userId));
  },

  getAvailableDefinitions(
    organizationId: number,
    userId: number,
  ): Promise<OrgDrivingLicenseCatalogDto> {
    return apiClient.get<OrgDrivingLicenseCatalogDto>(
      `${orgBase(organizationId, userId)}/available-definitions`,
    );
  },
};
