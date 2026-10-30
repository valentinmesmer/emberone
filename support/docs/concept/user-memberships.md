# Org- und Gruppen-Mitgliedschaften

Design-Notizen für die Mitgliedschafts-Kaskade: erst Org-Zugehörigkeit, dann Gruppenzugehörigkeit (die Permissions gewährt). Package roots: `com.emberone.userorganizationmembership`, `com.emberone.userorganizationgroupmembership`. Verwandt: [Organisation](organization.md), [Sicherheit](security-and-permissions.md), [User Permissions](userpermission.md).

## Mental Model

| Ebene | Package | Bedeutet | Allein ausreichend für Permissions? |
|-------|---------|----------|-------------------------------------|
| **Org-Membership** | `userorganizationmembership` | User gehört zur Org (`main`, Datumsbereich) | Nein |
| **Group-Membership** | `userorganizationgroupmembership` | User in Org-Gruppe (`LEADER` / `DEPUTY_LEADER` / `MEMBER`) | Ja (via PermissionGroup-Mappings), gated durch aktive Org-Membership |

```
UserOrganizationMembership (aktiv)
        │
        ▼
UserOrganizationGroupMembership ──► PermissionGroupMapping ──► PermissionTypes
        │
        ▼
CachedUserPermissionLoader / RequestScopedUserPermissions
```

`GroupMembershipType` steuert UX/Führung - **nicht** die ACL (Permissions kommen aus Group→PermissionGroup-Mappings).

## Org-Membership (`userorganizationmembership`)

### Was es besitzt

- `UserOrganizationMembership` (`main`, `startDate`, `endDate`, `endProcessedAt`)
- `UserOrganizationMembershipService` (create/update/`endMembership`)
- Active-Cache; Events Created/Updated/Ended → Permission-Cache-Eviction + thin Realtime List-Topic `organization-memberships` (`organization-membership.changed`) für Mitgliederlisten
- Org-Mitgliederliste (paginiert, `ListQuery`; `sort`: `lastName`, `firstName`): `GET /api/v1/auth/users/organizations/{organizationId}` - nur aktive Mitglieder; offene Einladungen **separat** `GET /api/v1/auth/organizations/{organizationId}/onboarding-invitations` (ohne `sort`; pending-Regeln serverseitig). Gruppen: `GET /api/v1/auth/organizations/groups?organizationId=…` mit Filter `status=ACTIVE|ARCHIVED|ALL` (ersetzt `includeArchived`). Gruppenmitglieder: `GET …/groups/{groupId}/members` (`sort`: `lastName`, `firstName`). Siehe [Paginierung](pagination.md).
- Auth-Controller: `PUT /api/v1/auth/users/organization-memberships/{membershipId}` (`USER_WRITE`) nur für Update (`startDate` only); **Anlegen** nur über Join-/Onboarding-Workflows (`ORGANIZATION_JOIN_REQUEST`, Einladung), kein direkter Create-Endpunkt (Zustimmung des Users); **Beenden** nur per Workflow - Self `ORGANIZATION_LEAVE_REQUEST` (Typ 8), Org-Verwaltung `ORGANIZATION_MEMBERSHIP_END_REQUEST` (Typ 13, `POST …/organizations/{organizationId}/users/{userId}/workflows/organization-membership-end`)
- Kein Hard-Delete von Membership-Zeilen (weder Org noch Gruppe, inkl. Admin-Leadership). Beenden setzt `endDate`. Kein Produkt-API-Hard-Delete für User/Org (Ausnahmen: `USER_MASTER`-Retention, Mandanten-Purge). **User-Deaktivierung** beendet alle aktiven Org- und Gruppen-Mitgliedschaften des Users (Leadership inklusive, ohne separaten Admin-Schritt). **Org-Deaktivierung (Admin)** auf einem Org-Unterbaum: Hauptorg in betroffenen Orgs → vollständige User-Deaktivierung; sonst `endMembership` über den Deaktivierungs-Pfad (Leadership-Guard umgangen). Siehe [Organisation](organization.md). Initial-Member: Admin `POST …/leader-user` → `createInitialTenantMember` (main Org-Membership + Leadership). Weitere Leadership-Pfade: `POST/PUT …/leadership-group-memberships` (`createSystemManagedMembership` / `updateSystemManagedMembership`).

