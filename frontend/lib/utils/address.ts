/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Formats an address from its components
 * @param street - Street address
 * @param city - City name
 * @param postalCode - Postal/ZIP code
 * @param country - Country name
 * @returns Formatted address string or '-' if no address components are provided
 */
export function formatAddress(
  street?: string,
  city?: string,
  postalCode?: string,
  country?: string,
): string {
  const parts: string[] = [];

  if (street) {
    parts.push(street);
  }

  // Combine postal code and city
  if (postalCode && city) {
    parts.push(`${postalCode} ${city}`);
  } else if (city) {
    parts.push(city);
  } else if (postalCode) {
    parts.push(postalCode);
  }

  if (country) {
    parts.push(country);
  }

  return parts.filter(Boolean).join(', ') || '-';
}
