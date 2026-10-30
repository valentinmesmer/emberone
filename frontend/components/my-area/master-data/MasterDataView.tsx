/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useRouter, useSearchParams } from 'next/navigation';
import { useEffect, useMemo, useRef, useState } from 'react';
import {
  AddressChangeRequestModal,
  EditNotificationEmailModal,
  NameChangeRequestModal,
  NotificationEmailCard,
  OrganizationMembershipsCard,
  PersonalInformationCard,
} from '@/components/my-area';
import { MyAreaOrganizationRequestModals } from '@/components/my-area/organizations/MyAreaOrganizationRequestModals';
import { OrganizationMembershipRequestActions } from '@/components/my-area/organizations/OrganizationMembershipRequestActions';
import { useMyAreaOrganizationRequests } from '@/components/my-area/organizations/useMyAreaOrganizationRequests';
import { LoadingState } from '@/components/ui/loading';
import { ErrorState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useToast } from '@/components/ui/toast';
import { useTranslation } from '@/hooks/useTranslation';
import {
  useCancelPendingNotificationEmail,
  useMyBeginnableWorkflowTypes,
  useOrganizationTree,
  useResendNotificationEmailVerification,
  useSelf,
  useUpdateNotificationEmail,
  useVerifyNotificationEmail,
  useWorkflowOpenState,
} from '@/lib/store';
import { WORKFLOW_TYPE_IDS } from '@/types/workflow';

