/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { usePathname } from 'next/navigation';
import { useEffect } from 'react';
import { AppointmentRealtimeSync } from '@/components/appointment/AppointmentRealtimeSync';
import { EventRealtimeSync } from '@/components/event/EventRealtimeSync';
import { Header } from '@/components/layout/Header';
import { MobileBottomNav } from '@/components/layout/MobileBottomNav';
import { MobilePullToRefresh } from '@/components/layout/MobilePullToRefresh';
import { NavDrawer } from '@/components/layout/NavDrawer';
import { NotificationPanel } from '@/components/notification/NotificationPanel';
import { NotificationRealtimeToast } from '@/components/notification/NotificationRealtimeToast';
import { OnboardingReminderPrompt } from '@/components/onboarding/OnboardingReminderPrompt';
import { OrganizationMembershipRealtimeSync } from '@/components/organization/OrganizationMembershipRealtimeSync';
import { PwaInstallPrompt } from '@/components/pwa/PwaInstallPrompt';
import { RefreshServerCacheProvider } from '@/lib/store/useRefreshServerCache';
import { ProtectedRoute } from './ProtectedRoute';

interface AuthLayoutProps {
  children: React.ReactNode;
}

const PUBLIC_PATH_PREFIXES = [
  '/auth/callback',
  '/verify-notification-email',
  '/~offline',
  '/source',
] as const;

function isPublicPath(pathname: string): boolean {
  return PUBLIC_PATH_PREFIXES.some(
    (prefix) => pathname === prefix || pathname.startsWith(`${prefix}/`),
  );
}

export function AuthLayout({ children }: AuthLayoutProps) {
  const pathname = usePathname();

  // <body> is the scroll container (see globals.css), so Next's default
  // scroll-to-top - which targets the now non-scrolling <html> - is a no-op.
  // Reset the body scroll on navigation to restore "land at the top" behaviour.
  useEffect(() => {
    document.body.scrollTo({ top: 0, left: 0 });
  }, []);

  // OIDC callback / public verify: no chrome, no login redirect
  if (isPublicPath(pathname)) {
    return <>{children}</>;
  }

  // All other pages require authentication
  return (
    <RefreshServerCacheProvider>
      <Header />
      <MobilePullToRefresh key={pathname} />
      <ProtectedRoute>{children}</ProtectedRoute>
      {/* Global mobile chrome - self-gates (null on desktop, auth/setup routes, logged out). */}
      <NavDrawer />
      <NotificationPanel />
      <NotificationRealtimeToast />
      <AppointmentRealtimeSync />
      <EventRealtimeSync />
      <OrganizationMembershipRealtimeSync />
      <MobileBottomNav />
      <PwaInstallPrompt />
      <OnboardingReminderPrompt />
    </RefreshServerCacheProvider>
  );
}
