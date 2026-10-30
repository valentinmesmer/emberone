/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import Link from 'next/link';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { SectionActions } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

export interface OrganizationMembershipRequestActionsProps {
  onRequestOrganizationJoin?: () => void;
  organizationJoinRequestDisabled?: boolean;
  organizationJoinRequestDisabledTitle?: string;
  organizationJoinWorkflowInboxHref?: string;
  onRequestMainOrganizationChange?: () => void;
  mainOrganizationChangeDisabled?: boolean;
  mainOrganizationChangeDisabledTitle?: string;
  onRequestOrganizationLeave?: () => void;
  organizationLeaveRequestDisabled?: boolean;
  organizationLeaveRequestDisabledTitle?: string;
  onRequestOrganizationGroupJoin?: () => void;
  organizationGroupJoinRequestDisabled?: boolean;
  organizationGroupJoinRequestDisabledTitle?: string;
  /** When a group-join workflow is already open, link to the workflow inbox. */
  organizationGroupJoinWorkflowInboxHref?: string;
  onRequestOrganizationGroupLeave?: () => void;
  organizationGroupLeaveRequestDisabled?: boolean;
  organizationGroupLeaveRequestDisabledTitle?: string;
}

export function OrganizationMembershipRequestActions({
  onRequestOrganizationJoin,
  organizationJoinRequestDisabled = false,
  organizationJoinRequestDisabledTitle,
  organizationJoinWorkflowInboxHref,
  onRequestMainOrganizationChange,
  mainOrganizationChangeDisabled = false,
  mainOrganizationChangeDisabledTitle,
  onRequestOrganizationLeave,
  organizationLeaveRequestDisabled = false,
  organizationLeaveRequestDisabledTitle,
  onRequestOrganizationGroupJoin,
  organizationGroupJoinRequestDisabled = false,
  organizationGroupJoinRequestDisabledTitle,
  organizationGroupJoinWorkflowInboxHref,
  onRequestOrganizationGroupLeave,
  organizationGroupLeaveRequestDisabled = false,
  organizationGroupLeaveRequestDisabledTitle,
}: OrganizationMembershipRequestActionsProps) {
  const t = useTranslation();

  const showOrganizationActions = Boolean(
    onRequestOrganizationJoin ||
      organizationJoinWorkflowInboxHref ||
      onRequestMainOrganizationChange ||
      onRequestOrganizationLeave,
  );
  const showGroupActions = Boolean(
    onRequestOrganizationGroupJoin ||
      onRequestOrganizationGroupLeave ||
      organizationGroupJoinWorkflowInboxHref,
  );

  if (!showOrganizationActions && !showGroupActions) {
    return null;
  }

  return (
    <Card className="p-3 sm:p-4">
      <h2 className="text-base font-semibold text-[var(--text-primary)]">
        {t('myArea.masterData.organizationRequestsSection')}
      </h2>
      <p className="mt-1 text-sm text-[var(--text-secondary)]">
        {t('myArea.masterData.organizationRequestsHint')}
      </p>
      <div className="mt-4 space-y-4 border-t border-[var(--border-color)] pt-4">
        {showOrganizationActions ? (
          <div>
            <p className="mb-2 text-xs font-medium text-[var(--text-secondary)]">
              {t('myArea.masterData.organizationRequestsOrganizationHeading')}
            </p>
            <SectionActions>
              {organizationJoinWorkflowInboxHref ? (
                <Link
                  href={organizationJoinWorkflowInboxHref}
                  className={cn(
                    'inline-flex min-h-9 w-full items-center justify-center rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] px-3 text-sm font-medium text-[var(--text-primary)] transition-colors hover:bg-[var(--hover-bg)] sm:w-auto',
                  )}
                >
                  {t('myArea.masterData.openWorkflowsButton')}
                </Link>
              ) : onRequestOrganizationJoin ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onRequestOrganizationJoin}
                  disabled={organizationJoinRequestDisabled}
                  title={organizationJoinRequestDisabledTitle}
                >
                  {t('myArea.organizationJoinRequest.requestButton')}
                </Button>
              ) : null}
              {onRequestMainOrganizationChange ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onRequestMainOrganizationChange}
                  disabled={mainOrganizationChangeDisabled}
                  title={mainOrganizationChangeDisabledTitle}
                >
                  {t('myArea.mainOrganizationChange.requestButton')}
                </Button>
              ) : null}
              {onRequestOrganizationLeave ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onRequestOrganizationLeave}
                  disabled={organizationLeaveRequestDisabled}
                  title={organizationLeaveRequestDisabledTitle}
                >
                  {t('myArea.organizationLeaveRequest.requestButton')}
                </Button>
              ) : null}
            </SectionActions>
          </div>
        ) : null}
        {showGroupActions ? (
          <div>
            <p className="mb-2 text-xs font-medium text-[var(--text-secondary)]">
              {t('myArea.masterData.organizationRequestsGroupsHeading')}
            </p>
            <SectionActions>
              {organizationGroupJoinWorkflowInboxHref ? (
                <Link
                  href={organizationGroupJoinWorkflowInboxHref}
                  className={cn(
                    'inline-flex min-h-9 w-full items-center justify-center rounded-lg border border-[var(--border-color)] bg-[var(--card-bg)] px-3 text-sm font-medium text-[var(--text-primary)] transition-colors hover:bg-[var(--hover-bg)] sm:w-auto',
                  )}
                >
                  {t('myArea.masterData.openWorkflowsButton')}
                </Link>
              ) : onRequestOrganizationGroupJoin ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onRequestOrganizationGroupJoin}
                  disabled={organizationGroupJoinRequestDisabled}
                  title={organizationGroupJoinRequestDisabledTitle}
                >
                  {t('myArea.organizationGroupJoinRequest.requestButton')}
                </Button>
              ) : null}
              {onRequestOrganizationGroupLeave ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onRequestOrganizationGroupLeave}
                  disabled={organizationGroupLeaveRequestDisabled}
                  title={organizationGroupLeaveRequestDisabledTitle}
                >
                  {t('myArea.organizationGroupLeaveRequest.requestButton')}
                </Button>
              ) : null}
            </SectionActions>
          </div>
        ) : null}
      </div>
    </Card>
  );
}
