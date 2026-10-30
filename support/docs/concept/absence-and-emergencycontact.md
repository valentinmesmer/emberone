# Abwesenheit & Notfallkontakt

Design-Notizen für zwei verwandte Self-/Org-HR-Module. Package roots: `com.emberone.absence`, `com.emberone.emergencycontact`. Verwandt: [User](user.md), [Sicherheit](security-and-permissions.md), [Aufbewahrung und Löschung](retention-and-erasure.md).

## Mental Model

Zwei getrennte Domänen unter einem Doc: **Abwesenheit** (zeitliche Abwesenheit mit Org-Links, keine Gesundheitsdaten) und **Notfallkontakt** (priorisierte Kontakte pro User). Beide folgen Self-/Org-API-Trennung und Permission-Gates (`ABSENCE_*` / `EMERGENCY_CONTACT_*`).

## Abwesenheit (`absence`)

`UserAbsence` mit Grund (`VACATION` / `PERSONAL` / `TRAINING` / `OTHER`) und Multi-Org-Links (`UserAbsenceOrganization`). Es gibt **keine** strukturierten Gesundheitsangaben (kein Krankheitsgrund, kein Attest-Feld). Im Kommentar weist die UI auf „keine Gesundheitsangaben“ hin; Freitext bleibt möglich. Löschklasse `ABSENCE` (Retention ab `end_date`, Hard Delete; siehe [Aufbewahrung und Löschung](retention-and-erasure.md)). Absence-Listen (Self, User-Org, Org-aktiv `…/user-absences/active`): paginiert (`ListQuery` + `AbsenceListFilter`) mit `period=CURRENT|UPCOMING|PAST` (fehlend: `UPCOMING`; `from`/`to` entfallen). „Heute“ nach Organisations-Wanduhrzeit ([time-handling](time-handling.md)). `CURRENT`: `startDate <= today <= endDate`, Sortierung `startDate` aufsteigend; `UPCOMING`: `endDate >= today` (laufend und künftig), `startDate` aufsteigend; `PAST`: `endDate < today`, `startDate` absteigend. Ohne `sort` folgt die Sortierung der Periode (`pageable(query, sortOverride)`); Vorrang: Client-`sort` > Periode > Standard. Erlaubte `sort`-Keys (je `asc`/`desc`): `startDate`; zusätzlich `lastName` (Nachname, dann Vorname) nur in der Org-Liste. Suche (`search`) über `user.firstName` und `user.lastName` (nicht mehr über den Kommentar).

Notfallkontakt-Listen-GETs: unverändert als begrenzte Sammlung (`@BoundedList`), kein `PageDto`.

### Was es besitzt

- CRUD inkl. Ersetzen der Org-Links (flush-before-insert)
- Create/Update/Delete prüft `ABSENCE_WRITE` **pro verknüpfter Org** via `OrganizationAccessService`
- Self-, User-Org- und Org-Listen-Controller

| Controller | Prefix |
|------------|--------|
| `UserAbsenceSelfController` | `/api/v1/self/absences` |
| `UserAbsenceUserOrganizationController` | `/api/v1/auth/organizations/{organizationId}/users/{userId}/absences` |
| `UserAbsenceOrganizationController` | `/api/v1/auth/organizations/{organizationId}/user-absences/active` |

### Fallstricke

Self-**GET** braucht nur `isSelfServiceUser()` (kein `ABSENCE_WRITE`). Self-Write (Create/Update/Delete) und Org-Write verlangen `ABSENCE_WRITE` auf allen verknüpften Orgs - ohne Write-Permission schlägt der Service fehl. Mindestens eine Org erforderlich.

### Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Domain | `absence/domain/UserAbsence.java`, `UserAbsenceOrganization.java` |
| Service | `absence/service/UserAbsenceService.java` |

---

## Notfallkontakt (`emergencycontact`)

`UserEmergencyContact` mit Relationship-Enum und **Priority-Ordnung** (unique `tenant_id, user_id, priority`).

### Was es besitzt

- CRUD, Reorder (temporärer Priority-Offset gegen Unique-Key), Renumber nach Delete
- Self braucht **keine** Write-Permission (nur `isSelfServiceUser`)
- Org-APIs: `EMERGENCY_CONTACT_READ` / `WRITE`

| Controller | Prefix |
|------------|--------|
| `EmergencyContactSelfController` | `/api/v1/self/emergency-contacts` |
| `EmergencyContactUserOrganizationController` | `/api/v1/auth/organizations/{organizationId}/users/{userId}/emergency-contacts` |
| `EmergencyContactOrganizationController` | `/api/v1/auth/organizations/{organizationId}/emergency-contacts` |

### Fallstricke

Reorder muss die exakte ID-Menge treffen. User-Adresse liegt in `UserContactInformation`, nicht hier. Onboarding-Promote: `EmergencyContactService.createFromOnboarding`.

### Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Domain | `emergencycontact/domain/UserEmergencyContact.java` |
| Service | `emergencycontact/service/EmergencyContactService.java` |
