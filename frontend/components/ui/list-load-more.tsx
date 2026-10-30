/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { Button } from '@/components/ui/button';
import { useTranslation } from '@/hooks/useTranslation';
import { cn } from '@/lib/utils/cn';

interface ListLoadMoreProps {
  hasNextPage: boolean;
  isFetchingNextPage: boolean;
  onLoadMore: () => void;
  className?: string;
  /** Render as `<li>` when placed inside `<ListContainer>`. */
  asListItem?: boolean;
}

export function ListLoadMore({
  hasNextPage,
  isFetchingNextPage,
  onLoadMore,
  className,
  asListItem = false,
}: ListLoadMoreProps) {
  const t = useTranslation();

  if (!hasNextPage) {
    return null;
  }

  const inner = (
    <Button
      type="button"
      variant="secondary"
      size="sm"
      onClick={onLoadMore}
      isLoading={isFetchingNextPage}
      disabled={isFetchingNextPage}
    >
      {t('common.loadMore')}
    </Button>
  );

  const shellClass = cn('flex justify-center px-3 py-3', className);

  if (asListItem) {
    return <li className={cn('border-l-[3px] border-l-transparent', shellClass)}>{inner}</li>;
  }

  return <div className={shellClass}>{inner}</div>;
}
