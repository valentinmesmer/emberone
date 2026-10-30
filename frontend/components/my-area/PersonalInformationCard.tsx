/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { FieldDisplay } from '@/components/ui/field-display';
import { SectionActions } from '@/components/ui/section-header';
import { useTranslation } from '@/hooks/useTranslation';
import { formatDateLong } from '@/lib/utils';
import { formatAddress } from '@/lib/utils/address';
import { Salutation } from '@/types/enums/salutation';
import type { UserDto } from '@/types/user';
import { ProfileImage } from './ProfileImage';

interface PersonalInformationCardProps {
  userData: UserDto;
  /** Master data: open name-change request (workflow). */
  onRequestNameChange?: () => void;
  /** When true, the name-change button is disabled (e.g. user already has an open name-change workflow). */
  nameChangeRequestDisabled?: boolean;
  /** Master data: open address-change request (workflow). */
  onRequestAddressChange?: () => void;
  /** When true, the address-change button is disabled (e.g. user already has an open address-change workflow). */
  addressChangeRequestDisabled?: boolean;
}

export function PersonalInformationCard({
  userData,
  onRequestNameChange,
  nameChangeRequestDisabled = false,
  onRequestAddressChange,
  addressChangeRequestDisabled = false,
}: PersonalInformationCardProps) {
  const t = useTranslation();

  const getSalutationLabel = (salutation?: Salutation) => {
    if (!salutation) return '-';
    return salutation === Salutation.MR
      ? t('setup.user.salutationMr')
      : t('setup.user.salutationMrs');
  };

  return (
    <Card className="p-3 sm:p-4">
      <div className="mb-4 flex items-start gap-3 sm:gap-4">
        <ProfileImage firstName={userData.firstName} lastName={userData.lastName} />
        <div className="min-w-0 flex-1 pt-0.5 sm:pt-1">
          <h1 className="break-words text-lg font-semibold text-[var(--text-primary)]">
            {userData.firstName} {userData.lastName}
          </h1>
          <p className="mt-1 text-sm text-[var(--text-secondary)]">
            {t('myArea.fields.personalNumber')}:{' '}
            <span className="text-[var(--text-primary)]">{userData.personalNumber}</span>
          </p>
          <p className="mt-0.5 text-sm text-[var(--text-secondary)]">
            {t('myArea.fields.userIdentifier')}:{' '}
            <span className="font-mono text-[var(--text-primary)]">
              {userData.username?.trim() ? userData.username : t('common.notAvailable')}
            </span>
          </p>
        </div>
      </div>

      <div className="space-y-4 border-t border-[var(--border-color)] pt-4">
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <FieldDisplay
            label={t('setup.user.salutation')}
            value={getSalutationLabel(userData.salutation)}
          />
          <FieldDisplay
            label={t('setup.user.dateOfBirth')}
            value={formatDateLong(userData.dateOfBirth)}
          />
        </div>

        {(userData.contactInformation?.street ||
          userData.contactInformation?.city ||
          userData.contactInformation?.postalCode ||
          userData.contactInformation?.country) && (
          <FieldDisplay
            label={t('myArea.fields.address')}
            value={formatAddress(
              userData.contactInformation.street,
              userData.contactInformation.city,
              userData.contactInformation.postalCode,
              userData.contactInformation.country,
            )}
          />
        )}

        {(onRequestNameChange || onRequestAddressChange) && (
          <div className="border-t border-[var(--border-color)] pt-4">
            <p className="mb-3 text-xs font-semibold uppercase tracking-wide text-[var(--text-secondary)]">
              {t('myArea.masterData.changeRequestsSection')}
            </p>
            <SectionActions>
              {onRequestNameChange ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onRequestNameChange}
                  disabled={nameChangeRequestDisabled}
                  title={
                    nameChangeRequestDisabled
                      ? t('myArea.nameChange.blockedOpenRequest')
                      : undefined
                  }
                >
                  {t('myArea.nameChange.requestButton')}
                </Button>
              ) : null}
              {onRequestAddressChange ? (
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  fullWidth="responsive"
                  onClick={onRequestAddressChange}
                  disabled={addressChangeRequestDisabled}
                  title={
                    addressChangeRequestDisabled
                      ? t('myArea.addressChange.blockedOpenRequest')
                      : undefined
                  }
                >
                  {t('myArea.addressChange.requestButton')}
                </Button>
              ) : null}
            </SectionActions>
          </div>
        )}
      </div>
    </Card>
  );
}