export function MasterDataView() {
  const t = useTranslation();
  const router = useRouter();
  const searchParams = useSearchParams();
  const { showError, showSuccess } = useToast();

  const { data: userData, isLoading, error: userError } = useSelf();
  const { data: organizationTree } = useOrganizationTree();
  const { data: beginnableTypes = [] } = useMyBeginnableWorkflowTypes();
  const { data: nameChangeOpenState } = useWorkflowOpenState(WORKFLOW_TYPE_IDS.NAME_CHANGE);
  const { data: addressChangeOpenState } = useWorkflowOpenState(WORKFLOW_TYPE_IDS.ADDRESS_CHANGE);
  const updateNotificationEmail = useUpdateNotificationEmail();
  const { mutateAsync: verifyNotificationEmail } = useVerifyNotificationEmail();
  const resendVerification = useResendNotificationEmailVerification();
  const cancelPending = useCancelPendingNotificationEmail();
  const organizationRequests = useMyAreaOrganizationRequests();

  const [isEmailModalOpen, setIsEmailModalOpen] = useState(false);
  const [nameChangeOpen, setNameChangeOpen] = useState(false);
  const [nameChangeModalKey, setNameChangeModalKey] = useState(0);
  const [addressChangeOpen, setAddressChangeOpen] = useState(false);
  const [addressChangeModalKey, setAddressChangeModalKey] = useState(0);
  const verifyEmailToken = searchParams.get('verifyEmailToken');
  const [isVerifyingEmail, setIsVerifyingEmail] = useState(() => !!verifyEmailToken);
  const verifyHandledRef = useRef<string | null>(null);

  const nameChangeBeginnable = useMemo(
    () => beginnableTypes.some((type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.NAME_CHANGE),
    [beginnableTypes],
  );
  const nameChangeBlocked = nameChangeOpenState?.typeBlocked ?? false;
  const addressChangeBeginnable = useMemo(
    () => beginnableTypes.some((type) => type.workflowTypeId === WORKFLOW_TYPE_IDS.ADDRESS_CHANGE),
    [beginnableTypes],
  );
  const addressChangeBlocked = addressChangeOpenState?.typeBlocked ?? false;

  useEffect(() => {
    if (!verifyEmailToken || verifyHandledRef.current === verifyEmailToken) {
      return;
    }
    verifyHandledRef.current = verifyEmailToken;
    setIsVerifyingEmail(true);

    void (async () => {
      try {
        await verifyNotificationEmail({ token: verifyEmailToken });
        showSuccess(t('myArea.success.notificationEmailVerified'));
      } catch (error) {
        showError(
          error instanceof Error ? error.message : t('myArea.error.notificationEmailVerifyFailed'),
        );
      } finally {
        setIsVerifyingEmail(false);
        router.replace('/my-area/master-data');
      }
    })();
  }, [verifyEmailToken, verifyNotificationEmail, router, showError, showSuccess, t]);

  const handleSaveNotificationEmail = async (privateEmail: string) => {
    await updateNotificationEmail.mutateAsync({ privateEmail });
    setIsEmailModalOpen(false);
  };

  const handleResendVerification = async () => {
    try {
      await resendVerification.mutateAsync();
      showSuccess(t('myArea.success.notificationEmailVerificationResent'));
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    }
  };

  const handleCancelPending = async () => {
    try {
      await cancelPending.mutateAsync();
      showSuccess(t('myArea.success.notificationEmailPendingCancelled'));
    } catch (error) {
      showError(error instanceof Error ? error.message : t('myArea.error.loadFailed'));
    }
  };

  const header = (
    <PageHeader
      title={t('myArea.masterData.title')}
      description={t('myArea.masterData.subtitle')}
      size="compact"
    />
  );

  const error =
    userError instanceof Error ? userError.message : userError ? String(userError) : null;
  if (error) {
    return (
      <>
        {header}
        <ErrorState message={t('myArea.error.loadFailed')} className="mt-1 sm:mt-0" />
      </>
    );
  }

  if (isLoading || isVerifyingEmail) {
    return (
      <>
        {header}
        <div className="mt-1 w-full sm:mt-0">
          <LoadingState variant="embedded" size="sm" />
        </div>
      </>
    );
  }

  if (!userData) {
    return null;
  }

  const contactInformation = userData.contactInformation || {
    id: 0,
    userId: userData.id,
    privateEmail: '',
    pendingEmail: null,
    street: undefined,
    city: undefined,
    postalCode: undefined,
    country: undefined,
  };
  const hasOrganizations = Boolean(organizationTree && organizationTree.length > 0);
  const showOrganizationCard = hasOrganizations || organizationRequests.organizationJoinEligible;

  return (
    <>
      {header}

      <div className="mt-1 w-full space-y-4 sm:mt-0">
        <PersonalInformationCard
          userData={userData}
          nameChangeRequestDisabled={nameChangeBlocked}
          onRequestNameChange={
            nameChangeBeginnable
              ? () => {
                  if (nameChangeBlocked) return;
                  setNameChangeModalKey((k) => k + 1);
                  setNameChangeOpen(true);
                }
              : undefined
          }
          addressChangeRequestDisabled={addressChangeBlocked}
          onRequestAddressChange={
            addressChangeBeginnable
              ? () => {
                  if (addressChangeBlocked) return;
                  setAddressChangeModalKey((k) => k + 1);
                  setAddressChangeOpen(true);
                }
              : undefined
          }
        />
        <NotificationEmailCard
          contactInformation={contactInformation}
          onEdit={() => setIsEmailModalOpen(true)}
          onResendVerification={
            contactInformation.pendingEmail ? handleResendVerification : undefined
          }
          onCancelPending={contactInformation.pendingEmail ? handleCancelPending : undefined}
          isResending={resendVerification.isPending}
          isCancelling={cancelPending.isPending}
        />

        {showOrganizationCard ? <OrganizationMembershipsCard /> : null}
        <OrganizationMembershipRequestActions {...organizationRequests.actionProps} />
      </div>

      <MyAreaOrganizationRequestModals
        userData={userData}
        organizationJoinBeginnable={organizationRequests.organizationJoinBeginnable}
        mainOrganizationChangeBeginnable={organizationRequests.mainOrganizationChangeBeginnable}
        organizationGroupJoinBeginnable={organizationRequests.organizationGroupJoinBeginnable}
        organizationGroupLeaveBeginnable={organizationRequests.organizationGroupLeaveBeginnable}
        organizationLeaveBeginnable={organizationRequests.organizationLeaveBeginnable}
        modals={organizationRequests.modals}
      />

      {nameChangeBeginnable ? (
        <NameChangeRequestModal
          key={`name-change-${nameChangeModalKey}`}
          isOpen={nameChangeOpen}
          onClose={() => setNameChangeOpen(false)}
          userData={userData}
        />
      ) : null}

      {addressChangeBeginnable ? (
        <AddressChangeRequestModal
          key={`address-change-${addressChangeModalKey}`}
          isOpen={addressChangeOpen}
          onClose={() => setAddressChangeOpen(false)}
          userData={userData}
        />
      ) : null}

      <EditNotificationEmailModal
        isOpen={isEmailModalOpen}
        onClose={() => setIsEmailModalOpen(false)}
        contactInformation={contactInformation}
        onSave={handleSaveNotificationEmail}
      />
    </>
  );
}
