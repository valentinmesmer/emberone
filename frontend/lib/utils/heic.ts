/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: Elastic-2.0
 */

const HEIF_BRANDS = [
  'heic',
  'heix',
  'heim',
  'heis',
  'hevc',
  'hevx',
  'hevm',
  'hevs',
  'mif1',
  'msf1',
];

/**
 * iPhone photos are HEIC by default and are often named .jpg/.jpeg, so the browser reports
 * `image/jpeg` for them. Only the file header tells the truth.
 */
export async function isHeifFile(file: File): Promise<boolean> {
  const header = new Uint8Array(await file.slice(0, 12).arrayBuffer());
  if (header.length < 12) return false;
  const text = (from: number, to: number) => String.fromCharCode(...header.slice(from, to));
  return text(4, 8) === 'ftyp' && HEIF_BRANDS.includes(text(8, 12));
}

/**
 * Returns the file as it can be stored and read everywhere: HEIC/HEIF photos become JPEG (decoded
 * in the browser with heic-to, loaded only when needed), every other file stays untouched.
 */
export async function toUploadableFile(file: File): Promise<File> {
  if (!(await isHeifFile(file))) return file;
  const { heicTo } = await import('heic-to');
  const jpeg = await heicTo({ blob: file, type: 'image/jpeg', quality: 0.9 });
  return new File([jpeg], `${file.name.replace(/\.[^.]*$/, '')}.jpg`, {
    type: 'image/jpeg',
    lastModified: file.lastModified,
  });
}

const HEIF_ACCEPT = 'image/heic,image/heif,.heic,.heif';

/**
 * Lets the file dialog offer HEIC photos wherever JPEG photos are accepted, because they are
 * converted to JPEG on selection ({@link toUploadableFile}). Other `accept` lists stay as they are.
 */
export function withHeifAccept(accept: string | undefined): string | undefined {
  return accept?.includes('image/jpeg') ? `${accept},${HEIF_ACCEPT}` : accept;
}
