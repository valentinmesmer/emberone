/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

const DEFAULT_SOURCE_REPO = 'https://github.com/valentinmesmer/emberone';

export type BuildInfo = {
  version: string | null;
  gitTag: string | null;
  revision: string | null;
  sourceRepo: string;
  sourceUrl: string;
  releaseUrl: string | null;
  licenseUrl: string;
};

/**
 * Version and Corresponding Source of this running build (AGPL-3.0 §13).
 * CI bakes `NEXT_PUBLIC_*` at image build from the Semantic Release tag.
 */
export function getBuildInfo(): BuildInfo {
  const version = process.env.NEXT_PUBLIC_APP_VERSION?.trim() || null;
  const gitTag = process.env.NEXT_PUBLIC_GIT_TAG?.trim() || null;
  const revision = process.env.NEXT_PUBLIC_SOURCE_REVISION?.trim() || null;
  const sourceRepo = process.env.NEXT_PUBLIC_SOURCE_REPO?.trim() || DEFAULT_SOURCE_REPO;
  const sourceUrl = gitTag ? `${sourceRepo}/tree/${gitTag}` : sourceRepo;
  const releaseUrl = gitTag ? `${sourceRepo}/releases/tag/${gitTag}` : null;
  const licenseUrl = gitTag
    ? `${sourceRepo}/blob/${gitTag}/LICENSE`
    : `${sourceRepo}/blob/development/LICENSE`;
  return { version, gitTag, revision, sourceRepo, sourceUrl, releaseUrl, licenseUrl };
}
