/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: 'class',
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        fire: {
          50: '#fdf1f1',
          100: '#f9dcdc',
          200: '#f2b8b8',
          300: '#e68a89',
          400: '#d85653',
          500: '#c1272d', // Ember Red - EmberOne brand primary
          600: '#a01f24',
          700: '#7f191d',
          800: '#611315',
          900: '#450d0f',
        },
        background: 'var(--background)',
        foreground: 'var(--foreground)',
      },
    },
  },
  plugins: [],
};
export default config;
