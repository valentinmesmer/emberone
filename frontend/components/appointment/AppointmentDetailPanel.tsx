/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { AppointmentAttachments } from '@/components/appointment/AppointmentAttachments';
import { AppointmentParticipationStats } from '@/components/appointment/AppointmentParticipationStats';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import { useOrganizationMembership } from '@/hooks/useWorkspaceOrganizations';
import {
  useAppointmentParticipationStats,
  useMyAppointmentResponses,
  useSubmitAppointmentResponse,
} from '@/lib/store';
import { formatAppointmentDateTimeRange } from '@/lib/utils/date';
import { isResponseDeadlinePassed } from '@/lib/utils/responseDeadline';
import {
  statusDangerSolidButtonClass,
  statusSuccessSolidButtonClass,
} from '@/lib/utils/statusActionClasses';
import {
  type AppointmentDto,
  AppointmentResponseMode,
  AppointmentResponseValue,
  AudienceScope,
  type MyAppointmentResponseDto,
} from '@/types/appointment';

/** Selected response uses the same colors as the row tint in the list (green = yes, red = no, gray = uncertain). */
const selectedResponseClasses: Record<AppointmentResponseValue, string> = {
  [AppointmentResponseValue.YES]: statusSuccessSolidButtonClass,
  [AppointmentResponseValue.NO]: statusDangerSolidButtonClass,
  [AppointmentResponseValue.UNCERTAIN]: 'border-gray-500 bg-gray-500 hover:bg-gray-600 text-white',
};

interface AppointmentDetailPanelProps {
  organizationId: number;
  appointment: AppointmentDto;
  canWrite: boolean;
}

