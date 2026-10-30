/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

export type FederalState =
  | 'BADEN_WUERTTEMBERG'
  | 'BAYERN'
  | 'BERLIN'
  | 'BRANDENBURG'
  | 'BREMEN'
  | 'HAMBURG'
  | 'HESSEN'
  | 'MECKLENBURG_VORPOMMERN'
  | 'NIEDERSACHSEN'
  | 'NORDRHEIN_WESTFALEN'
  | 'RHEINLAND_PFALZ'
  | 'SAARLAND'
  | 'SACHSEN'
  | 'SACHSEN_ANHALT'
  | 'SCHLESWIG_HOLSTEIN'
  | 'THUERINGEN';

export const DEFAULT_FEDERAL_STATE: FederalState = 'BAYERN';

/** All supported federal states (stable order for filters and forms). */
export const FEDERAL_STATES: readonly FederalState[] = [
  'BADEN_WUERTTEMBERG',
  'BAYERN',
  'BERLIN',
  'BRANDENBURG',
  'BREMEN',
  'HAMBURG',
  'HESSEN',
  'MECKLENBURG_VORPOMMERN',
  'NIEDERSACHSEN',
  'NORDRHEIN_WESTFALEN',
  'RHEINLAND_PFALZ',
  'SAARLAND',
  'SACHSEN',
  'SACHSEN_ANHALT',
  'SCHLESWIG_HOLSTEIN',
  'THUERINGEN',
];
