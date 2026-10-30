/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { NextConfig } from 'next';
import { withSerwist } from '@serwist/turbopack';

const nextConfig: NextConfig = {
  output: 'standalone',
  outputFileTracingIncludes: {
    '*': ['./node_modules/@swc/helpers/**'],
  },
  experimental: {
    turbopackFileSystemCacheForDev: false,
    // Dev only: keep React debug info inline in the RSC payload. Over the HMR WebSocket it is
    // handed out once per document; a Chrome-prerendered page that reconnects on activation
    // never receives it and never hydrates (stuck on the loading screen until reload).
    reactDebugChannel: false,
  },
};

export default withSerwist(nextConfig);
