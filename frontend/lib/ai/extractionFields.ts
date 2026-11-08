/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

/** Defensive readers for the untyped `fields` of an extraction suggestion. */

export function readString(fields: Record<string, unknown>, key: string): string | undefined {
  const value = fields[key];
  return typeof value === 'string' && value.trim() ? value : undefined;
}

export function readNumber(fields: Record<string, unknown>, key: string): number | undefined {
  const value = fields[key];
  return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
}

export function readNumberArray(fields: Record<string, unknown>, key: string): number[] {
  const value = fields[key];
  return Array.isArray(value)
    ? value.filter((item): item is number => typeof item === 'number' && Number.isFinite(item))
    : [];
}

export function readStringArray(fields: Record<string, unknown>, key: string): string[] {
  const value = fields[key];
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    : [];
}

export function readObjectArray(
  fields: Record<string, unknown>,
  key: string,
): Record<string, unknown>[] {
  const value = fields[key];
  return Array.isArray(value)
    ? value.filter(
        (item): item is Record<string, unknown> =>
          typeof item === 'object' && item !== null && !Array.isArray(item),
      )
    : [];
}

/** Document types the backend can read (PDFs: up to three pages are rendered server-side). */
export const EXTRACTION_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/gif',
  'application/pdf',
];

/** Most files the backend reads in one request (front and back of a card, several pages). */
export const EXTRACTION_MAX_FILES = 5;

/** All chosen files the backend can read; all of them go into one extraction request. */
export function pickExtractionFiles(files: File[]): File[] {
  return files
    .filter((file) => EXTRACTION_IMAGE_TYPES.includes(file.type))
    .slice(0, EXTRACTION_MAX_FILES);
}
