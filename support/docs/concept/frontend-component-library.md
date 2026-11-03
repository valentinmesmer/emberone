# Frontend Component-Library (`components/ui/*`)

Design-Notizen für wiederverwendbare UI-Primitives. Domänenneutrale Bausteine unter `frontend/components/ui/`; Feature-Views liegen in `components/{admin,organization,my-area,…}`. Verwandt: [Frontend Detail-Routing](frontend-detail-routing.md), [Frontend i18n](frontend-i18n.md), [Paginierung von Listen](pagination.md).

## Mental Model

```
Feature-Komponenten (components/{admin,organization,my-area,…})
        │ nutzen
        ▼
components/ui/*   ← Primitives + Page-Chrome
        │ nutzen
        ▼
cn() + CSS-Vars + lucide-react
```

**Kein shadcn/ui, kein Radix, keine `components.json`.** Handgeschrieben. Basis-Primitive: `react-aria-components` (Date-Picker u. a.); zusätzlich `libphonenumber-js` (`phone-input`), `@internationalized/date` in Date-Pickern. `address-input` fragt Photon über `NEXT_PUBLIC_GEOCODER_URL` per `fetch` ab, ohne Geocoding-Bibliothek.

## Was wohin gehört

| Ort | Inhalt |
|-----|--------|
| `components/ui/` | Domänenneutrale Controls, Feedback, Surfaces, Page-Chrome |
| `components/{feature}/` | Domänen-Views, Modals, Business-Logik |
| `components/layout/` | App-Shell (Sidebar, Mobile-Nav) |
| `lib/ui/` | State-/Label-Helfer ohne JSX |

## Inventar (Auszug)

- **Controls:** `button`, `input`, `textarea`, `select`, `checkbox`, `switch`, `file-input`, `date-picker`, `date-time-picker`, `date-field-segment` (gemeinsames Segment-Styling für beide Date-Picker), …
- **Feedback:** `toast`, `loading`, `message-cards` (`NoticeCard`), `confirm-dialog`, `badge`
  - **`confirm-dialog`:** Standard ist `variant="danger"` (destruktiv). `warning` hebt heikle Bestätigungen hervor. `variant="info"` für neutrale Bestätigungen (z. B. Rolle ändern, optional importieren) - Bestätigen wie `ModalFooter` Primary (`--fire-red`), nicht Blau.
- **Surfaces:** `card`, `modal`, `list` (`ListRow` / `ListContainer`), `field-display`, `actions-section` (`ActionsSection`)
- **Page-Chrome:** `page-header`, `page-content-section`, `back-button`, `section-header` (`SectionHeader` / `SectionActions` / `SectionEditAction`), `detail-card-header` (`DetailCardHeader` / `DetailCardAction`)
- **Listen-Steuerung:** `list-filter.tsx` (`ListControls`, `ListLoadMore`, `ListNoResults`, …), `list-filter-panel.tsx` (Filter-Sheet: Multi-Select, Zeitraum mit Presets; Filter-Chips)
- **Tones:** `tones.ts` (`BadgeTone`, `ListRowTone`)

Kein Barrel-`index.ts` - Importe immer direkt:

```ts
import { Button } from '@/components/ui/button';
```

## Naming & Patterns

1. Dateien **kebab-case** (`date-time-picker.tsx`); Exports **PascalCase** Named Functions. Ausnahme: `DestructiveTwoStepConfirmModal.tsx` (PascalCase-Dateiname)
2. Fast alle Module: `'use client'`
3. Klassen: `cn()` aus `lib/utils/cn.ts` (nicht den `@/lib/utils`-Barrel aus Primitives)
4. Styling: Tailwind + CSS-Variablen (`--fire-red`, `--border-color`, …); Icons: `lucide-react`
5. i18n wo sichtbarer Text: `useTranslation()`
6. Disabled/Loading-Tooltips: `HoverHint` + `useActionHoverTitle` / `useFieldHoverTitle`

