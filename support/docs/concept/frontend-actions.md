# Frontend-Aktionen auf Detailseiten

Einheitliches Muster für Aktionen auf **Detailseiten** (eine einzelne Entität) und detailähnlichen Seiten (Stammdaten, Org-Detail, Admin-Detail). **Listen** sind nicht Teil dieses Dokuments: Zeilen-Aktionen laufen dort weiter über `ListRow.actions`, Create über `PageHeader` / `SectionHeader` ([Component-Library](frontend-component-library.md#aktionsmuster-ein-intent--ein-pattern)).

## Entscheidungsregel

> Betrifft die Aktion nur den Inhalt dieser Karte, oder die ganze Entität?

| Ebene | Ort | Inhalt | Komponente |
|-------|-----|--------|------------|
| **Container** | Icon oben rechts in der Karte | **Bearbeiten** (nur Bearbeiten, keine Ausnahme) | `DetailCardHeader` + `DetailCardAction` (Entity-Detail-Cards), `SectionEditAction` (Section-Cards) |
| **Seite** | Ganz unten | Alles andere, was die Entität betrifft: Ansehen und Herunterladen (Dateien), Löschen, Archivieren / Entarchivieren, Beenden, Deaktivieren, Klonen, Abbrechen, Zurückziehen, Veröffentlichen, Abschließen, Verschieben, Als Vorlage speichern, Event aus Termin anlegen, Ernennen / Entziehen | `ActionsSection` + `ListRow` |
| **Primäre Aufgabe** (Ausnahme) | Eigene Karte oben | Die Aufgabe, für die die Seite geöffnet wird: Workflow-Entscheidung (Genehmigen / Ablehnen) und „Änderung vornehmen“ bei angefragter Änderung, Todo erledigen, Zeiteintrag ausstempeln | Karte mit Primär-Button |

## Regeln

1. **Der Header hat höchstens ein Icon: `Pencil` (Bearbeiten).** Alles andere, auch Ansehen und Herunterladen, steht unten.
2. **Kein beschrifteter `Button` im `DetailCardHeader`** und kein Button-Footer in Detail-Karten. Beschriftete Aktionen sind Zeilen der `ActionsSection`.
3. **`ActionsSection` steht am Seitenende**, nach allen Inhalts-Sections (auch nach Verlauf und Listen). Höchstens eine pro Seite. Modals und Dialoge stehen danach.
4. **Reihenfolge**: nicht-destruktive Zeilen zuerst, Löschen immer zuletzt.
5. **Zeilenaufbau**: `leading={<ActionRowIcon icon={…} />}`, `title` = Verb + Objekt, `subtitle` = Hinweis auf die Folge. Kein Tone, kein Chevron (`ActionsSection` normalisiert das).
6. **Gesperrte Aktion**: Wenn der Grund für den Nutzer relevant ist (z. B. eigener Account), bleibt die Zeile ohne `onClick` stehen und der Subtitel nennt den Grund. Fehlt nur die Berechtigung, wird die Zeile weggelassen.
7. **Bestätigung**: jede Seiten-Aktion mit Folgen öffnet `ConfirmDialog` bzw. `DestructiveTwoStepConfirmModal`. Nach Löschen: `router.push` auf die Liste.
8. **Titel**: Default ist `common.actionsSection` („Aktionen"). Ein eigener `title` nur, wenn der Domänenkontext es braucht.
9. **Create** (Hinzufügen, Einreichen) bleibt beschrifteter `Button` im `SectionHeader` / `PageHeader`, nicht in der `ActionsSection`.
10. **Eingebettete Listen** auf Detailseiten (z. B. Sessions eines Kursangebots) behalten `ListRow.actions` für ihre Zeilen. Die Aktionen der Seite selbst folgen dieser Regel.
11. **Navigation** ist keine Aktion: `href` / Chevron oder Hub-Tile (Ausnahme: „Workflow öffnen" auf Self-Seiten, solange es der einzige Weg zum Prozess ist).

## Referenzseiten

| Fall | Referenz |
|------|----------|
| Edit-Icon + Lifecycle unten | Admin-Org-Detail (`admin/tenants/[id]/organizations/[orgId]`), Gruppen-Mitgliedschaft (`GroupMembershipDetailContent`) |
| Viele Statuswechsel + Löschen | `CourseOfferingDetailView` |
| Ansehen / Herunterladen | `UserDocumentDetailView` |
| Löschen einer Entität | `CourseOfferingSessionDetailView`, `MyAreaAbsenceDetailView` |
| Archivieren / Entarchivieren | `AdminTenantCertificationDetailView` |
| Primäre Aufgabe oben, Rest unten | `WorkflowDetailView` |

## Bausteine

- `ActionsSection`, `ActionRowIcon`: `frontend/components/ui/actions-section.tsx`
- `DetailCardHeader`, `DetailCardAction`: `frontend/components/ui/detail-card-header.tsx`
- `SectionEditAction`: `frontend/components/ui/section-header.tsx`
