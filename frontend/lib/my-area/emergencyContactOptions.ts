/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { EmergencyContactRelationship } from '@/types/enums/emergencyContactRelationship';

export const EMERGENCY_CONTACT_RELATIONSHIP_OPTIONS = [
  EmergencyContactRelationship.SPOUSE,
  EmergencyContactRelationship.PARTNER,
  EmergencyContactRelationship.PARENT,
  EmergencyContactRelationship.CHILD,
  EmergencyContactRelationship.SIBLING,
  EmergencyContactRelationship.FRIEND,
  EmergencyContactRelationship.NEIGHBOR,
  EmergencyContactRelationship.COLLEAGUE,
  EmergencyContactRelationship.OTHER,
] as const;

export function getEmergencyContactRelationshipLabelKey(
  relationship: EmergencyContactRelationship,
): string {
  return `myArea.emergencyContact.relationship.${relationship}`;
}
