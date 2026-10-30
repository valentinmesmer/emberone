/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import type { ReactNode } from 'react';
import {
  formatOfferingCoursePeriod,
  formatOfferingRegistrationPeriod,
} from '@/components/my-area/courseOfferings/selfCourseOfferingDisplay';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { DetailCardHeader } from '@/components/ui/detail-card-header';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow, ScheduleMetaRow } from '@/components/ui/list';
import { SectionHeader } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { courseSessionTypeLabelKey } from '@/lib/utils/courseOffering';
import { formatAppointmentDateTimeRange } from '@/lib/utils/date';
import type { SelfCourseOfferingDto } from '@/types/courseOffering';

interface OfferingProps {
  offering: SelfCourseOfferingDto;
}

function Fact({
  label,
  children,
  wide = false,
}: {
  label: string;
  children: ReactNode;
  wide?: boolean;
}) {
  return (
    <div className={wide ? 'col-span-2 sm:col-span-1' : undefined}>
      <dt className="text-xs text-[var(--text-secondary)]">{label}</dt>
      <dd className="mt-0.5 text-sm font-medium text-[var(--text-primary)]">{children}</dd>
    </div>
  );
}

/** Title, state chips and the key facts of the offering. */
export function SelfCourseOfferingSummaryCard({ offering }: OfferingProps) {
  const t = useTranslation();

  return (
    <Card className="p-3 sm:p-4">
      <DetailCardHeader
        className="mb-4"
        title={offering.trainingCourseTypeName}
        eyebrow={offering.organizationName}
        badges={
          <>
            {offering.alreadyHeld ? (
              <Badge tone="accent" pill>
                {t('myArea.courseRegistrations.offerings.alreadyHeld')}
              </Badge>
            ) : null}
            {!offering.prerequisitesMet && !offering.myRegistration ? (
              <Badge tone="warning" pill>
                {t('myArea.courseRegistrations.offerings.prerequisitesMissing')}
              </Badge>
            ) : null}
          </>
        }
      />
      <dl className="grid grid-cols-2 gap-x-4 gap-y-3">
        <Fact label={t('myArea.courseRegistrations.detail.coursePeriod')} wide>
          {formatOfferingCoursePeriod(offering, t)}
        </Fact>
        <Fact label={t('myArea.courseRegistrations.detail.registrationPeriod')} wide>
          {formatOfferingRegistrationPeriod(offering, t)}
        </Fact>
        <Fact label={t('myArea.courseRegistrations.detail.maxParticipants')}>
          {offering.maxParticipants}
        </Fact>
        <Fact label={t('myArea.courseRegistrations.detail.hasExam')}>
          {offering.hasExam ? t('common.yes') : t('common.no')}
        </Fact>
        {offering.requiresHomeOrgApproval ? (
          <Fact label={t('myArea.courseRegistrations.detail.homeOrgApproval')}>
            {t('common.yes')}
          </Fact>
        ) : null}
      </dl>
    </Card>
  );
}

export function SelfCourseOfferingSessionsCard({ offering }: OfferingProps) {
  const t = useTranslation();

  return (
    <Card className="p-3 sm:p-4">
      <SectionHeader title={t('courseOfferings.sessions.title')} />
      {offering.sessions.length === 0 ? (
        <p className="mt-2 text-sm text-[var(--text-secondary)]">
          {t('courseOfferings.sessions.empty')}
        </p>
      ) : (
        <ListContainer className="mt-3" variant="inset">
          {offering.sessions.map((session) => (
            <ListRow
              key={session.id}
              accent={false}
              chevron={false}
              title={t(courseSessionTypeLabelKey(session.type))}
              meta={
                <>
                  <ScheduleMetaRow
                    when={formatAppointmentDateTimeRange(session.start, session.end)}
                  />
                  {session.location ? (
                    <div className="mt-0.5 text-xs text-[var(--text-secondary)]">
                      {session.location}
                    </div>
                  ) : null}
                  {session.notes ? (
                    <div className="mt-0.5 text-xs text-[var(--text-secondary)]">
                      {session.notes}
                    </div>
                  ) : null}
                </>
              }
            />
          ))}
        </ListContainer>
      )}
    </Card>
  );
}

/** Free-text content and notes; renders nothing when both are empty. */
export function SelfCourseOfferingInfoCard({ offering }: OfferingProps) {
  const t = useTranslation();

  if (!offering.content && !offering.notes) {
    return null;
  }

  return (
    <Card className="space-y-3 p-3 sm:p-4">
      {offering.content ? (
        <FieldDisplay
          label={t('myArea.courseRegistrations.detail.content')}
          value={offering.content}
          multiline
        />
      ) : null}
      {offering.notes ? (
        <FieldDisplay
          label={t('myArea.courseRegistrations.detail.notes')}
          value={offering.notes}
          multiline
        />
      ) : null}
    </Card>
  );
}
