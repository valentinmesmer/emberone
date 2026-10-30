/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export function formatOrganizationMemberLabel(user: {
  firstName?: string | null;
  lastName?: string | null;
  username?: string | null;
}): string {
  const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
  if (fullName) {
    return fullName;
  }
  return user.username?.trim() ?? '';
}

/**
 * Resolves a user label from a member list; missing users are shown as former members.
 */
export function resolveUserDisplayName(
  users: {
    id: number;
    firstName?: string | null;
    lastName?: string | null;
    username?: string | null;
  }[],
  userId: number | null | undefined,
  formerMemberLabel: string,
): string {
  if (userId == null) {
    return '-';
  }
  const user = users.find((item) => item.id === userId);
  if (!user) {
    return formerMemberLabel;
  }
  const fullName = `${user.firstName ?? ''} ${user.lastName ?? ''}`.trim();
  if (fullName) {
    return fullName;
  }
  if (user.username?.trim()) {
    return user.username.trim();
  }
  return formerMemberLabel;
}
