/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import { AbsenceReason } from '@/types/enums/absenceReason';

export const ABSENCE_REASON_OPTION_VALUES: AbsenceReason[] = [
  AbsenceReason.VACATION,
  AbsenceReason.PERSONAL,
  AbsenceReason.TRAINING,
  AbsenceReason.OTHER,
];

export function getAbsenceReasonLabelKey(reason: AbsenceReason): string {
  return `myArea.absence.reason.${reason}`;
}
