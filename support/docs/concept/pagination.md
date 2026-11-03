# Paginierung von Listen

Einheitlicher Listen-GET-Vertrag für wachsende Collections (Backend + Frontend). Verwandt: [Frontend API-Client](frontend-api-client.md), [Frontend Component-Library](frontend-component-library.md) (`ListControls`), [Todo-Modul](todo.md), [Benachrichtigungssystem](notification-system.md). Domänenspezifische Endpunkte stehen in den jeweiligen Konzept-Dokumenten (z. B. [Zeiterfassung](timeentry.md), [Workflow-Engine](workflow-engine.md), [Mitgliedschaften](user-memberships.md)).

## Mental Model

```
HTTP GET  ?page&size&search&sort&…Filter
        │
        ▼
ListQuery (+ domänenspezifisches *ListFilter-Record)
        │
        ▼
ListDefinition → Pageable + Search-Spec
        +
XxxListSpecs   → Visibility / Status / …
        │
        ▼
PageDto<T>  (content, page, size, totalElements, totalPages, hasNext)
        │
        ▼
defineList → useListState | useUrlSyncedListState → listQuery → usePagedList → ListControls + ListLoadMore
```

**Ein Weg:** Gleiche Mechanik für jede wachsende Liste. Keine Parallel-Parameter (`archived`, `openOnly`, `unreadOnly`, …). Ausnahme nur mit `@BoundedList` am Handler (Kataloge, natürlich begrenzte Sub-Ressourcen).

## HTTP-Vertrag

### Anfrage

| Parameter | Regel |
|-----------|--------|
| `page` | 0-basiert; Default `0` |
| `size` | Optional; Default **50**, Maximum **200** (`ListEndpointLimits`) |
| `sort` | `key,asc` / `key,desc`; `key` aus der Whitelist der `ListDefinition` (ungültiger Key, Format oder Richtung → `IllegalArgumentException` → 400). Optional: ohne `sort` gilt die Standardreihenfolge. **Vorrang:** Client-`sort` > Segment-Override (`ListDefinition.pageable(query, sortOverride)`, z. B. Termine/Abwesenheiten anstehend aufsteigend, vergangen absteigend; Todos offen `createdAt`, archiviert `archivedAt` absteigend) > `defaultSort`. Tiebreaker immer `id` aufsteigend. Ein Key kann mehrere JPA-Properties umfassen (gleiche Richtung, z. B. Nachname dann Vorname), Textspalten sortieren case-insensitive (`textSortKey`) |
| `search` | Optional; Leerstring wird ignoriert |
| Filter | Pro Endpoint als eigene Query-Parameter (siehe unten) |

**Suche:** Whitespace-getrennte Tokens; **alle** Tokens müssen treffen (AND). Pro Token matcht **mindestens ein** konfiguriertes Suchfeld (OR über Felder). Max. **5** Wörter, **100** Zeichen (`ListSpecs`); Verletzung → `400`.

**Filter-Typen (Frontend-Deklaration ↔ API):**

| Art | Serialisierung | Semantik |
|-----|----------------|----------|
| `select` | ein Query-Param (`key=value`) | Enum/String; fehlend → Backend-Default im `*ListFilter`-Record |
| `multiSelect` | wiederholter gleicher Param | leer = kein Filter |
| `dateRange` | `fromParam`, `toParam` (ISO-Instant) | `from` inklusive, `to` **exklusiv** (`ListSpecs.range`) |

Der Such-Param-Name ist pro Liste konfigurierbar (Default: `search`). Segmente (`period`, `status`, …) sind `select`-Filter, werden immer mitgesendet (auch der Standardwert) und wechseln Datenbestand und Reihenfolge.

**Filter-Optionen (`runtimeOptions`):** Pro paginierter Liste ein `GET …/list-filter-options` **am selben Controller** wie die Liste, gleiche `@PreAuthorize`. Backend: `ListFilterOptionsSource` (z. B. `AdminUserListFilterOptions`); Frontend: `filterOptionsSource` in der `ListDefinition` (z. B. `admin/users`). Einzelwerte über `ListFilterOptionsAuthorization` wie beim Listen-GET. Org-Listen folgen nach demselben Muster (`Appointment…ListFilterOptions`, …).

### Antwort

JSON-Seite analog `PageResponse<T>` im Frontend (`frontend/types/page.ts`):

- `content`: Array der Zeilen
- `page`, `size`, `totalElements`, `totalPages`, `hasNext`

Domänen-DTOs heißen oft `XxxPageDto`, bauen aber auf dem gleichen Feldset auf.

### Nicht-Ziele (bewusst)

