/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Suspense, useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { useRouter, useSearchParams } from 'next/navigation';
import { CheckCircle2, XCircle } from 'lucide-react';
import { useQueryClient } from '@tanstack/react-query';
import { useTranslation } from '@/hooks/useTranslation';
import { useAuth } from '@/lib/auth/AuthContext';
import { userApi } from '@/lib/api/user';
import { ApiRequestError } from '@/lib/api/client';
import { userKeys } from '@/lib/store/queries/users';
import { LoadingState } from '@/components/ui/loading';
import { Button } from '@/components/ui/button';
import { NoticeCard, ErrorStateCard } from '@/components/ui/message-cards';

type VerifyStatus = 'loading' | 'success' | 'error';

function VerifyNotificationEmailContent() {
  const t = useTranslation();
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();
  const { isAuthenticated, isLoading: authLoading, login } = useAuth();
  const token = searchParams.get('token');
  const tenantIdRaw = searchParams.get('tenantId');
  const tenantId = tenantIdRaw ? Number(tenantIdRaw) : NaN;

  const paramsValid = Boolean(token && Number.isFinite(tenantId) && tenantId > 0);
  const [status, setStatus] = useState<VerifyStatus>(() => (paramsValid ? 'loading' : 'error'));
  const [errorMessage, setErrorMessage] = useState<string | null>(() =>
    paramsValid ? null : t('myArea.error.notificationEmailVerifyMissingToken'),
  );
  const handledRef = useRef(false);

  useEffect(() => {
    if (!paramsValid || !token) {
      return;
    }
    if (handledRef.current) {
      return;
    }
    handledRef.current = true;

    void (async () => {
      try {
        await userApi.verifyNotificationEmailPublic({ token, tenantId });
        // Drop stale self profile so master data never shows the pre-verify email.
        queryClient.removeQueries({ queryKey: userKeys.self() });
        setStatus('success');
      } catch (error) {
        setStatus('error');
        if (error instanceof ApiRequestError) {
          setErrorMessage(error.message);
        } else if (error instanceof Error) {
          setErrorMessage(error.message);
        } else {
          setErrorMessage(t('myArea.error.notificationEmailVerifyFailed'));
        }
      }
    })();
  }, [paramsValid, token, tenantId, t, queryClient]);

  const goToMasterData = async () => {
    try {
      const self = await queryClient.fetchQuery({
        queryKey: userKeys.self(),
        queryFn: () => userApi.getSelf(),
      });
      queryClient.setQueryData(userKeys.self(), self);
    } catch {
      // Session may still be establishing after login; MasterDataView will load.
    }
    router.push('/my-area/master-data');
  };

  if (status === 'loading') {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col gap-4 px-4 py-10 sm:py-16">
        <LoadingState
          variant="embedded"
          size="sm"
          message={t('myArea.notificationEmailVerify.verifying')}
        />
      </div>
    );
  }

  if (status === 'success') {
    return (
      <div className="mx-auto flex w-full max-w-lg flex-col gap-5 px-4 py-10 sm:py-16">
        <div className="flex items-center gap-3">
          <CheckCircle2
            className="h-7 w-7 shrink-0 text-[var(--status-success-accent)]"
            aria-hidden
          />
          <h1 className="text-xl font-bold tracking-tight text-[var(--text-primary)] sm:text-2xl">
            {t('myArea.notificationEmailVerify.successTitle')}
          </h1>
        </div>
        <NoticeCard tone="success">{t('myArea.notificationEmailVerify.successMessage')}</NoticeCard>
        {!authLoading && !isAuthenticated ? (
          <>
            <p className="text-sm text-[var(--text-secondary)]">
              {t('myArea.notificationEmailVerify.successLoginHint')}
            </p>
            <Button onClick={() => void login('/my-area/master-data')} fullWidth>
              {t('myArea.notificationEmailVerify.login')}
            </Button>
          </>
        ) : (
          <Button fullWidth onClick={() => void goToMasterData()}>
            {t('myArea.notificationEmailVerify.successContinue')}
          </Button>
        )}
      </div>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-lg flex-col gap-5 px-4 py-10 sm:py-16">
      <div className="flex items-center gap-3">
        <XCircle className="h-7 w-7 shrink-0 text-red-600 dark:text-red-400" aria-hidden />
        <h1 className="text-xl font-bold tracking-tight text-[var(--text-primary)] sm:text-2xl">
          {t('myArea.notificationEmailVerify.errorTitle')}
        </h1>
      </div>
      <ErrorStateCard>
        {errorMessage ?? t('myArea.error.notificationEmailVerifyFailed')}
      </ErrorStateCard>
      <Link
        href="/"
        className="text-center text-sm font-medium text-[var(--fire-red)] underline-offset-2 hover:underline"
      >
        {t('myArea.notificationEmailVerify.backHome')}
      </Link>
    </div>
  );
}

export default function VerifyNotificationEmailPage() {
  return (
    <Suspense fallback={<LoadingState variant="embedded" size="sm" />}>
      <VerifyNotificationEmailContent />
    </Suspense>
  );
}