export function AppointmentDetailPanel({
  organizationId,
  appointment,
  canWrite,
}: AppointmentDetailPanelProps) {
  const t = useTranslation();
  const { showError, showSuccess } = useToast();
  const [draftComment, setDraftComment] = useState<{ appointmentId: number; value: string } | null>(
    null,
  );
  const submitResponseMutation = useSubmitAppointmentResponse();

  const { isMember } = useOrganizationMembership(organizationId);
  const { data: myResponses = [] } = useMyAppointmentResponses(isMember ? [appointment.id] : []);
  const myResponse: MyAppointmentResponseDto | undefined = myResponses[0];

  const collectsResponses =
    appointment.responseMode !== AppointmentResponseMode.NONE && !appointment.archived;
  // Only memberships answer; the participation stats stay visible for managers.
  const canRespond = isMember && collectsResponses;
  const participationStatsQuery = useAppointmentParticipationStats(organizationId, appointment.id, {
    enabled: collectsResponses,
  });
  const participationStats = collectsResponses ? (participationStatsQuery.data ?? null) : null;
  const comment =
    draftComment?.appointmentId === appointment.id
      ? draftComment.value
      : (myResponse?.comment ?? '');
  const deadlinePassed = isResponseDeadlinePassed(
    appointment.start,
    appointment.responseDeadlineMinutesBefore,
  );
  const allowsComment =
    appointment.responseMode === AppointmentResponseMode.YES_NO_WITH_COMMENT ||
    appointment.responseMode === AppointmentResponseMode.YES_NO_UNCERTAIN_WITH_COMMENT;

  const selectedValue = myResponse?.value;
  const submitting = submitResponseMutation.isPending;

  const audienceLabel = useMemo(() => {
    if (appointment.audienceScope === AudienceScope.ALL) {
      return null;
    }
    const names = (appointment.audienceGroups ?? []).map((g) => g.name);
    return names.length > 0 ? names.join(', ') : t('appointments.audience.SELECTED_GROUPS');
  }, [appointment.audienceScope, appointment.audienceGroups, t]);

  const dateTimeRange = useMemo(
    () => formatAppointmentDateTimeRange(appointment.start, appointment.end),
    [appointment.start, appointment.end],
  );

  const submitResponse = async (value: AppointmentResponseValue, commentOverride?: string) => {
    if (deadlinePassed || submitting) return;
    try {
      const commentText = (commentOverride ?? comment).trim();
      await submitResponseMutation.mutateAsync({
        organizationId,
        appointmentId: appointment.id,
        data: {
          value,
          comment: commentText || undefined,
        },
      });
      setDraftComment(null);
      showSuccess(t('appointments.success.responseSubmitted'));
    } catch (e) {
      showError(e instanceof Error ? e.message : t('appointments.errors.responseFailed'));
    }
  };

  const handleCommentBlur = () => {
    if (!allowsComment || !selectedValue || deadlinePassed || submitting) return;
    const trimmed = comment.trim();
    const existing = myResponse?.comment?.trim() ?? '';
    if (trimmed === existing) return;
    void submitResponse(selectedValue, trimmed);
  };

  const location = appointment.location?.trim();
  const description = appointment.description?.trim();

  const uncertainMode =
    appointment.responseMode === AppointmentResponseMode.YES_NO_UNCERTAIN ||
    appointment.responseMode === AppointmentResponseMode.YES_NO_UNCERTAIN_WITH_COMMENT;

  return (
    <div className="space-y-4 text-sm">
      <div className="space-y-1.5">
        <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <span className="text-sm text-[var(--text-primary)]">{dateTimeRange}</span>
        </div>
        {location && <p className="text-sm text-[var(--text-secondary)]">{location}</p>}
        {description && (
          <p className="text-sm text-[var(--text-primary)] whitespace-pre-wrap">{description}</p>
        )}
        {audienceLabel && <p className="text-xs text-[var(--text-secondary)]">{audienceLabel}</p>}
      </div>

      <AppointmentAttachments
        organizationId={organizationId}
        appointmentId={appointment.id}
        canManage={canWrite}
        compact
      />

      {canRespond && (
        <section className="space-y-2 rounded-lg border border-[var(--border-color)] p-3">
          <p className="text-sm font-semibold text-[var(--text-primary)]">
            {t('appointments.response.title')}
          </p>

          {deadlinePassed ? (
            <p className="text-sm text-[var(--text-secondary)]">
              {t('appointments.detail.deadlinePassed')}
            </p>
          ) : (
            <>
              <div className={`grid gap-2 ${uncertainMode ? 'grid-cols-3' : 'grid-cols-2'}`}>
                <Button
                  type="button"
                  variant="secondary"
                  className={
                    selectedValue === AppointmentResponseValue.YES
                      ? selectedResponseClasses[AppointmentResponseValue.YES]
                      : undefined
                  }
                  fullWidth
                  disabled={submitting}
                  onClick={() => submitResponse(AppointmentResponseValue.YES)}
                >
                  {t('common.yes')}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  className={
                    selectedValue === AppointmentResponseValue.NO
                      ? selectedResponseClasses[AppointmentResponseValue.NO]
                      : undefined
                  }
                  fullWidth
                  disabled={submitting}
                  onClick={() => submitResponse(AppointmentResponseValue.NO)}
                >
                  {t('common.no')}
                </Button>
                {uncertainMode && (
                  <Button
                    type="button"
                    variant="secondary"
                    className={
                      selectedValue === AppointmentResponseValue.UNCERTAIN
                        ? selectedResponseClasses[AppointmentResponseValue.UNCERTAIN]
                        : undefined
                    }
                    fullWidth
                    disabled={submitting}
                    onClick={() => submitResponse(AppointmentResponseValue.UNCERTAIN)}
                  >
                    {t('appointments.response.uncertain')}
                  </Button>
                )}
              </div>
              {allowsComment && (
                <Textarea
                  label={t('appointments.response.comment')}
                  value={comment}
                  onChange={(e) =>
                    setDraftComment({ appointmentId: appointment.id, value: e.target.value })
                  }
                  onBlur={handleCommentBlur}
                  disabled={submitting}
                  rows={2}
                  className="min-h-[76px] rounded-lg px-3 py-2 text-sm"
                />
              )}
            </>
          )}
        </section>
      )}

      {collectsResponses && participationStats && (
        <AppointmentParticipationStats stats={participationStats} showUncertain={uncertainMode} />
      )}
    </div>
  );
}
