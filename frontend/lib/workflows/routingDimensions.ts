/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { useMemo } from 'react';
import {
  useTenantCertifications,
  useTenantDrivingLicenseDefinitions,
  useTenantRanks,
  useTenantTrainingCourseTypes,
} from '@/lib/store';
import type { WorkflowRoutingDimension } from '@/types/workflow';

export interface RoutingSubjectOption {
  id: number;
  label: string;
  archived: boolean;
}

interface RoutingDimensionConfig {
  /** i18n key of the singular noun, used in "Regeln je …". */
  singularKey: string;
  /** i18n key of the plural noun, used in "Gilt für alle …" and the subject picker. */
  pluralKey: string;
}

function toOption(item: { id: number; name: string; archived: boolean }): RoutingSubjectOption {
  return { id: item.id, label: item.name, archived: item.archived };
}

/**
 * One entry per routing dimension of a workflow type (see backend WorkflowRoutingDimension).
 * A new dimension needs its singular/plural keys in locales/de.json and a catalog query in
 * {@link useRoutingSubjectOptions}.
 */
export const ROUTING_DIMENSIONS: Record<WorkflowRoutingDimension, RoutingDimensionConfig> = {
  CERTIFICATION_DEFINITION: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.CERTIFICATION_DEFINITION.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.CERTIFICATION_DEFINITION.plural',
  },
  TRAINING_COURSE_TYPE: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.TRAINING_COURSE_TYPE.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.TRAINING_COURSE_TYPE.plural',
  },
  DRIVING_LICENSE_DEFINITION: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.DRIVING_LICENSE_DEFINITION.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.DRIVING_LICENSE_DEFINITION.plural',
  },
  RANK: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.RANK.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.RANK.plural',
  },
};

/** Singular and plural noun of a routing dimension, e.g. "Zertifizierung" / "Zertifizierungen". */
export function routingDimensionLabels(
  dimension: WorkflowRoutingDimension,
  t: (key: string) => string,
): { singular: string; plural: string } {
  const config = ROUTING_DIMENSIONS[dimension];
  return { singular: t(config.singularKey), plural: t(config.pluralKey) };
}

/** Subjects (catalog entries) of a routing dimension; empty while loading or without dimension. */
export function useRoutingSubjectOptions(
  tenantId: number,
  dimension: WorkflowRoutingDimension | null,
): RoutingSubjectOption[] {
  const certifications = useTenantCertifications(
    tenantId,
    dimension === 'CERTIFICATION_DEFINITION',
  );
  const trainingCourseTypes = useTenantTrainingCourseTypes(
    tenantId,
    dimension === 'TRAINING_COURSE_TYPE',
  );
  const drivingLicenses = useTenantDrivingLicenseDefinitions(
    tenantId,
    dimension === 'DRIVING_LICENSE_DEFINITION',
  );
  const ranks = useTenantRanks(tenantId, dimension === 'RANK');
  const items = {
    CERTIFICATION_DEFINITION: certifications.data,
    TRAINING_COURSE_TYPE: trainingCourseTypes.data,
    DRIVING_LICENSE_DEFINITION: drivingLicenses.data,
    RANK: ranks.data,
  }[dimension ?? 'RANK'];
  return useMemo(() => (dimension && items ? items.map(toOption) : []), [dimension, items]);
}
