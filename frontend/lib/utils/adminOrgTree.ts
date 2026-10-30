/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { AdminOrganizationTreeNodeDto } from '@/lib/api/admin';
import { compareText } from '@/lib/i18n/locale';

export interface FlatAdminOrganization {
  id: number;
  name: string;
  organizationCode: string;
  organizationTypeId: number;
}

export function collectAdminOrgSubtreeIds(
  roots: AdminOrganizationTreeNodeDto[],
  rootOrganizationId: number,
): Set<number> {
  const byId = new Map<number, AdminOrganizationTreeNodeDto>();

  function index(nodes: AdminOrganizationTreeNodeDto[]) {
    for (const node of nodes) {
      byId.set(node.id, node);
      if (node.children.length > 0) {
        index(node.children);
      }
    }
  }

  index(roots);

  const result = new Set<number>();
  const queue = [rootOrganizationId];
  while (queue.length > 0) {
    const current = queue.shift();
    if (current == null || result.has(current)) continue;
    result.add(current);
    const node = byId.get(current);
    if (!node) continue;
    for (const child of node.children) {
      queue.push(child.id);
    }
  }
  return result;
}

export interface ParentPickerOrganization {
  id: number;
  name: string;
  organizationCode: string;
  labelPrefix: string;
}

export function flattenAdminOrgTreeForParentPicker(
  roots: AdminOrganizationTreeNodeDto[],
  excludedIds: Set<number>,
): ParentPickerOrganization[] {
  const result: ParentPickerOrganization[] = [];

  function walk(nodes: AdminOrganizationTreeNodeDto[], depth: number) {
    for (const node of nodes) {
      if (!excludedIds.has(node.id) && !node.deactivatedAt) {
        result.push({
          id: node.id,
          name: node.name,
          organizationCode: node.organizationCode,
          labelPrefix: depth > 0 ? `${'-'.repeat(depth)} ` : '',
        });
      }
      if (node.children.length > 0) {
        walk(node.children, depth + 1);
      }
    }
  }

  walk(roots, 0);
  return result.sort((a, b) => compareText(a.name, b.name));
}

export function flattenAdminOrgTree(
  roots: AdminOrganizationTreeNodeDto[],
): FlatAdminOrganization[] {
  const result: FlatAdminOrganization[] = [];

  function walk(nodes: AdminOrganizationTreeNodeDto[]) {
    for (const node of nodes) {
      result.push({
        id: node.id,
        name: node.name,
        organizationCode: node.organizationCode,
        organizationTypeId: node.organizationTypeId,
      });
      if (node.children.length > 0) {
        walk(node.children);
      }
    }
  }

  walk(roots);
  return result.sort((a, b) => compareText(a.name, b.name));
}
