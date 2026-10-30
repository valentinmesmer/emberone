/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useMemo, useState } from 'react';
import { useTranslation } from '@/hooks/useTranslation';
import { getMyAreaWorkflowInboxPath } from '@/lib/my-area/myAreaSections';
import {
  useJoinableOrganizations,
  useMainOrganizationChangeTargets,
  useMyBeginnableWorkflowTypes,
  useOrganizationGroupLeaveTargets,
  useOrganizationLeaveTargets,
  useOrganizationTree,
  useSelfOnboardingInvitations,
  useWorkflowOpenState,
} from '@/lib/store';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';
import type { OrganizationMembershipRequestActionsProps } from './OrganizationMembershipRequestActions';

export function useMyAreaOrganizationRequests() {
  const t = useTranslation();
  const { data: organizationTree } = useOrganizationTree();
  const { data: selfOnboardingInvitations = [] } = useSelfOnboardingInvitations();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();
  const { data: mainOrganizationChangeOpenState } = useWorkflowOpenState(
    WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE,
  );
  const organizationGroupJoinBeginnableForState = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_JOIN_REQUEST,
      ),
    [beginnableTypes],
  );
  const { data: organizationGroupJoinOpenState } = useWorkflowOpenState(
    WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_JOIN_REQUEST,
    organizationGroupJoinBeginnableForState,
  );
  const hasActiveOnboardingInvitation = useMemo(
    () =>
      selfOnboardingInvitations.some(
        (invitation) => invitation.status === 'OPEN' || invitation.status === 'SUBMITTED',
      ),
    [selfOnboardingInvitations],
  );

  const organizationJoinEligible = !hasActiveOnboardingInvitation;
  const { data: joinableOrganizations = [], isLoading: joinableOrganizationsLoading } =
    useJoinableOrganizations(organizationJoinEligible);
  const { data: organizationJoinOpenState } = useWorkflowOpenState(
    WORKFLOW_TYPE_IDS.ORGANIZATION_JOIN_REQUEST,
    organizationJoinEligible,
  );

  const [organizationJoinOpen, setOrganizationJoinOpen] = useState(false);
  const [organizationJoinModalKey, setOrganizationJoinModalKey] = useState(0);
  const [mainOrganizationChangeOpen, setMainOrganizationChangeOpen] = useState(false);
  const [mainOrganizationChangeModalKey, setMainOrganizationChangeModalKey] = useState(0);
  const [organizationLeaveOpen, setOrganizationLeaveOpen] = useState(false);
  const [organizationLeaveModalKey, setOrganizationLeaveModalKey] = useState(0);
  const [organizationGroupLeaveOpen, setOrganizationGroupLeaveOpen] = useState(false);
  const [organizationGroupLeaveModalKey, setOrganizationGroupLeaveModalKey] = useState(0);
  const [organizationGroupJoinOpen, setOrganizationGroupJoinOpen] = useState(false);
  const [organizationGroupJoinModalKey, setOrganizationGroupJoinModalKey] = useState(0);

  const organizationJoinTypeBlocked = organizationJoinOpenState?.typeBlocked ?? false;
  const organizationJoinNoTargetsBlocked =
    joinableOrganizationsLoading || joinableOrganizations.length === 0;
  /** ORGANIZATION_JOIN_REQUEST („Eintritt beantragen“) - not onboarding submission („Eintritt einreichen“). */
  const organizationJoinBeginnable =
    organizationJoinEligible &&
    beginnableTypes.some(
      (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_JOIN_REQUEST,
    );

  const mainOrganizationChangeBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.MAIN_ORGANIZATION_CHANGE,
      ),
    [beginnableTypes],
  );
  const mainOrganizationChangeOpenBlocked = mainOrganizationChangeOpenState?.typeBlocked ?? false;
  const { data: mainChangeTargets = [], isLoading: mainChangeTargetsLoading } =
    useMainOrganizationChangeTargets(mainOrganizationChangeBeginnable);
  const mainOrganizationChangeNoTargetsBlocked =
    mainChangeTargetsLoading || mainChangeTargets.length === 0;
  const mainOrganizationChangeBlocked =
    mainOrganizationChangeOpenBlocked || mainOrganizationChangeNoTargetsBlocked;

  const organizationLeaveBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_LEAVE_REQUEST,
      ),
    [beginnableTypes],
  );
  const { data: organizationLeaveTargets = [], isLoading: organizationLeaveTargetsLoading } =
    useOrganizationLeaveTargets(organizationLeaveBeginnable);
  const organizationLeaveBlocked =
    organizationLeaveTargetsLoading || organizationLeaveTargets.length === 0;

  const organizationGroupLeaveBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_LEAVE_REQUEST,
      ),
    [beginnableTypes],
  );
  const {
    data: organizationGroupLeaveTargets = [],
    isLoading: organizationGroupLeaveTargetsLoading,
  } = useOrganizationGroupLeaveTargets(organizationGroupLeaveBeginnable);
  const organizationGroupLeaveBlocked =
    organizationGroupLeaveTargetsLoading || organizationGroupLeaveTargets.length === 0;

  const organizationGroupJoinBeginnable = useMemo(
    () =>
      beginnableTypes.some(
        (type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.ORGANIZATION_GROUP_JOIN_REQUEST,
      ),
    [beginnableTypes],
  );
  const organizationGroupJoinTypeBlocked = organizationGroupJoinOpenState?.typeBlocked ?? false;

  const hasOrganizations = Boolean(organizationTree && organizationTree.length > 0);

  const actionProps: OrganizationMembershipRequestActionsProps = {
    onRequestOrganizationJoin:
      organizationJoinEligible && !organizationJoinTypeBlocked
        ? () => {
            if (organizationJoinNoTargetsBlocked) return;
            setOrganizationJoinModalKey((k) => k + 1);
            setOrganizationJoinOpen(true);
          }
        : undefined,
    organizationJoinRequestDisabled:
      !organizationJoinTypeBlocked && organizationJoinNoTargetsBlocked,
    organizationJoinRequestDisabledTitle: organizationJoinTypeBlocked
      ? t('myArea.organizationJoinRequest.blockedOpenRequest')
      : organizationJoinNoTargetsBlocked
        ? t('myArea.organizationJoinRequest.blockedNoJoinableOrganizations')
        : undefined,
    organizationJoinWorkflowInboxHref: organizationJoinTypeBlocked
      ? getMyAreaWorkflowInboxPath()
      : undefined,
    onRequestMainOrganizationChange:
      hasOrganizations && mainOrganizationChangeBeginnable
        ? () => {
            if (mainOrganizationChangeBlocked) return;
            setMainOrganizationChangeModalKey((k) => k + 1);
            setMainOrganizationChangeOpen(true);
          }
        : undefined,
    mainOrganizationChangeDisabled: mainOrganizationChangeBlocked,
    mainOrganizationChangeDisabledTitle: mainOrganizationChangeOpenBlocked
      ? t('myArea.mainOrganizationChange.blockedOpenRequest')
      : mainOrganizationChangeNoTargetsBlocked
        ? t('myArea.mainOrganizationChange.blockedNoTargetOrganizations')
        : undefined,
    onRequestOrganizationGroupJoin:
      hasOrganizations && organizationGroupJoinBeginnable
        ? () => {
            if (organizationGroupJoinTypeBlocked) return;
            setOrganizationGroupJoinModalKey((k) => k + 1);
            setOrganizationGroupJoinOpen(true);
          }
        : undefined,
    organizationGroupJoinRequestDisabled: organizationGroupJoinTypeBlocked,
    organizationGroupJoinRequestDisabledTitle: organizationGroupJoinTypeBlocked
      ? t('myArea.organizationGroupJoinRequest.blockedOpenRequest')
      : undefined,
    organizationGroupJoinWorkflowInboxHref: organizationGroupJoinTypeBlocked
      ? getMyAreaWorkflowInboxPath()
      : undefined,
    onRequestOrganizationGroupLeave:
      hasOrganizations && organizationGroupLeaveBeginnable
        ? () => {
            if (organizationGroupLeaveBlocked) return;
            setOrganizationGroupLeaveModalKey((k) => k + 1);
            setOrganizationGroupLeaveOpen(true);
          }
        : undefined,
    organizationGroupLeaveRequestDisabled: organizationGroupLeaveBlocked,
    organizationGroupLeaveRequestDisabledTitle: organizationGroupLeaveBlocked
      ? t('myArea.organizationGroupLeaveRequest.blockedNoLeaveTargets')
      : undefined,
    onRequestOrganizationLeave:
      hasOrganizations && organizationLeaveBeginnable
        ? () => {
            if (organizationLeaveBlocked) return;
            setOrganizationLeaveModalKey((k) => k + 1);
            setOrganizationLeaveOpen(true);
          }
        : undefined,
    organizationLeaveRequestDisabled: organizationLeaveBlocked,
    organizationLeaveRequestDisabledTitle: organizationLeaveBlocked
      ? t('myArea.organizationLeaveRequest.blockedNoLeaveTargets')
      : undefined,
  };

  return {
    organizationTree: organizationTree || [],
    hasOrganizations,
    organizationJoinEligible,
    organizationJoinBeginnable,
    mainOrganizationChangeBeginnable,
    organizationGroupJoinBeginnable,
    organizationGroupLeaveBeginnable,
    organizationLeaveBeginnable,
    actionProps,
    modals: {
      organizationJoinOpen,
      setOrganizationJoinOpen,
      organizationJoinModalKey,
      mainOrganizationChangeOpen,
      setMainOrganizationChangeOpen,
      mainOrganizationChangeModalKey,
      organizationLeaveOpen,
      setOrganizationLeaveOpen,
      organizationLeaveModalKey,
      organizationGroupLeaveOpen,
      setOrganizationGroupLeaveOpen,
      organizationGroupLeaveModalKey,
      organizationGroupJoinOpen,
      setOrganizationGroupJoinOpen,
      organizationGroupJoinModalKey,
    },
  };
}
