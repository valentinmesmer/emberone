/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/** Mirrors backend paginated list DTOs (e.g. {@link com.emberone.platform.web.PageDto}). */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

/** Matches {@link com.emberone.workflow.service.WorkflowService#DEFAULT_WORKFLOW_PAGE_SIZE}. */
export const DEFAULT_WORKFLOW_PAGE_SIZE = 50;