### Was es nicht besitzt

Gruppenmitgliedschaften, PermissionGroups, Org-Hierarchie.

### Workflow-getriebene Änderungen

Neben Admin-/Auth-CRUD können Memberships über Workflows entstehen, wechseln oder enden (siehe [Workflow-Engine](workflow-engine.md)):

| Workflow | Effekt bei Approve |
|----------|--------------------|
| `ORGANIZATION_JOIN_REQUEST` | Aufnahme mit Onboarding (nicht alle Steps live erfüllt) → `OPEN`-Einladung, keine Membership. Direktaufnahme (alle Steps live erfüllt oder keine konfiguriert) → leere `APPROVED`-Einladung + `ensureOrganizationMembershipAndAllMembersGroup` (nur `createMembership`; ALL_MEMBERS via `AllMembersGroupMembershipListener`). |
| `ONBOARDING_SUBMISSION` | `ensureOrganizationMembershipAndAllMembersGroup` (nur `createMembership`; ALL_MEMBERS via sync `@EventListener` auf `OrganizationMembershipCreatedEvent`) + `promote` aller Onboarding-Steps; Einladung `APPROVED` |
| Admin `recordCompletedOnboarding` | Nur `promote` + `APPROVED`. Membership besteht bereits (z. B. Leiter nach `leader-user`); kein Workflow |
| `MAIN_ORGANIZATION_CHANGE` (Typ 7, Self) | `changeMainOrganization` auf bestehende Membership der neuen Haupt-Org |
| `ORGANIZATION_USER_MAIN_ORGANIZATION_CHANGE` (Typ 14, Org) | wie Typ 7 für ein anderes Mitglied (Org `USER_WRITE`) |
| `ORGANIZATION_LEAVE_REQUEST` | Austritt aus gewählter Org (Self); Hauptorg → vollständiger Konto-Austritt (`deactivateUser`), sonst `endMembership` mit `now` |
| `ORGANIZATION_MEMBERSHIP_END_REQUEST` | wie Typ 8 für ein anderes Mitglied (Org `USER_WRITE`) |
| `ORGANIZATION_GROUP_JOIN_REQUEST` | `createMembership(..., MEMBER)` für die Ziel-Gruppe (nur Custom, nur mit aktiver Org-Membership) |
| `ORGANIZATION_GROUP_LEAVE_REQUEST` | `endActiveMembership` für die gewählte Custom-Gruppe |

Self-Übersicht: `GET /api/v1/self/organizations/memberships` (`@BoundedList`, `SelfMembershipHistoryService`) liefert alle begonnenen Org- und Gruppen-Zeiträume des Users inkl. beendeter (`active`, `startDate`, `endDate`; serverseitig sortiert: aktive zuerst - bei Organisationen die aktive Hauptorganisation vor den übrigen -, danach nach Ende und Beginn absteigend; UI-versteckte Gruppen wie `ALL_MEMBERS` entfallen); Frontend: Stammdaten → Karte „Organisationen" (Aktuell / Früher), Anträge in eigener Karte „Mitgliedschaft ändern". `GET …/organizations/tree` enthält nur aktive Zugehörigkeiten (Hierarchie für Dialoge).

Self-Picker: `GET /api/v1/self/organizations/joinable`, `.../main-change-targets`, `.../leave-targets`, `.../group-join-targets`, `.../group-leave-targets`.

### Mitgliedschaftsübersicht (Org-UI)

`GET /api/v1/auth/organizations/{organizationId}/users/{userId}/membership-overview` (`USER_READ`) liefert Org-Memberships, Gruppen inkl. Memberships, `canWrite`, `inLeadershipGroup`, `canRequestOrganizationMembershipEnd` und `canRequestOrganizationUserMainOrganizationChange` (jeweils nur wenn Workflow aktiv und fachlich erlaubt). Picker für Hauptorg-Wechsel (Typ 14): `GET …/users/{userId}/main-change-targets`. Org-Mitgliedschaft per UI nur noch `startDate` bearbeiten; Austritt über Workflow Typ 8/13 (sofort nach Freigabe).

### Leadership blockiert Org-Ende und Austritt

