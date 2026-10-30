/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

/**
 * Shared UI tone unions - kept outside Badge/List components so lib helpers
 * (e.g. workflowLabels) can depend on tones without creating import cycles
 * through the component modules.
 */

export type BadgeTone = 'neutral' | 'accent' | 'success' | 'danger' | 'warning';

export type ListRowTone =
  | 'default'
  | 'success'
  | 'danger'
  | 'neutral'
  | 'warning'
  | 'active'
  | 'info';