- Kein URL-Sync von Page (Filter und Suche der Hauptliste: opt-in `syncUrl`, siehe unten)
- Keine generische Filter-DSL
- Kein clientseitiges `.filter()` / `.sort()` auf paginierten Listen-Daten
- Gruppierte Übersichten (Dienstgrad-/Funktionsübersicht): Filtern, Suchen und Sortieren der Mitglieder passiert serverseitig; das Frontend gruppiert nur (Darstellung) und behält die Server-Reihenfolge. Die Funktionsübersicht (`…/functions/overview`) nimmt dafür `search` entgegen.

## Backend

Package `com.emberone.platform.web.list`:

| Baustein | Rolle |
|----------|--------|
| `ListQuery` | Gemeinsame `@RequestParam`-Parameter `page`, `size`, `search`, `sort` |
| `ListDefinition<E>` | Whitelist Sort-Keys (`sortKey`, `textSortKey`), Such-Pfade, `pageable()`, `searchSpecification()` |
| `ListSort` | Record `key` + `direction`; `ListSort.parse(String)` ist das **einzige** Parsing von `sort` (auch für In-Memory-Listen) |
| `ListSpecs` | Token-Suche, `eq`/`in`/`range`, Like-Escaping |
| `PageReorder` | ID-Page laden, Assoziationen batch-hydratisieren, Reihenfolge wiederherstellen |
| `BoundedList` | Marker: bewusst **kein** `PageDto` (Grund im `value`-Attribut) |

Limits: `com.emberone.platform.web.ListEndpointLimits`.

**Neue paginierte Liste (Checkliste):**

1. `XxxListDefinition` - `ListDefinition.forEntity(...).sortKey(...).defaultSort(...).build()`; Suchfelder nur wenn UI-Suche geplant. Sort-Keys: `sortKey(key, props…)` (Mehrspalten, gleiche Richtung), `textSortKey` für Namen/Titel/Orte (case-insensitive). In-Memory-Listen (Qualifikations-/Zertifikatsübersicht): `ListSort.parse` + konstante Map `key → Comparator`, kein eigenes Parsing.

**Gruppierte Übersichten** (Gruppen mit Mitgliedern, z. B. Qualifikationsübersicht `GET …/qualifications/status-overview`) sind keine Seite: Sie liefern alle Gruppen in Backend-Reihenfolge und nehmen nur `search` entgegen, der serverseitig angewendet wird (Gruppenname trifft → alle Mitglieder; sonst nur passende Mitglieder, leere Gruppen entfallen). Das Frontend sendet `debouncedSearch` und filtert/sortiert nicht selbst; die Definition hat kein `sort`.
2. `XxxListFilter` - Record mit domänenspezifischen `@RequestParam` + sinnvolle Defaults im Compact-Constructor.
3. `XxxListSpecs` - Sichtbarkeit, Status, Mandant/RLS-relevante Prädikate (kein `WHERE tenant_id` - RLS über Session-GUC).
4. Service: `Specification`-Kette (`definition.searchSpecification(query).and(...)`), `repository.findAll(spec, pageable)`, Mapping zu `PageDto`.
5. Controller: `ListQuery query, XxxListFilter filter` - keine Extra-Liste-Parameter.
6. Tests: Controller-/Security-Tests mit `page`/`sort`/Filter; ungültiges `sort` → Fehler; Sortierung ändert nie die Sichtbarkeit.

**Referenzimplementierung Todo:** `com.emberone.todo.list.*`, Self-`GET /api/v1/self/todos` - Filter `status=OPEN|ARCHIVED` (Default `OPEN`), Sortierung per Status-Override (`OPEN` → `createdAt desc`, `ARCHIVED` → `archivedAt desc`). Details: [todo.md](todo.md).

**Referenzimplementierung Notifications:** `com.emberone.notification.list.*`, Self-`GET /api/v1/self/notifications` - Filter `status=UNREAD|READ` (Default `UNREAD` wenn Parameter fehlt). Panel nutzt `notificationListUnreadQuery` wie Todos `todoListOpenQuery`. Details: [notification-system.md](notification-system.md).

## Frontend

