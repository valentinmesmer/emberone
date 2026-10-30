/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { EmergencyContactRelationship } from './enums/emergencyContactRelationship';

export interface EmergencyContactDto {
  id: number;
  userId: number;
  priority: number;
  relationship: EmergencyContactRelationship;
  firstName: string;
  lastName: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  email: string;
}

export interface CreateEmergencyContactRequest {
  relationship: EmergencyContactRelationship;
  firstName: string;
  lastName: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  email: string;
}

export interface UpdateEmergencyContactRequest {
  relationship: EmergencyContactRelationship;
  firstName: string;
  lastName: string;
  street: string;
  city: string;
  postalCode: string;
  country: string;
  phone: string;
  email: string;
}

export interface ReorderEmergencyContactsRequest {
  contactIds: number[];
}
