# Dienstgrad und Funktion

Design-Notizen für Mandanten-Dienstgrade, User-Historie und aufgelöste Organisationsfunktionen. Packages: `com.emberone.rank`, `com.emberone.dutyfunction`. Bundesland-Baseline und Standard-Auswahl: [Bundesland](federal-state.md), [Standard](standard.md). Onboarding-Schritt `RANK`: [Onboarding](onboarding.md). Erstvergabe per Antrag: [Workflow-Engine](workflow-engine.md) (`RANK_REQUEST`, Id 16). Berechtigungen: [Sicherheit & Berechtigungen](security-and-permissions.md) (`RANK_READ`/`RANK_WRITE`, Id 35/36).

## Mental Model

```
TenantRank (Katalog) ──► UserRank (max. eine offene Zeile, Historie)
std_rank_selections / std_function_selections ──► Admin-Vormerkung im Standard (kein Laufzeit-Filter für Dienstgrade; Funktionen nur für Grant-Dropdowns)
std_function_grants (Gruppe + Rolle) ──► ResolvedDutyFunction (Lesezeit)
Gruppenmitgliedschaft (start_date/end_date) ──► Funktion inkl. Historie (mehrere möglich; kein Workflow)
```

## Kernentitäten

| Entität | Bedeutung |
|---------|-----------|
| `tenant_ranks` | Mandanten-Katalog (Reihenfolge über `sort_order`) |
| `user_ranks` | Intervall `start_date`/`end_date`, optional `reason`; genau eine offene Zeile |
| `tenant_functions` | Benannte Funktionen (Kommandant, …) |
| `std_rank_selections` | Standard markiert nutzbare Dienstgrade (Admin/Clone; Vergabe nutzt aktiven Mandanten-Katalog) |
| `std_function_selections` | Standard markiert nutzbare Funktionen für Grant-Zuordnungen |
| `std_function_grants` | Mapping Org-Gruppe + `membership_type` → Funktion |

## API (Überblick)

| Bereich | Pfad | Gate |
|---------|------|------|
| Admin Katalog Ränge/Funktionen | `GET/POST …/admin/tenants/{tenantId}/ranks` bzw. `…/functions` | `canAccessTenant` |
| Admin Standard | `PUT …/standards/{id}/rank-selections`, `…/function-selections`, `…/function-grants` | `canAccessTenant` |
| Self Dienstgrad | `GET /api/v1/self/rank`, `…/available-ranks` (`@BoundedList`) | `isSelfServiceUser()` |
| Self Funktionen | `GET /api/v1/self/functions` (`@BoundedList`) | `isSelfServiceUser()` |
| Org Übersicht | `GET …/organizations/{id}/ranks` (paginiert: `ListQuery`, Suche Name, Sort `lastName` / `firstName`; die Gruppierung nach Dienstgrad ist reine Darstellung, Mitglieder je Gruppe behalten die Server-Reihenfolge), `…/available-ranks` (`@BoundedList`), `…/functions/overview?search=` (nicht paginiert; serverseitige Token-Suche, case-insensitiv: Treffer auf Funktions-/Gruppenname behält alle Mitglieder, sonst nur Mitglieder mit Treffer auf Vor-/Nachname; Gruppen ohne Treffer entfallen) | `RANK_READ` / `RANK_WRITE` (available-ranks) |
| Org User | `GET/POST …/organizations/{id}/users/{userId}/rank`, `GET …/users/{userId}/functions` (`@BoundedList`) | `RANK_READ` / `RANK_WRITE`; Schreiben nur Hauptorganisation des Users |
| Antrag | `POST /api/v1/auth/workflows/rank-request` | `canBeginSelfServiceWorkflow(RANK_REQUEST)` |

Frontend: `/my-area/rank`, `/organizations/[id]/ranks`. Standard-Admin: Funktions-Grants in der Organisationsgruppe im Bearbeiten-Dialog (Rolle → Funktion); Detailseite und aktivierte Funktionen zeigen Zuordnungen nur kompakt an.

## Vergabe-Regeln

- **Org `POST …/rank`:** Promotion möglich (offene Zeile schließen, neue öffnen); nur wenn Pfad-Org = aktive Hauptorg des Users.
- **Onboarding / `RANK_REQUEST`:** nur `assignFirstRank` (kein offener Dienstgrad).
- **`RANK_REQUEST`:** aktive Hauptmitgliedschaft, kein offener `UserRank`, Ziel-Rang aktiv; optional Anhänge.
- **Funktionen:** Auflösung zur Lesezeit aus Grants + Gruppenmitgliedschaften. Eine Funktion beginnt mit `start_date` und endet mit `end_date` der Gruppenmitgliedschaft (Mitgliedschaften werden nie gelöscht); `GET …/functions` liefert aktive und beendete Funktionen (`active`, `membershipStartDate`, `membershipEndDate`), noch nicht begonnene entfallen. Die Funktionsübersicht der Organisation zeigt weiterhin nur aktive Mitglieder. Grant pro Org-Gruppe und `membership_type` (`LEADER` / `DEPUTY_LEADER` / `MEMBER`). Grants nur für Org-Gruppen vom Typ `LEADERSHIP` oder `CUSTOM` (nicht `ALL_MEMBERS`).

## Quellcode-Karte

| Bereich | Pfad |
|---------|------|
| Katalog Admin | `AdminTenantRankController`, `AdminTenantDutyFunctionController` |
| Org/Self API | `OrganizationRankController`, `UserRankOrganizationController`, `UserRankSelfController`, `UserDutyFunctionSelfController` |
| Onboarding | `RankStepHandler` |
| Workflow | `RankRequestWorkflowHandler`, `RankRequestWorkflowController` |
| Standard-Domains | `RankStandardDomain`, `FunctionSelectionStandardDomain`, `DutyFunctionStandardDomain` |

## Verwandte Docs

- [Organisation & Mitgliedschaft](organization.md) - Hauptorganisation für Schreiben des Dienstgrads
- [Lifecycle & Statusfelder](lifecycle-and-status-fields.md) - `end_date` der Dienstgrad-Intervalle
- [User-Löschung: Fremdschlüssel](user-delete-foreign-keys.md) - `user_ranks` CASCADE