## Aktionsmuster (ein Intent → ein Pattern)

Pro Intent genau ein UI-Muster ([`AGENTS.md`](../../../AGENTS.md)):

| Intent | Pattern |
|--------|---------|
| **Create** | Beschrifteter `Button` in `PageHeader` / `SectionHeader` via `SectionActions layout="inline"` |
| **Edit aktuelle Fläche** | `SectionEditAction` (Section-Cards) oder `DetailCardHeader` + `DetailCardAction` (Entity-Detail-Cards) |
| **Row edit/delete** | `ListRow.actions` (oft `IconButton`); Navigations-Zeilen blenden Actions auf Mobile aus |
| **Navigate** | `href` / Chevron oder Hub-Tiles |
| **Selten / destruktiv / Admin-Lifecycle** | `ActionsSection` + `ListRow` (optional `leading={<ActionRowIcon icon={…} />}`, Untertitel als Hinweis; kein Tone-Akzent, kein Chevron) |

Keine zweite Variante für denselben Intent. Detail-Routen: [Frontend Detail-Page-Routing](frontend-detail-routing.md).

## Mobile Aktualisierung

Aktualisieren soll ohne versteckte Geste auffindbar sein und Scrollen nicht unterbrechen. Pull-to-refresh am Seitenanfang; Alternative: **Aktualisieren** im mobilen Menü (kein dauerhafter Header-Refresh-Knopf). Beide Wege teilen `useRefreshServerCache`. Orientierung: [Android](https://developer.android.com/develop/ui/views/touch-and-input/swipe/add-swipe-interface), [Apple UIRefreshControl](https://developer.apple.com/documentation/uikit/uirefreshcontrol), [Material swipe-to-refresh](https://m1.material.io/patterns/swipe-to-refresh.html) - Schwellen unten sind lokale Startwerte.

**Geste** (`MobilePullToRefresh` in der Shell, unterhalb `xl`):

- Nur im Seiteninhalt, wenn ≥300 ms nicht mehr gescrollt wurde
- Erste 20 px ohne Anzeige; überwiegend senkrecht nach unten; seitlich/aufwärts bleibt Scrollen
- Ab 144 px Fingerweg: **Loslassen zum Aktualisieren**; unter 116 px wieder entschärft; Auslösen nur beim Loslassen im bereiten Zustand
- Ausnahmen: Bedienelemente, Nested-Scroll, Header/Nav, Dialoge, fokussierte Inputs, Mehrfingergesten; Abbruch bei Navigation/Desktop

**Daten:** Mindest-Ladeanzeige 600 ms; kein zweiter Abruf bei schnellem Doppeltipp; gesperrt während Speichern. Aktive React-Query-Queries nachladen, inaktive invalidieren - kein Full-Reload. Fehler/Offline: **Erneut versuchen**; Screenreader: **Aktualisiert**.

Kein dediziertes Unit-Test-File für Pull-to-Refresh (`lib/ui/pullToRefresh.ts`). Geräteabnahme: iOS Safari, Android Chrome, installierte PWA.

## Listen-Steuerung (`ListControls`)

Paginierte Feature-Listen nutzen **eine** UI-Schicht aus `list-filter.tsx` + State aus `useListState` (`lib/list/`). Daten kommen serverseitig; die Controls ändern nur `listQuery` (siehe [Paginierung von Listen](pagination.md)).

### UI-Muster

```
[ Anstehend | Vergangen ]          ① Segment (select-Filter, höchstens 3 Optionen sonst seitlich scrollend)
[ Suchen … (volle Breite) ✕ ] [⚙︎ 2] ② Suche + Filter-Icon (Modal: Sheet; Menubar: Kategorie-Zeile ein/aus)
```

**Das ist das einheitliche Muster für alle Listen. Referenzimplementierung: `/admin/users`** (`AdminGlobalUsersPanel`, `lib/list/adminUserList.ts`). Beim Ausrollen auf andere Listen entsteht kein seitenspezifisches Filter-UI: nur die `ListDefinition` (und ggf. `runtimeOptions`) ändert sich.

- **Suche** nutzt die restliche Breite der Zeile (kein Max-Width). Rechts daneben der Filter-Button (40 × 40, nur Icon, Zähl-Badge, `aria-label` mit Anzahl aktiver Filter): **Modal-Layout** öffnet das Sheet; **Menubar-Layout** blendet die Kategorie-Zeile ein/aus. Ohne Suche beschrifteter Button „Filter“ (nur Modal). Ohne Sheet-Filter entfällt der Button.
- **Aktive Sheet-Filter** werden nicht mehr als Chip-Zeile unter der Suche gezeigt. Im **Menubar**-Layout steht der gewählte Wert (oder „n ausgewählt“) direkt im Kategorie-Button; im **Modal**-Layout im Hub-Kurztext bzw. in der Detail-Ansicht. Nicht-gesetzte Werte („Alle“) werden nicht gesendet.
- **Filter-Modal** (`components/ui/modal`, wie alle anderen Dialoge; live angewendet; „Zurücksetzen“ und „Fertig“ im Footer). Ab zwei Sheet-Filtern zuerst **Hub** (eine Zeile pro Kategorie mit Kurztext), Tipp öffnet die Detail-Ansicht nur für diese Kategorie (`onBack` im Modal-Header). Alternativ `sheetLayout: 'menubar'`: rechts neben der Suche dasselbe Filter-Icon (Zähl-Badge bei aktiven Filtern) blendet die Kategorie-Zeile ein/aus (standardmäßig aus); sichtbar dann eine Zeile Kategorie-Pills mit seitlichem Scrollen (kein Zeilenumbruch) und Popover (Mehrfachauswahl als tappbare Zeilen mit Häkchen).

| Export | Rolle |
|--------|--------|
| `ListControls` | Rendert eine `ListDefinition`: `select`-Filter als Segment ganz oben (`ListFilterGroup`, `role="radiogroup"` mit Pfeiltasten und Roving-Tabindex), darunter Suche (`ListSearchInput`) und Filter-Button (`ListFilterPanel`) oder Menubar (`ListSheetFilterMenubar`). Optional `selectCounts` (Zähler im Segment-Label) und `runtimeOptions` (Optionen für `select`-Filter mit `runtimeOptions`, z. B. Dokument-Tags; das Segment fehlt, solange es keine gibt) |
| `ListFilterGroup` / `ListFilterTabs` / `ListFilterTab` | Segment; ab 4 Optionen scrollt es seitlich. Jedes Segment ist ein `select`-Filter der Definition und damit URL-synchron |
| `ListSearchInput` | Suchfeld mit ✕-Button; Debounce liegt in `useListState` |
| `ListFilterPanel` | Filter-Button (nur Icon + Zähl-Badge, wenn eine Suche daneben steht; sonst Button „Filter“) und `Modal` mit `multiSelect`, `dateRange` und `select`-Filtern mit `presentation: 'sheet'`; ab zwei Filtern Hub-Navigation; Zeitraum als Presets. Ohne Sheet-Inhalt entfällt der Button |
| `ListSheetFilterMenubar` | Kategorie-Buttons + Popover (`sheetLayout: 'menubar'`); Mehrfachauswahl live im Popover |
| `ListNoResults` | „Filter ohne Treffer“; ruft `useListState.reset` auf |
| `ListLoadMore` | Nächste Seite über `usePagedList.fetchNextPage` (Button, kein Infinite Scroll) |
| `PaginatedListView` | `paginated-list.tsx`: bündelt `ListControls` + Lade-/Leer-/Filter-leer-Zustände + `ListLoadMore`; pro Liste nur noch `defineList`, `useUrlSyncedListState`/`useListState`, Daten-Hook und Zeilen-`children` |

### Select-Filter: `presentation`, `dependsOn`, rollenabhängige Definitionen

| Option (`ListSelectFilter`) | Wirkung |
|---|---|
| `presentation?: 'segment' \| 'sheet'` | Standard `segment`: voll breites Segment über der Suche (bestehende Listen bleiben unverändert). `sheet`: Sektion im Filter-Sheet bzw. Menubar/Modal. Sheet-Selects haben implizit die erste Option „Alle“ (`''`, Label `allLabelKey`, Standard `list.filters.all`) und setzen kein `defaultValue` |
| Darstellung im Sheet | bis 6 statische Optionen: Einzelauswahl-Chips (`role="radiogroup"`, Roving-Tabindex, Pfeiltasten wie beim Segment); mehr Optionen oder `runtimeOptions`: Dropdown `components/ui/select` |
| `dependsOn?: string` (auf **jedem** Filter) | Key eines anderen Filters. Der Filter ist im Sheet verborgen, bis der Parent einen Wert hat, wird dann nicht gesendet und wird beim Ändern des Parents automatisch geleert (State **und** URL; eine URL mit verwaistem Kind-Wert fällt auf den Standard). Logik in `lib/list/filterState.ts` |

**Rollenabhängige Definitionen:** Eine Definition ist reine Daten. Variiert sie je Rolle, liefert eine Factory eine von wenigen Definitionen auf Modul-Ebene (`getAdminUserListDefinition(isGlobalAdmin)`); die Identität muss stabil bleiben, weil `useListState` seine Effekte daran bindet. Nie eine Definition inline in einer Komponente bauen. Es gibt bewusst kein `hidden`-Flag zur Laufzeit.

**Beispiel `/admin/users`:** `sheetLayout: 'menubar'` - Suche mit Filter-Icon rechts (Kategorie-Zeile Mandant / Organisation / Rolle erst nach Tipp; Popover mit `multiSelect`, live). `GLOBAL_ADMIN`: Mandant-Filter; Organisationen aller Mandanten (`useAdminUserOrganizationFilterOptions`). `TENANT_ADMIN`: nur Organisation + Rolle. Optionen `useAdminTenantOptions` / `useAdminUserOrganizationFilterOptions`; API `tenantId`, `organizationId`, `appRole` (wiederholbar).

**Sortierung (`ListSortMenu`).** Optional per `ListDefinition.sort: { options }` (flache Optionen `value` = `property,asc|desc`, `labelKey`); ohne `sort` gibt es keine Sortier-UI. `ListControls` rendert `ListSortMenu` (`components/ui/list-sort.tsx`) kontextabhängig: mit Suche in der Such-Zeile links neben dem Filter-Button; ohne Suche rechts neben Segment- oder Menubar-Filtern; ohne Suche und ohne Filter allein links in einer Zeile. Icon-Button (`ArrowUpDown`, Maße wie der Filter-Toggle, Punkt-Badge/Hervorhebung bei Sort ≠ Standard) mit Popover und Radiogruppe (Pfeiltasten wie bei den Filter-Chips). Erste Option ist immer „Standard“ = kein `sort` gesendet, das Backend liefert seine natürliche Reihenfolge; danach die Optionen der Definition. `toListQuery` sendet `sort` nur, wenn der Wert in `definition.sort.options` steht. URL-Sync (`useUrlSyncedListState`): Param `sort`, nur wenn gesetzt, unbekannte Werte fallen auf Standard. Sortierung zählt nicht zu `hasActiveFilters`; `reset()` setzt sie auf Standard zurück. Labels je Liste: `<domain>.list.sort.<option>`.

**Muster in Feature-Views:**

1. `const listState = useUrlSyncedListState(xxxListDefinition)` für die Hauptliste der Seite, sonst `useListState(xxxListDefinition)`. Store-Hooks heißen entsprechend `useUrlSynced…ListState` bzw. `use…ListState`.
2. `<PaginatedListView definition={…} state={listState} totalItems={…} …>` unter `PageHeader` (oder weiterhin manuell `ListControls` + Leerzustände wie bisher).
3. Liste aus Hook (`useTodos`, `useNotifications`, …) mit `listQuery: listState.listQuery`; `onLoadMore` an `PaginatedListView` oder `<ListLoadMore … />`.
4. Leerzustände: `emptyState` / `paginatedListEmptyCard` wenn es nichts gibt; `ListNoResults` automatisch, wenn `listState.hasActiveFilters` (Suche oder Sheet-Filter aktiv). Segmente zählen nicht als „aktiver Filter“.
5. Die Listen-Eingabe steuert nur `listQuery`; kein clientseitiges `.filter()`/`.sort()`. `runtimeOptions: true` + `filterOptionsSource` in `defineList` → `GET …/list-filter-options` der Liste. Tags o. Ä. weiterhin per `runtimeOptions`-Prop.

### URL-Sync (`useUrlSyncedListState`)

- `useUrlSyncedListState(definition)` liest den Startzustand aus `useSearchParams` und schreibt Änderungen mit `router.replace(…, { scroll: false })`. Ändern sich die Search-Params von außen (Zurück/Vor, gleiche Route mit anderer Query), liest der Hook sie neu ein; eigene Schreibvorgänge werden dabei erkannt (keine Schleife). Detail öffnen → Zurück liefert denselben Filter; Links mit Filter sind teilbar.
- Parameter: `q` (Suche, erst nach dem Debounce geschrieben), der Key jedes `select`-Filters (z. B. `period`, `status`) und `fromParam`/`toParam` jedes Zeitraums (`from`, `to`). Nur Werte, die vom Standard abweichen, stehen in der URL; unbekannte oder ungültige Werte fallen auf den Standard zurück.
- **Pro Seite höchstens eine synchrone Liste: die Hauptliste.** Lokal bleiben Abschnittslisten in der Personenansicht (`components/organization/user-management/*`), das Gruppenmitglieder-Panel, Modals und Panels (`NotificationPanel`, `HomeTodosCard`).
- Zwei getrennte Hooks statt Option: die Hook-Reihenfolge kann sich nie ändern.
- Zeiträume stehen in der URL als `yyyy-MM-dd`; an die API gehen sie als Organisations-Wanduhr (`from` = `…T00:00:00`, `to` = Folgetag `T00:00:00`, exklusiv; siehe [Zeitbehandlung](time-handling.md)).
- `useSearchParams` verlangt in Next 16 eine `<Suspense>`-Grenze oberhalb der Liste. Sie steht in `app/**/page.tsx` direkt um die Listen-Komponente (Fallback `LoadingState variant="embedded" size="sm"`), damit `page.tsx` dünn bleibt.
- Mitgliederverwaltung: Eine Suche steuert Mitglieder und Eingeladene. Nur der Mitglieder-State ist URL-synchron; die Einladungs-Abfrage leitet ihre Suche aus dessen (initial aus der URL gelesenem) Wert ab.

Gemeinsame Strings: `list.search.*`, `list.filters.*`, `list.dateRange.*`, `list.noResults.*`, `list.loadMore` in `locales/de.json`.

## Neue UI-Komponente hinzufügen

1. Datei unter `frontend/components/ui/<name>.tsx`
2. Named Export, Props-Interface, `cn()`, CSS-Vars; bei Interaktion `'use client'`
3. Domänenlogik **nicht** hier - Feature-Komponente in `components/<domain>/`
4. Shared Tone-Typen → `tones.ts` (Import-Zyklen vermeiden)
5. Kein CLI/Generator

Siehe auch: [Frontend Detail-Page-Routing](frontend-detail-routing.md), [Frontend i18n](frontend-i18n.md), [Frontend Auth](frontend-auth.md).
