/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

import { tenantCertificationsApi } from '@/lib/api/tenantCertifications';
import { tenantDrivingLicenseDefinitionsApi } from '@/lib/api/tenantDrivingLicenseDefinitions';
import { tenantRanksApi } from '@/lib/api/tenantRanks';
import { tenantTrainingCourseTypesApi } from '@/lib/api/tenantTrainingCourseTypes';
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
  loadOptions: (tenantId: number) => Promise<RoutingSubjectOption[]>;
}

function toOption(item: { id: number; name: string; archived: boolean }): RoutingSubjectOption {
  return { id: item.id, label: item.name, archived: item.archived };
}

/**
 * One entry per routing dimension of a workflow type (see backend WorkflowRoutingDimension).
 * A new dimension needs its singular/plural keys in locales/de.json and a catalog loader here.
 */
export const ROUTING_DIMENSIONS: Record<WorkflowRoutingDimension, RoutingDimensionConfig> = {
  CERTIFICATION_DEFINITION: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.CERTIFICATION_DEFINITION.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.CERTIFICATION_DEFINITION.plural',
    loadOptions: async (tenantId) => (await tenantCertificationsApi.list(tenantId)).map(toOption),
  },
  TRAINING_COURSE_TYPE: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.TRAINING_COURSE_TYPE.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.TRAINING_COURSE_TYPE.plural',
    loadOptions: async (tenantId) =>
      (await tenantTrainingCourseTypesApi.list(tenantId)).map(toOption),
  },
  DRIVING_LICENSE_DEFINITION: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.DRIVING_LICENSE_DEFINITION.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.DRIVING_LICENSE_DEFINITION.plural',
    loadOptions: async (tenantId) =>
      (await tenantDrivingLicenseDefinitionsApi.list(tenantId)).map(toOption),
  },
  RANK: {
    singularKey: 'admin.workflowDefinitions.routing.dimension.RANK.singular',
    pluralKey: 'admin.workflowDefinitions.routing.dimension.RANK.plural',
    loadOptions: async (tenantId) => (await tenantRanksApi.list(tenantId)).map(toOption),
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
