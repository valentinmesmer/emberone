/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

const STORAGE_KEY = 'emberone.listSiblings';

/** Carries the detail href of a `ListRow`; the rows of one list define prev/next on the detail page. */
export const LIST_ROW_HREF_ATTRIBUTE = 'data-list-row-href';

function toPathname(href: string): string {
  return href.split(/[?#]/)[0] ?? href;
}

/** Remembers the detail hrefs of the list the clicked row belongs to (only what is loaded). */
export function rememberListSiblings(rowLink: HTMLElement): void {
  const list = rowLink.closest('ul');
  if (!list) return;
  const paths = Array.from(list.querySelectorAll<HTMLElement>(`[${LIST_ROW_HREF_ATTRIBUTE}]`))
    .map((row) => row.getAttribute(LIST_ROW_HREF_ATTRIBUTE))
    .filter((href): href is string => !!href)
    .map(toPathname);
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(paths));
  } catch {
    // storage unavailable: no sibling navigation
  }
}

export function readListSiblings(): string[] {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    const parsed: unknown = raw ? JSON.parse(raw) : [];
    return Array.isArray(parsed) ? parsed.filter((p): p is string => typeof p === 'string') : [];
  } catch {
    return [];
  }
}