| Ort | Rolle |
|-----|--------|
| `frontend/lib/list/types.ts` | `ListDefinition`, Filter-Typen, `ListSortOption` / `ListSortConfig`, `ListQuery` |
| `frontend/lib/list/defineList.ts` | `defineList`, `createInitialListQueryState`, `toListQuery` |
| `frontend/lib/list/useListState.ts` | Such-Debounce, Filter- und Sort-State (`sort`/`setSort`), optionaler URL-Sync (`syncUrl`), `listQuery` für React Query |
| `frontend/lib/list/urlSync.ts` | Lesen/Schreiben der URL-Parameter (`q`, `sort`, Segment-Key, `from`/`to`; nur Nicht-Standardwerte) |
| `frontend/lib/list/<domain>List.ts` | Pro Liste: `defineList({ id, search?, sort?, filters? })` (`sort.options` nur mit Keys der Backend-Whitelist; ohne `sort` keine Sortier-UI) + Hilfs-Queries |
| `frontend/lib/api/paged.ts` | `buildQuery` (Multi-Select = repeated keys), `fetchAllPageContent` |
| `frontend/lib/store/usePagedList.ts` | `useInfiniteQuery` + optional `fetchAll` für Lookups |
| `frontend/types/page.ts` | `PageResponse<T>`, `LIST_ENDPOINT_MAX_PAGE_SIZE` |

**Neue paginierte Liste (Checkliste):**

1. `defineList` in `lib/list/<domain>List.ts` (labelKeys → `locales/de.json`).
2. API-Modul: `list(query)` mit `buildQuery({ page, size, search, sort, …filter })`.
3. Store: `usePagedList({ queryKey, listQuery, queryFn })`; Cache-Key enthält `listQuery`.
4. View: `useListState(definition, { syncUrl: true })` (nur Hauptliste der Seite; `<Suspense>` in `page.tsx`) + `ListControls` + `ListLoadMore` / `ListNoResults`; Leerzustand `EmptyStateCard` (nichts vorhanden) bzw. `ListNoResults` (Suche/Filter aktiv); **kein** lokales Filtern der `content`-Zeilen.
5. Dropdowns/Lookups: `fetchAllPageContent` oder `usePagedList({ fetchAll: true })` - Hard-Cap 50 Seiten (`FETCH_ALL_MAX_PAGES`).

**Listen je Kontext:** Self- und Personenlisten nutzen `absenceListDefinition` / `timeEntryListDefinition` (ohne Suche); die Organisationsseiten `orgAbsenceListDefinition` (Segment Aktuell/Anstehend/Vergangen, Suche nach Person) und `orgTimeEntryListDefinition` (Zeitraum + Suche nach Person). Termine: Segment `period=UPCOMING|PAST`; explizites `from`/`to` (Ereignis-Import der Teilnehmer, `useAppointments`: 1 Monat zurück bis 12 Monate voraus) hat Vorrang vor `period`.

**URL-Sync:** Pro Seite genau eine Hauptliste mit `syncUrl: true`; Abschnittslisten, Modals und Panels bleiben lokal. Details und Parameter: [Frontend Component-Library - URL-Sync](frontend-component-library.md#url-sync-useurlsyncedliststate).

UI-Bausteine: [Frontend Component-Library - Listen-Steuerung](frontend-component-library.md#listen-steuerung-listcontrols).

Store- und Client-Muster: [Frontend API-Client - Paginierte Listen](frontend-api-client.md#paginierte-listen).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Shared DTO / Limits | `platform/web/PageDto.java`, `ListEndpointLimits.java` |
| List-Infrastruktur | `platform/web/list/*` (`ListFilters` für wiederholte Query-Filter) |
| Todo-Liste | `todo/list/*`, `TodoSelfController`, `frontend/lib/list/todoList.ts` |
| Notification-Liste | `notification/list/*`, `NotificationSelfController`, `frontend/lib/list/notificationList.ts` |
| Org-Dienstgrad-Mitglieder | `rank/list/*`, `OrganizationRankController`, `frontend/lib/list/organizationRankMemberList.ts` |
| UI | `components/ui/list-filter.tsx`, `list-filter-panel.tsx`, `list-sort.tsx` |

Agent-Regel (Kurz): [`AGENTS.md`](../../../AGENTS.md) - List GETs / `defineList` + `useListState` + `ListControls`.

## Qualitätssicherung

Vor größeren Listen-Änderungen oder vor Merge:

| Check | Befehl |
|-------|--------|
| Keine Legacy-Listen-Parameter auf HTTP-Oberfläche | `./support/scripts/check-list-legacy-params.sh` |
| Jeder Collection-`GET` paginiert oder `@BoundedList` | `./gradlew test --tests 'com.emberone.platform.web.list.BoundedListConventionTest'` |
| Backend (bei Listen-Touch) | `./gradlew test --tests 'com.emberone.platform.web.list.*'` |
| Frontend | `cd frontend && bun lint && bun run build` |

`@BoundedList` erfordert einen kurzen englischen `value`-Grund am Handler. Neue paginierte Listen ohne `@BoundedList` müssen `PageDto` / `*PageDto` zurückgeben.