Aktive Mitgliedschaft in der **Leitungsgruppe** (`OrganizationGroupType.LEADERSHIP`) einer Org ist eine harte Backend-Invariante, kein reines UI-Filter:

| Aktion | Verhalten |
|--------|-----------|
| Org-REST `updateMembership` (nur `startDate`) | `OrganizationMembershipEndBlockedByLeadershipException` |
| `endMembership` | dieselbe Exception |
| `ORGANIZATION_LEAVE_REQUEST` / `ORGANIZATION_MEMBERSHIP_END_REQUEST` open/update/approve | `WorkflowValidationException` `LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE` |
| `ORGANIZATION_GROUP_LEAVE_REQUEST` für die Leitungsgruppe | `WorkflowValidationException` `LEADERSHIP_GROUP_LEAVE_NOT_ALLOWED`; `endActiveMembership` lehnt Systemgruppen ab |
| `ORGANIZATION_GROUP_JOIN_REQUEST` für die Leitungsgruppe | `WorkflowValidationException` `LEADERSHIP_GROUP_JOIN_NOT_ALLOWED` |
| `GET …/leave-targets` | Orgs, in deren Leitungsgruppe der User aktiv ist, **fehlen** |
| `GET …/group-leave-targets` | Leitungsgruppe und ALL_MEMBERS **fehlen** |
| `GET …/group-join-targets` | Leitungsgruppe, ALL_MEMBERS und Gruppen, in denen der User schon Mitglied ist, **fehlen** |

Erst wenn Tenant-Admin die Leitungsmitgliedschaft über `PUT …/leadership-group-memberships/{id}` mit `endDate` beendet, sind Org-Bearbeitung, Org-Ende und Org-Austritt wieder möglich. Org-UI und Self-Service dürfen Leadership-Memberships nicht ändern (`SystemGroupModificationException`).

Frontend: `leave-targets` / `group-leave-targets` / `group-join-targets` sind die Picker-Listen (Leadership wird zusätzlich clientseitig aus Gruppen-Targets gefiltert). Mitgliedschaftsübersicht: **Bearbeiten** (`startDate`) nur wenn `canWrite && !inLeadershipGroup`; **Beenden** nur wenn `canRequestOrganizationMembershipEnd` (prüft Workflow/`USER_WRITE`/aktive Membership - **kein** Leadership-Check; Backend lehnt Leave/Ende bei Leadership weiterhin mit `LEADERSHIP_MEMBERSHIP_BLOCKS_LEAVE` ab).

Check: `AllMembersGroupService.hasActiveLeadershipMembership(userId, organizationId)`.

### Fallstricke

- Service `createMembership` ist intern (Join-/Onboarding-/Invite-Direktaufnahme); Request hat kein `endDate`-Feld (Zeile startet immer offen), `main` explizit. Org-REST `updateMembership` setzt kein `endDate`. Beenden im Produkt: Workflow Typ 8/13 (Approve → `endMembership` mit **`now`**, kein Wunschtermin im Payload).
- Service `endMembership` (intern, kein Org-REST): setzt `endDate`. `OrganizationMembershipEndedEvent` (Gruppen-Cascade, Permissions) erst wenn `endDate` in der Vergangenheit liegt - sofort oder per `MembershipEndCleanupScheduler`. `endProcessedAt` markiert angewandte Seiteneffekte; der minütliche Job holt unprocessed Zeilen nach (auch nach Downtime). Zukünftiges `endDate` am Service ist **kein** geplanter Austritt über UI/Workflow.
- `startDate` darf bei Org-Create/Update nicht in der Zukunft liegen.
- Overlap-Check pro User+Org; `main` Default = true wenn User noch keine main hat.
- Leadership-Mitglieder: Org-Update, Org-Ende und Leave-Requests sind serverseitig blockiert, bis Admin die Leitungsmitgliedschaft beendet. Leave-Target-APIs listen diese Orgs/Gruppen nicht.

## Gruppen-Membership (`userorganizationgroupmembership`)

### Was es besitzt

