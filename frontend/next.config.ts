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
};

export default withSerwist(nextConfig);
