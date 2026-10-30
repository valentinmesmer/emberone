/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import {
  MainOrganizationChangeModal,
  OrganizationGroupJoinRequestModal,
  OrganizationGroupLeaveRequestModal,
  OrganizationJoinRequestModal,
  OrganizationLeaveRequestModal,
} from '@/components/my-area';
import type { UserDto } from '@/types/user';

interface MyAreaOrganizationRequestModalsProps {
  userData: UserDto;
  organizationJoinBeginnable: boolean;
  mainOrganizationChangeBeginnable: boolean;
  organizationGroupJoinBeginnable: boolean;
  organizationGroupLeaveBeginnable: boolean;
  organizationLeaveBeginnable: boolean;
  modals: {
    organizationJoinOpen: boolean;
    setOrganizationJoinOpen: (open: boolean) => void;
    organizationJoinModalKey: number;
    mainOrganizationChangeOpen: boolean;
    setMainOrganizationChangeOpen: (open: boolean) => void;
    mainOrganizationChangeModalKey: number;
    organizationLeaveOpen: boolean;
    setOrganizationLeaveOpen: (open: boolean) => void;
    organizationLeaveModalKey: number;
    organizationGroupLeaveOpen: boolean;
    setOrganizationGroupLeaveOpen: (open: boolean) => void;
    organizationGroupLeaveModalKey: number;
    organizationGroupJoinOpen: boolean;
    setOrganizationGroupJoinOpen: (open: boolean) => void;
    organizationGroupJoinModalKey: number;
  };
}

export function MyAreaOrganizationRequestModals({
  userData,
  organizationJoinBeginnable,
  mainOrganizationChangeBeginnable,
  organizationGroupJoinBeginnable,
  organizationGroupLeaveBeginnable,
  organizationLeaveBeginnable,
  modals,
}: MyAreaOrganizationRequestModalsProps) {
  return (
    <>
      {organizationJoinBeginnable ? (
        <OrganizationJoinRequestModal
          key={`organization-join-${modals.organizationJoinModalKey}`}
          isOpen={modals.organizationJoinOpen}
          onClose={() => modals.setOrganizationJoinOpen(false)}
          userData={userData}
        />
      ) : null}

      {mainOrganizationChangeBeginnable ? (
        <MainOrganizationChangeModal
          key={`main-organization-change-${modals.mainOrganizationChangeModalKey}`}
          isOpen={modals.mainOrganizationChangeOpen}
          onClose={() => modals.setMainOrganizationChangeOpen(false)}
          userData={userData}
        />
      ) : null}

      {organizationGroupJoinBeginnable ? (
        <OrganizationGroupJoinRequestModal
          key={`organization-group-join-${modals.organizationGroupJoinModalKey}`}
          isOpen={modals.organizationGroupJoinOpen}
          onClose={() => modals.setOrganizationGroupJoinOpen(false)}
          userData={userData}
        />
      ) : null}

      {organizationGroupLeaveBeginnable ? (
        <OrganizationGroupLeaveRequestModal
          key={`organization-group-leave-${modals.organizationGroupLeaveModalKey}`}
          isOpen={modals.organizationGroupLeaveOpen}
          onClose={() => modals.setOrganizationGroupLeaveOpen(false)}
          userData={userData}
        />
      ) : null}

      {organizationLeaveBeginnable ? (
        <OrganizationLeaveRequestModal
          key={`organization-leave-${modals.organizationLeaveModalKey}`}
          isOpen={modals.organizationLeaveOpen}
          onClose={() => modals.setOrganizationLeaveOpen(false)}
          userData={userData}
        />
      ) : null}
    </>
  );
}
