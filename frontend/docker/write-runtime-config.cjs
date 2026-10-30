/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use strict';

const fs = require('fs');
const path = require('path');

const KEYS = [
  'NEXT_PUBLIC_API_BASE_URL',
  'NEXT_PUBLIC_AUTHENTIK_ISSUER',
  'NEXT_PUBLIC_AUTHENTIK_CLIENT_ID',
  'NEXT_PUBLIC_AUTHENTIK_SCOPE',
  'NEXT_PUBLIC_GEOCODER_URL',
  'NEXT_PUBLIC_REALTIME_ENABLED',
];

const out = {};
for (const k of KEYS) {
  const v = process.env[k];
  if (v != null && String(v).length > 0) {
    out[k] = v;
  }
}

const publicDir = path.join(process.cwd(), 'public');
fs.mkdirSync(publicDir, { recursive: true });
fs.writeFileSync(path.join(publicDir, 'runtime-config.json'), JSON.stringify(out));
