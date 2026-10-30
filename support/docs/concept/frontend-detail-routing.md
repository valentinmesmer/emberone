# Frontend Detail-Page-Routing

Design-Notizen zum App-Router-Muster für Entity-Detailseiten. Next.js unter `frontend/app/`; Views in `frontend/components/…`. Shell: [Component-Library](frontend-component-library.md) (`BackButton`). Daten: [API-Client](frontend-api-client.md); Gates: [Auth](frontend-auth.md).

## Mental Model

```
Route-Split (Self / Org / Admin)
        │
        ▼
dünne page.tsx  → Params + Permission-/Auth-Gate
        │
        ▼
*DetailView (components/…)  → Store-Hooks + ui/*
```

Drei getrennte Flächen (wie Backend-APIs):

| Fläche | App-Pfad | API-Prefix |
|--------|----------|------------|
| **Self** | `/my-area/…` | `/api/v1/self/…` |
| **Org** | `/organizations/[id]/…` | `/api/v1/auth/organizations/{id}/…` |
| **Admin** | `/admin/…` (Tenants, Users, …) | `/api/v1/auth/admin/…` |

Nicht vermischen: Org-Feature-UI unter `/organizations/…`, Plattform-/Mandanten-Admin unter `/admin/…` (inkl. `/admin/users`, `/admin/users/[userId]`).

## App-Struktur

```
app/
├── layout.tsx                 # Root: Serwist -> Theme -> Query -> Auth -> Realtime -> Permission -> Locale -> Toast
├── my-area/                   # Self-Service („Mein Bereich“)
│   ├── layout.tsx             # Auth-Gate + MobileNavigationLayout
│   └── …/[entityId]/page.tsx
├── organizations/[id]/        # Org-Workspace
│   ├── layout.tsx
│   └── …/[nestedId]/page.tsx
├── admin/                     # Administration
│   ├── users/                 # /admin/users, /admin/users/[userId]
│   └── tenants/[id]/…
├── auth/{callback,silent-callback}/
├── source/                    # öffentlich (AGPL-Hinweis)
├── serwist/[path]/route.ts
└── ~offline/page.tsx
```

## Param-Konventionen

Param-Namen sind **semantisch**, nicht generisch `[id]` überall:

| Segment | Bedeutung |
|---------|-----------|
| `[id]` | Org- oder Tenant-Kontext |
| `[absenceId]`, `[todoId]`, `[workflowId]`, … | Entity |
| `[definitionId]` / `[recordId]` | Zertifizierung/Qualifikation-Hierarchie |
| `[configKey]` | Standard-Config (Admin) |
| `[userId]` | User (Org-Verwaltung oder `/admin/users/[userId]`) |

Verschachtelung folgt der Domäne, z. B.  
`/organizations/[id]/users/[userId]/certifications/[definitionId]/records/[recordId]`.

## Kernmuster: dünne `page.tsx`

```
page.tsx ('use client')
  → useParams() → parseInt → Validierung
  → optional: usePermissions / Org-Load Gate / canAccessAdministration
  → *DetailView (components/…)
```

Beispiele: `my-area/todos/[todoId]/page.tsx` → `TodoDetailView`; `my-area/onboarding/[invitationId]/page.tsx` → `MyAreaOnboardingWizard`; Org-Time-Entries mit Permission-Gate; `admin/users/[userId]/page.tsx` → `AdminUserDetailView`. Aktuell **33** `*DetailView.tsx` unter Feature-Packages.

## Layout & Navigation

| Mechanismus | Rolle |
|-------------|-------|
| `MobileNavigationLayout` | Shell; rendert automatisch `<BackButton />` |
| `PageHeader` / `PageContentSection` | Content-Chrome (Listen/Hubs; DetailViews oft `Card` + `FieldDisplay` / `DetailCardHeader`) |
| `BackButton` + `useParentPath()` | History-back bevorzugt, sonst hierarchischer Parent |
| `BackHrefOverrideProvider` | Override für Sonderfälle |

Fehler/Laden: `LoadingState`, `NoticeCard`. Aktionen: [Component-Library Aktionsmuster](frontend-component-library.md#aktionsmuster-ein-intent--ein-pattern).

## AppLinks (My-Area vs. Org)

Backend-`navigationPath` (`AppNavigationResolver`) ist der **Self-/My-Area-Kanon** (Todos, Notifications, Dokument-Links aus der API).

In der **Org-User-Verwaltung** mappt das Frontend Ziele kontextabhängig über `resolveAppNavigationHref` in `frontend/lib/utils/appLink.ts` (`linkType` + `linkId` + `organizationId`/`userId`) - ohne zweite URL-Logik im Backend.

Sonderfall **Führerschein**: keine Record-Detail-Route; Link führt zur Section `/my-area/driving-licenses` bzw. `…/users/{userId}/driving-licenses`.

Sonderfall **`CERTIFICATION_EVENT_GRANT`**: Backend liefert bereits den Org-Event-Pfad (`…/department/{service|operation}/{eventId}`); Frontend zeigt den Herkunfts-Link nur bei Event-Read-Permission. Grant-Admin-Detail nur aus dem Event-UI.

**Permission-Gate (ein Muster):** Pfadauflösung in `frontend/lib/utils/appLink.ts`; Sichtbarkeit in `frontend/lib/utils/sourceLink.ts` (`resolveGatedAppNavigationHref` / Event-/Workflow-/Onboarding-Helper). Org-Links brauchen die passende Read-Permission (`WORKFLOW_READ`, `USER_READ`, Cert/Training/Driving-Read, Event-Read). Self-/My-Area-Links (Todos, Notifications, eigene Dokumente) bleiben ungegated. UI-Herkunftszeile: `RecordSourceLinkSection`.

## Daten in DetailViews

- React-Query-Hooks aus `@/lib/store`
- Listen oft im Cache → Detail sucht per ID
- Toast für Erfolg/Fehler; Edit/Delete über Modals bzw. `DetailCardAction` / `ActionsSection`
- Nach Delete: `router.push` auf Listen-Pfad

Siehe auch: [Frontend API-Client](frontend-api-client.md), [Frontend Auth](frontend-auth.md), [Frontend Component-Library](frontend-component-library.md).
