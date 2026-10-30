/*
 * Copyright (C) 2026 Valentin Mesmer
 * This file is part of EmberOne.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

'use client';

import { CalendarClock, ClipboardList, GitBranch, UserPlus } from 'lucide-react';
import { ListContainer, ListRow } from '@/components/ui/list';
import { useTranslation } from '@/hooks/useTranslation';

export type RecordSourceLinkKind = 'SERVICE_EVENT' | 'OPERATION_EVENT' | 'WORKFLOW' | 'ONBOARDING';

interface RecordSourceLinkSectionProps {
  kind: RecordSourceLinkKind;
  href: string;
}

function SourceIcon({ kind }: { kind: RecordSourceLinkKind }) {
  const className = 'h-4 w-4';
  switch (kind) {
    case 'SERVICE_EVENT':
      return <CalendarClock className={className} aria-hidden />;
    case 'OPERATION_EVENT':
      return <ClipboardList className={className} aria-hidden />;
    case 'WORKFLOW':
      return <GitBranch className={className} aria-hidden />;
    case 'ONBOARDING':
      return <UserPlus className={className} aria-hidden />;
  }
}

/** Shared Herkunfts-Link row (Dienstleistung / Einsatz / Antrag / Eintritt). */
export function RecordSourceLinkSection({ kind, href }: RecordSourceLinkSectionProps) {
  const t = useTranslation();
  const title = t(`certifications.record.sourceTitle.${kind}`);
  const linkLabel = t(`certifications.record.sourceLink.${kind}`);

  return (
    <section>
      <h2 className="mb-1.5 text-xs font-medium uppercase tracking-wide text-[var(--text-secondary)]">
        {title}
      </h2>
      <ListContainer label={title} rowSeparator="inset">
        <ListRow
          compact
          href={href}
          chevron
          leading={
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-[var(--surface-muted)] text-[var(--text-secondary)]">
              <SourceIcon kind={kind} />
            </div>
          }
          title={linkLabel}
        />
      </ListContainer>
    </section>
  );
}
