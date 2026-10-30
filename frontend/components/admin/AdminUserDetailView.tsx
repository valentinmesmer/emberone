/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { useQueryClient } from '@tanstack/react-query';
import { UserX } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useEffect, useLayoutEffect, useMemo, useState } from 'react';
import { ActionRowIcon, ActionsSection } from '@/components/ui/actions-section';
import { Badge } from '@/components/ui/badge';
import { Card } from '@/components/ui/card';
import { ConfirmDialog } from '@/components/ui/confirm-dialog';
import { FieldDisplay } from '@/components/ui/field-display';
import { ListContainer, ListRow } from '@/components/ui/list';
import { LoadingState } from '@/components/ui/loading';
import { NotFoundState } from '@/components/ui/message-cards';
import { PageContentSection } from '@/components/ui/page-content-section';
import { PageHeader } from '@/components/ui/page-header';
import { useToast } from '@/components/ui/toast';
import { useAuth } from '@/hooks/useAuth';
import { useTranslation } from '@/hooks/useTranslation';
import { adminApi } from '@/lib/api/admin';
import { APP_ROLE_VALUES, getAssignableAppRoles } from '@/lib/auth/claims';
import { useSetBackHref } from '@/lib/navigation/backHrefOverride';
import { adminListKeys, useAdminUser, useSelf } from '@/lib/store';
import { translateBackendError } from '@/lib/utils';
import { getAdminUsersPath } from '@/lib/utils/adminPaths';
import { isValidAppRole } from '@/lib/validation/appRole';

const ROLE_ORDER: string[] = [
  APP_ROLE_VALUES.GLOBAL_ADMIN,
  APP_ROLE_VALUES.TENANT_ADMIN,
  APP_ROLE_VALUES.USER,
];

interface AdminUserDetailViewProps {
  userId: number;
  showTenantFields?: boolean;
}

