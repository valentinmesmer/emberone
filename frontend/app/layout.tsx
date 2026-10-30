/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { Metadata, Viewport } from 'next';
import { Sora, Manrope, JetBrains_Mono } from 'next/font/google';
import { SerwistProvider } from '@serwist/turbopack/react';
import './globals.css';
import { t } from '@/lib/i18n/translations';
import { ClientAuthProvider } from '@/components/auth/ClientProviders';
import { AuthLayout } from '@/components/auth/AuthLayout';
import { ThemeChromeSync, ThemeProvider } from '@/components/theme';
import { LocaleProvider } from '@/components/locale';
import { PermissionProvider } from '@/lib/permission';
import { ToastProvider } from '@/components/ui/toast';
import { QueryProvider } from '@/lib/store/providers';
import { RealtimeProvider } from '@/lib/realtime';

const sora = Sora({
  variable: '--font-sora',
  subsets: ['latin'],
  weight: ['700', '800'],
  // Only applied via CSS on headings - preload would warn as unused on first paint.
  preload: false,
});

const manrope = Manrope({
  variable: '--font-manrope',
  subsets: ['latin'],
});

const jetbrainsMono = JetBrains_Mono({
  variable: '--font-jetbrains-mono',
  subsets: ['latin'],
  weight: ['400', '500'],
  // Sparse `font-mono` usage - avoid unused preload warnings on most routes.
  preload: false,
});

export const metadata: Metadata = {
  title: t('app.title'),
  description: t('app.description'),
  applicationName: t('app.title'),
  manifest: '/site.webmanifest',
  // App-icon everywhere (tabs, home screen, mail-client sender lookup via /favicon.ico).
  icons: {
    icon: [
      { url: '/favicon.ico', sizes: '48x48' },
      { url: '/favicon.svg', type: 'image/svg+xml' },
      { url: '/favicon-16.png', sizes: '16x16', type: 'image/png' },
      { url: '/favicon-32.png', sizes: '32x32', type: 'image/png' },
      { url: '/icon-192.png', sizes: '192x192', type: 'image/png' },
      { url: '/icon-512.png', sizes: '512x512', type: 'image/png' },
    ],
    shortcut: [{ url: '/favicon.ico' }],
    apple: [{ url: '/apple-touch-icon.png', sizes: '180x180', type: 'image/png' }],
  },
  appleWebApp: {
    title: t('app.title'),
    capable: true,
    statusBarStyle: 'default',
  },
  formatDetection: {
    telephone: false,
  },
};

/**
 * - `viewport-fit=cover`: bottom tab bar respects the iOS home-indicator safe area.
 * - `maximumScale` + `userScalable: false`: no pinch-zoom and no auto-zoom-on-focus
 *   (keeps the app “fullscreen” on phones; weaker for accessibility than allowing zoom).
 * - `themeColor`: `--chrome-bg` from `globals.css`, server-rendered so the first
 *   paint is already correct. `ThemeChromeSync` rewrites both entries once the
 *   user picks a theme that diverges from the system one. Honored by Android
 *   Chrome, iOS Safari < 26 and the installed PWA; iOS 26 Safari instead samples
 *   the fixed chrome elements (`Header` / `MobileBottomNav`).
 */
export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
  userScalable: false,
  viewportFit: 'cover',
  themeColor: [
    { media: '(prefers-color-scheme: light)', color: '#fdfcfa' },
    { media: '(prefers-color-scheme: dark)', color: '#1e1f21' },
  ],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="de" suppressHydrationWarning>
      <body
        className={`${manrope.className} ${manrope.variable} ${sora.variable} ${jetbrainsMono.variable} antialiased`}
        suppressHydrationWarning
      >
        <SerwistProvider
          swUrl="/serwist/sw.js"
          disable={process.env.NODE_ENV === 'development'}
          reloadOnOnline={false}
        >
          <ThemeProvider>
            <ThemeChromeSync />
            <QueryProvider>
              <ClientAuthProvider>
                <RealtimeProvider>
                  <PermissionProvider>
                    <LocaleProvider>
                      <ToastProvider>
                        <AuthLayout>{children}</AuthLayout>
                      </ToastProvider>
                    </LocaleProvider>
                  </PermissionProvider>
                </RealtimeProvider>
              </ClientAuthProvider>
            </QueryProvider>
          </ThemeProvider>
        </SerwistProvider>
      </body>
    </html>
  );
}
