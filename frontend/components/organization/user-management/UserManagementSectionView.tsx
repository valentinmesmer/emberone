/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Plus } from 'lucide-react';
import { Suspense, useMemo, useState } from 'react';
import { Button } from '@/components/ui/button';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { PageHeader } from '@/components/ui/page-header';
import { useTranslation } from '@/hooks/useTranslation';
import {
  USER_MANAGEMENT_SECTIONS,
  type UserManagementSectionId,
} from '@/lib/navigation/userManagementSections';
import {
  useCanAssignCertifications,
  useUserDrivingLicense,
  useUserDrivingLicensePendingWorkflow,
  useUserMembershipOverview,
  useUsersForOrganization,
} from '@/lib/store';
import { partitionOrganizationMemberships } from '@/lib/utils/membershipPartition';
import { UserAbsencesSection } from './UserAbsencesSection';
import { UserCertificationsSection } from './UserCertificationsSection';
import { UserCourseRegistrationsSection } from './UserCourseRegistrationsSection';
import { UserDocumentsSection } from './UserDocumentsSection';
import { UserDrivingLicensesSection } from './UserDrivingLicensesSection';
import { UserEmergencyContactsSection } from './UserEmergencyContactsSection';
import { UserManagementSectionGate } from './UserManagementSectionGate';
import { UserMasterDataSection } from './UserMasterDataSection';
import { UserMembershipsSection } from './UserMembershipsSection';
import { UserOnboardingSection } from './UserOnboardingSection';
import { UserQualificationsSection } from './UserQualificationsSection';
import { UserRankSection } from './UserRankSection';
import { UserTimeEntriesSection } from './UserTimeEntriesSection';
import { UserTrainingCoursesSection } from './UserTrainingCoursesSection';
import { UserWorkflowsSection } from './UserWorkflowsSection';

const SECTIONS_WITH_ADD = new Set<UserManagementSectionId>([
  'documents',
  'certifications',
  'training-courses',
  'driving-licenses',
  'absences',
  'emergency-contacts',
  'time-entries',
]);

interface UserManagementSectionViewProps {
  organizationId: number;
  userId: number;
  sectionId: UserManagementSectionId;
}

function SectionContent({
  organizationId,
  userId,
  sectionId,
  canWrite,
  isAddOpen,
  onAddClose,
  isSetRankOpen,
  onSetRankClose,
  isMainOrganization,
  user,
}: {
  organizationId: number;
  userId: number;
  sectionId: UserManagementSectionId;
  canWrite: boolean;
  isAddOpen: boolean;
  onAddClose: () => void;
  isSetRankOpen: boolean;
  onSetRankClose: () => void;
  isMainOrganization: boolean;
  user: { firstName: string; lastName: string };
}) {
  switch (sectionId) {
    case 'master-data':
      return (
        <UserMasterDataSection
          organizationId={organizationId}
          userId={userId}
          canWrite={canWrite}
        />
      );
    case 'memberships':
      return (
        <UserMembershipsSection
          organizationId={organizationId}
          userId={userId}
          canWrite={canWrite}
        />
      );
    case 'emergency-contacts':
      return (
        <UserEmergencyContactsSection
          organizationId={organizationId}
          userId={userId}
          canWrite={canWrite}
          isAddOpen={isAddOpen}
          onAddClose={onAddClose}
        />
      );
    case 'documents':
      return (
        <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
          <UserDocumentsSection
            organizationId={organizationId}
            userId={userId}
            canWrite={canWrite}
            isAddOpen={isAddOpen}
            onAddClose={onAddClose}
          />
        </Suspense>
      );
    case 'certifications':
      return (
        <UserCertificationsSection
          organizationId={organizationId}
          userId={userId}
          canWrite={canWrite}
          isAddOpen={isAddOpen}
          onAddClose={onAddClose}
        />
      );
    case 'qualifications':
      return (
        <UserQualificationsSection
          organizationId={organizationId}
          userId={userId}
          canWrite={canWrite}
        />
      );
    case 'rank':
      return (
        <UserRankSection
          organizationId={organizationId}
          userId={userId}
          firstName={user.firstName}
          lastName={user.lastName}
          canWrite={canWrite}
          isMainOrganization={isMainOrganization}
          setRankOpen={isSetRankOpen}
          onSetRankClose={onSetRankClose}
        />
      );
    case 'training-courses':
      return (
        <UserTrainingCoursesSection
          organizationId={organizationId}
          userId={userId}
          canWrite={canWrite}
          isAddOpen={isAddOpen}
          onAddClose={onAddClose}
        />
      );
    case 'course-registrations':
      return <UserCourseRegistrationsSection organizationId={organizationId} userId={userId} />;
    case 'driving-licenses':
      return (
        <UserDrivingLicensesSection
          organizationId={organizationId}
          userId={userId}
          canWrite={canWrite}
          isAddOpen={isAddOpen}
          onAddClose={onAddClose}
        />
      );
    case 'onboarding':
      return <UserOnboardingSection organizationId={organizationId} userId={userId} />;
    case 'workflows':
      return (
        <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
          <UserWorkflowsSection organizationId={organizationId} userId={userId} />
        </Suspense>
      );
    case 'absences':
      return (
        <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
          <UserAbsencesSection
            organizationId={organizationId}
            userId={userId}
            canWrite={canWrite}
            isAddOpen={isAddOpen}
            onAddClose={onAddClose}
          />
        </Suspense>
      );
    case 'time-entries':
      return (
        <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
          <UserTimeEntriesSection
            organizationId={organizationId}
            userId={userId}
            canWrite={canWrite}
            isAddOpen={isAddOpen}
            onAddClose={onAddClose}
          />
        </Suspense>
      );
  }
}