- `UserOrganizationGroupMembership`
- Service inkl. `*SystemManaged*` für Leadership/Admin (umgeht ALL_MEMBERS-Manual-Guard)
- Auth-Controller: `/api/v1/auth/users/organization-group-memberships`
- Aktivierung → `OrganizationGroupMembershipActivatedEvent`
- Deaktivierung → `OrganizationGroupMembershipDeactivatedEvent` (Workflow-Claims freigeben); synchrone Gruppen-Cascade auf Org-Membership-Ende (gleiche Transaktion wie `endProcessedAt`)

### Was es nicht besitzt

Gruppen selbst, PermissionGroupMappings, `GroupMembershipType`-Enum (liegt in `organizationgroup`).

### Fallstricke

- Manuelle Membership in `ALL_MEMBERS` ist geblockt (Auto-Sync).
- Manuelle Membership in `LEADERSHIP` ist auf normalen APIs geblockt; nur Tenant-Admin (`createSystemManagedMembership` / `updateSystemManagedMembership`). Leave-Request und `endActiveMembership` lehnen Leadership ebenfalls ab.
- Admin Leadership nutzt System-managed Create über `AdminOrganizationController`.
- `startDate` darf bei Create/Update von Org- und Gruppenmitgliedschaften nicht in der Zukunft liegen (vorerst; zukünftige Starts bleiben für später reserviert). Bestehende künftige Starts werden erst aktiv, wenn das Datum erreicht ist bzw. per Update angepasst wird, kein Start-Cron.
- **Intervalle:** Org- und Gruppenmitgliedschaften sind halboffene Zeiträume `[start_date, end_date)` (Instant): zum `end_date` ist die Zeile nicht mehr aktiv, aneinandergrenzende Zeilen (Ende = Start der nächsten) überlappen nicht; `end_date` muss nach `start_date` liegen. Gilt einheitlich für `isActiveAt`, Overlap-Prüfung und alle `…Active…`-Queries; fällig zum Aufräumen ist `end_date <= now`. Kalendertag-Intervalle (`LocalDate`: Abwesenheit, Dienstgrad, Qualifikations-/Zertifikatsentscheidungen, Lehrgänge) bleiben auf den Tag inklusiv (`bis` = letzter Tag).
- **Rollenwechsel** (`membershipType` ändert sich, z. B. Leitung → Mitglied) beendet die aktive Zeile mit `endDate = now` (`endProcessedAt = now`) und legt eine neue Zeile mit neuem Typ ab `now` an; die frühere Rolle bleibt in der Historie (Funktionshistorie: [Dienstgrad und Funktion](rank-and-function.md)). `startDate` des Requests wird dabei nicht angewandt, `endDate` gilt für die neue Zeile; keine Aktivierungs-/Deaktivierungs-Events (User bleibt in der Gruppe). Rollenwechsel einer beendeten Mitgliedschaft wird abgelehnt (`IllegalArgumentException`).
- Gruppen-`updateMembership` darf `endDate` setzen (Vergangenheit → Deaktivierungs-Events sofort; Zukunft → Cleanup-Job). Org-Mitgliedschaft: siehe Fallstricke oben (kein `endDate` per Org-REST).
- Fällige `endDate`s mit leerem `endProcessedAt` verarbeitet `MembershipEndCleanupScheduler` minütlich (`OrganizationMembershipEndedEvent` / `OrganizationGroupMembershipDeactivatedEvent`).

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Org-Membership | `userorganizationmembership/{domain,service,controller,…}/` |
| Org-Membership Realtime | `userorganizationmembership/realtime/{OrganizationMembershipRealtimeListener,OrganizationMembershipRealtimeNotifier}.java` |
| Group-Membership | `userorganizationgroupmembership/{domain,service,controller,…}/` |
| Ended→Groups | `userorganizationgroupmembership/.../OrganizationMembershipEndedGroupListener.java` |
| ALL_MEMBERS-Sync | `organizationgroup/service/AllMembersGroupMembershipListener.java` → `AllMembersGroupService.ensureUserInAllMembersGroup` |
| Leadership-Guard | `organizationgroup/service/AllMembersGroupService.java` (`hasActiveLeadershipMembership`) |
| End-Cleanup-Cron | `userorganizationmembership/schedule/MembershipEndCleanupScheduler.java` |
| Membership-Overview | `user/controller/UserMembershipOverviewController.java`, `user/service/UserMembershipOverviewService.java` |

Siehe auch: [Realtime](realtime.md).