export function AdminUserDetailView({ userId, showTenantFields = true }: AdminUserDetailViewProps) {
  const t = useTranslation();
  const router = useRouter();
  const { showError, showSuccess } = useToast();
  const { appRole } = useAuth();
  const { data: self } = useSelf();
  const setBackHref = useSetBackHref();

  const queryClient = useQueryClient();
  const { data: user = null, isLoading: loading, error: loadError } = useAdminUser(userId);
  const [roleSaving, setRoleSaving] = useState(false);
  const [pendingRole, setPendingRole] = useState<string | null>(null);
  const [deactivateOpen, setDeactivateOpen] = useState(false);
  const [deactivateLoading, setDeactivateLoading] = useState(false);

  useLayoutEffect(() => {
    setBackHref(getAdminUsersPath());
    return () => setBackHref(null);
  }, [setBackHref]);

  const assignableRoles = useMemo(() => new Set(getAssignableAppRoles(appRole)), [appRole]);

  const roleOptions = useMemo(() => {
    const choices = new Set(assignableRoles);
    if (user?.appRole) {
      choices.add(user.appRole);
    }
    return ROLE_ORDER.filter((role) => choices.has(role));
  }, [assignableRoles, user?.appRole]);

  useEffect(() => {
    if (loadError) {
      showError(loadError instanceof Error ? loadError.message : t('admin.usersLoadError'));
    }
  }, [loadError, showError, t]);

  const displayName = user ? `${user.firstName} ${user.lastName}`.trim() || user.username : '';

  const canChangeRoles = assignableRoles.size > 0;
  const isSelf = self?.id === userId;
  const canDeactivate = !isSelf && user != null;

  async function applyRoleChange(newRole: string) {
    if (!user || !isValidAppRole(newRole) || newRole === user.appRole) {
      return;
    }
    setRoleSaving(true);
    try {
      await adminApi.patchUserAppRole(user.tenantId, user.userId, { appRole: newRole });
      showSuccess(t('admin.roleUpdated'));
      await queryClient.invalidateQueries({ queryKey: adminListKeys.all });
    } catch (e) {
      showError(e instanceof Error ? e.message : t('admin.roleUpdateError'));
    } finally {
      setRoleSaving(false);
      setPendingRole(null);
    }
  }

  async function handleDeactivate() {
    if (!user) {
      return;
    }
    setDeactivateLoading(true);
    try {
      await adminApi.deactivateUser(user.tenantId, user.userId);
      showSuccess(t('admin.userDetail.deactivateSuccess'));
      setDeactivateOpen(false);
      router.push(getAdminUsersPath());
    } catch (err) {
      showError(
        translateBackendError(
          err instanceof Error ? err.message : t('admin.userDetail.deactivateFailed'),
          t,
        ),
      );
    } finally {
      setDeactivateLoading(false);
    }
  }

  if (loading) {
    return <LoadingState variant="embedded" size="sm" />;
  }

  if (!user) {
    return <NotFoundState message={t('admin.userDetail.notFound')} />;
  }

  return (
    <>
      <PageHeader title={displayName} description={user.username} size="compact" />

      <PageContentSection>
        {showTenantFields ? (
          <Card className="p-3 sm:p-4">
            <FieldDisplay label={t('admin.table.tenant')} value={user.tenantName} />
          </Card>
        ) : null}

        <section className="space-y-1.5">
          <h2 className="text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
            {t('admin.userDetail.appRoleSection')}
          </h2>
          <ListContainer label={t('admin.userDetail.appRoleSection')} rowSeparator="inset">
            {roleOptions.map((role) => {
              const isCurrent = role === user.appRole;
              const canSelect = canChangeRoles && !isCurrent && assignableRoles.has(role);
              return (
                <ListRow
                  key={role}
                  title={t(`admin.roles.${role}`)}
                  badges={
                    isCurrent ? (
                      <Badge tone="accent" pill>
                        {t('admin.userDetail.currentBadge')}
                      </Badge>
                    ) : undefined
                  }
                  onClick={canSelect && !roleSaving ? () => setPendingRole(role) : undefined}
                  chevron={false}
                  selected={isCurrent}
                  accent={isCurrent}
                  tone={isCurrent ? 'active' : 'default'}
                  ariaLabel={
                    canSelect
                      ? t('admin.userDetail.selectRoleAria', {
                          role: t(`admin.roles.${role}`),
                        })
                      : undefined
                  }
                />
              );
            })}
          </ListContainer>
          {!canChangeRoles ? (
            <p className="text-xs text-[var(--text-secondary)]">
              {t('admin.userDetail.noRoleChanges')}
            </p>
          ) : null}
        </section>

        <ActionsSection>
          {canDeactivate ? (
            <ListRow
              leading={<ActionRowIcon icon={UserX} />}
              title={t('admin.userDetail.deactivateAction')}
              subtitle={t('admin.userDetail.deactivateHint')}
              onClick={() => setDeactivateOpen(true)}
            />
          ) : (
            <ListRow
              leading={<ActionRowIcon icon={UserX} />}
              title={t('admin.userDetail.deactivateAction')}
              subtitle={
                isSelf
                  ? t('admin.userDetail.deactivateSelfBlocked')
                  : t('admin.userDetail.deactivateUnavailable')
              }
              accent={false}
            />
          )}
        </ActionsSection>
      </PageContentSection>

      <ConfirmDialog
        isOpen={pendingRole != null}
        onClose={() => setPendingRole(null)}
        onConfirm={() => {
          if (pendingRole) {
            void applyRoleChange(pendingRole);
          }
        }}
        title={t('admin.userDetail.confirmRoleTitle')}
        message={
          pendingRole
            ? t('admin.userDetail.confirmRoleMessage', {
                name: displayName,
                role: t(`admin.roles.${pendingRole}`),
              })
            : ''
        }
        confirmText={t('admin.userDetail.confirmRoleAction')}
        isLoading={roleSaving}
        variant="info"
      />

      <ConfirmDialog
        isOpen={deactivateOpen}
        onClose={() => setDeactivateOpen(false)}
        onConfirm={() => {
          void handleDeactivate();
        }}
        title={t('admin.userDetail.confirmDeactivateTitle')}
        message={t('admin.userDetail.confirmDeactivateMessage', { name: displayName })}
        confirmText={t('admin.userDetail.confirmDeactivateAction')}
        isLoading={deactivateLoading}
        variant="danger"
      />
    </>
  );
}
