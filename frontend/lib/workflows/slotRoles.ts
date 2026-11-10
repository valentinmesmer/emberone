/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/** Approver slot roles with a translation; mirrors backend `WorkflowApproverSlotRoles`. */
const KNOWN_SLOT_ROLES = new Set([
  'CONTEXT',
  'ADDITIONAL',
  'NEW_MAIN_ORG',
  'TARGET_ORG',
  'HOME_ORG',
]);

type Translate = (key: string) => string;

/**
 * Looks up `workflows.<group>.<TYPE_KEY>.<ROLE>` first (type-specific wording), then the generic
 * `workflows.<group>.<ROLE>`. A missing key makes `t` return the key itself.
 */
function lookup(group: string, role: string, t: Translate, typeKey?: string | null): string | null {
  if (!KNOWN_SLOT_ROLES.has(role)) return null;
  if (typeKey) {
    const specificKey = `workflows.${group}.${typeKey}.${role}`;
    const specific = t(specificKey);
    if (specific !== specificKey) return specific;
  }
  return t(`workflows.${group}.${role}`);
}

/**
 * Label of an approver slot role, specific to the workflow type where the wording differs
 * (`typeKey` is the workflow type enum key); an unknown role (newer backend) falls back to its key.
 */
export function workflowSlotRoleLabel(role: string, t: Translate, typeKey?: string | null): string {
  return lookup('slotRoles', role, t, typeKey) ?? role;
}

/** One-line explanation of what the role means; null for an unknown role. */
export function workflowSlotRoleHint(
  role: string,
  t: Translate,
  typeKey?: string | null,
): string | null {
  return lookup('slotRoleHints', role, t, typeKey);
}