export function UserManagementSectionView({
  organizationId,
  userId,
  sectionId,
}: UserManagementSectionViewProps) {
  const t = useTranslation();
  const { data: users = [], isLoading } = useUsersForOrganization(organizationId);
  const isRankSection = sectionId === 'rank';
  const isCertificationsSection = sectionId === 'certifications';
  const isDrivingLicensesSection = sectionId === 'driving-licenses';
  const { data: rankMembershipOverview } = useUserMembershipOverview(
    isRankSection ? organizationId : null,
    isRankSection ? userId : null,
  );
  const canSetUserRank = useMemo(() => {
    if (!rankMembershipOverview) {
      return false;
    }
    const { active } = partitionOrganizationMemberships(
      rankMembershipOverview.organizationMemberships,
    );
    return active[0]?.main === true;
  }, [rankMembershipOverview]);
  const { canAssign: canAssignCertifications, isLoading: canAssignLoading } =
    useCanAssignCertifications(organizationId, userId, isCertificationsSection);
  const { data: pendingDrivingLicenseWorkflow, isLoading: pendingDrivingLicenseLoading } =
    useUserDrivingLicensePendingWorkflow(organizationId, userId, isDrivingLicensesSection);
  const { data: drivingLicense } = useUserDrivingLicense(
    organizationId,
    userId,
    isDrivingLicensesSection,
  );
  const drivingLicenseAddUnavailable =
    isDrivingLicensesSection &&
    (pendingDrivingLicenseLoading || pendingDrivingLicenseWorkflow != null);
  const addDisabled =
    isCertificationsSection && (canAssignLoading || canAssignCertifications === false);
  const section = USER_MANAGEMENT_SECTIONS.find((item) => item.id === sectionId);
  const user = users.find((item) => item.id === userId);
  const [prevSectionId, setPrevSectionId] = useState(sectionId);
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [isSetRankOpen, setIsSetRankOpen] = useState(false);

  if (prevSectionId !== sectionId) {
    setPrevSectionId(sectionId);
    setIsAddOpen(false);
    setIsSetRankOpen(false);
  }

  if (!section) {
    return <NotFoundState message={t('organizationManagement.errors.notFound')} />;
  }

  if (isLoading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!user) {
    return <NotFoundState message={t('userManagement.errors.notFound')} />;
  }

  const displayName = `${user.firstName} ${user.lastName}`.trim();

  return (
    <UserManagementSectionGate organizationId={organizationId} sectionId={sectionId}>
      {({ canWrite }) => (
        <>
          <PageHeader
            title={t(section.labelKey)}
            description={displayName}
            size="compact"
            action={
              isRankSection && canWrite && canSetUserRank ? (
                <Button
                  type="button"
                  size="xs"
                  onClick={() => setIsSetRankOpen(true)}
                  fullWidth="responsive"
                >
                  {t('ranks.overview.setRank')}
                </Button>
              ) : canWrite && SECTIONS_WITH_ADD.has(sectionId) && !drivingLicenseAddUnavailable ? (
                <Button
                  type="button"
                  size="xs"
                  onClick={() => setIsAddOpen(true)}
                  fullWidth="responsive"
                  disabled={addDisabled}
                  title={addDisabled ? t('common.hoverDisabledAction') : undefined}
                >
                  <Plus className="mr-1 h-3.5 w-3.5" aria-hidden />
                  {isDrivingLicensesSection
                    ? drivingLicense != null
                      ? t('drivingLicense.record.renewAction')
                      : t('drivingLicense.record.createAction')
                    : t('common.add')}
                </Button>
              ) : undefined
            }
          />
          <div className="mt-1 w-full sm:mt-0">
            <SectionContent
              organizationId={organizationId}
              userId={userId}
              sectionId={sectionId}
              canWrite={canWrite}
              isAddOpen={isAddOpen}
              onAddClose={() => setIsAddOpen(false)}
              isSetRankOpen={isSetRankOpen}
              onSetRankClose={() => setIsSetRankOpen(false)}
              isMainOrganization={canSetUserRank}
              user={user}
            />
          </div>
        </>
      )}
    </UserManagementSectionGate>
  );
}
