# Qualifikation

Design-Notizen für die Qualifikations-Domäne - einer der größten, am wenigsten selbsterklärenden Backend-Cluster (Exercise Status & Sync, Catalog & Tenant Repositories, Org Decision Intervals, …). Package root: `com.emberone.qualification`.

## Mental Model

| Konzept | Was es ist | Gespeichert als |
|---------|------------|-----------------|
| **TrainingCourse** | Kurs, den ein User absolvieren kann | Mandanten-Typ + Submissions / granted types |
| **DrivingLicense** | Führerscheinklasse, die ein User besitzt | Mandanten-Definition + Führerschein-Dokument / Klassen |
| **Certification** | Zeitlich begrenzte Berechtigung | Mandanten-Definition + Records (`VALID`, `EXPIRED`, …) |
| **Qualification** | **Berechnetes Übungsrecht** für User **in einer Organisation** | Definition + optionale datierte Org-Entscheidungen; **Status nicht persistiert** |

Zertifizierung / Training sind Dinge, die man *hat* oder *abschließt*. Qualifikation ist, ob man eine Fähigkeit in einer Org *ausüben* darf - oft erst nach explizitem Org-Grant.

```
TenantQualificationDefinition  (Mandanten-Katalog)
  ├── trainingCourses[] + mode ALL|ANY
  ├── certDefinitions[] + mode ALL|ANY
  ├── drivingLicenses[] + mode ALL|ANY
  └── requiresOrgDecision

Standard ──selects──► QualificationSelection(configKey)
Organization ──effektiver Standard──► Katalog-Teilmenge (Read-Time)

User-Status (pro Org) = Voraussetzungen + Org-Entscheidungsintervalle
```

Ein Standard **wählt** nur, welche Definitionen im Org-Katalog erscheinen (`QualificationStandardDomain`). Keine per-Org-Materialisierungszeilen.

## Kernentität: `TenantQualificationDefinition`

Tabelle `tenant_qualification_definitions`:

| Feld | Rolle |
|------|-------|
| `configKey`, `name`, `description` | Identität / Anzeige. `config_key` ist das Kürzel; Rename zieht Standard-Selections mit. |
| `requiresOrgDecision` | Wenn false → `CAN_EXERCISE` allein aus Voraussetzungen |
| `trainingCourseRequirementMode` / `certRequirementMode` / `drivingLicenseRequirementMode` | `ALL` \| `ANY` |
| `active`, `archived` | Katalog-Filter |
| `trainingCourses` / `certDefinitions` / `drivingLicenses` | Voraussetzungs-Links per Config Key |

Mindestens eine Training-, Cert- oder Führerschein-Voraussetzung, oder `requiresOrgDecision = true`. Admin-CRUD: `AdminTenantQualificationController` → `TenantQualificationDefinitionService`.

## Status-Zustandsautomat

`QualificationExerciseStatus` - berechnet von `QualificationStatusService.evaluateStatus` (package-private `resolveStatus` hinter `evaluateItem`):

```
                    prerequisitesMet?
                         │
              ┌──────────┴──────────┐
              NEIN                  JA
              ▼                     ▼
    PREREQUISITES_MISSING    requiresOrgDecision?
                                   │
                        ┌──────────┴──────────┐
                        NEIN                  JA
                        ▼                     ▼
                  CAN_EXERCISE         aktiver Grant heute?
                                             │
                                  ┌──────────┴──────────┐
                                  JA                    NEIN
                                  ▼                     ▼
                            CAN_EXERCISE         revoke deckt heute ab?
                                                       │
                                            ┌──────────┴──────────┐
                                            JA                    NEIN
                                            ▼                     ▼
                                  ORG_DECISION_REVOKED    ORG_DECISION_DENIED
```

| Wert | Bedeutung |
|------|-----------|
| `NOT_AVAILABLE` | Enum-Wert vorhanden, aber **unbenutzt**: Backend setzt/synced ihn nie als Status (kein Persistenz-Status, kein evaluate-/Sync-Pfad liefert ihn) |
| `PREREQUISITES_MISSING` | Training-, Cert- und/oder Führerschein-Anforderungen nicht erfüllt |
| `ORG_DECISION_DENIED` | Voraussetzungen OK, Entscheidung nötig, kein aktiver Grant, kein Revoke für heute |
| `ORG_DECISION_REVOKED` | Voraussetzungen OK, kein aktiver Grant, `REVOKED`-Entscheidung deckt heute ab |
| `CAN_EXERCISE` | Ausübung erlaubt |

**Priorität:** Voraussetzungen gewinnen immer - auch mit Grant bei fehlenden Voraussetzungen → `PREREQUISITES_MISSING` (Sync auto-revoked den Grant).

### Voraussetzungen

Die Auswertung (Halter laden, ALL/ANY je Art) liegt im geteilten `PrerequisiteEvaluationService`, der auch von den [Lehrgangsangeboten](courseoffering.md#voraussetzungen) genutzt wird.

Alle drei Gruppen müssen passen (`trainingCoursesMet && certDefinitionsMet && drivingLicensesMet`). Leere Gruppe ⇒ diese Seite `true`.

- **Training:** Config Keys der Definition → Type-IDs → granted types des Users aus Submissions; `ALL` / `ANY`.
- **Certs:** Config Keys → Definitionen → `CertificationStatusService`; nur `CertificationStatus.VALID` zählt; gleiche Modi.
- **Führerschein:** Config Keys → Definition-IDs → Klassen am User-Führerschein; Klasse zählt nur wenn `document_expiration_date` und Klassen-`expiration_date` leer oder nicht vor heute; `ALL` / `ANY`. FSK-Kontrolle läuft separat über **Certs** (siehe [Führerschein](driving-license.md)).

## Org-Entscheidungen & Intervalle

Entity `UserQualificationOrgDecision` (`qual_user_org_decisions`) - Scope `(tenant, user, definition, organization)`.

| Feld | Rolle |
|------|-------|
| `status` | `GRANTED` / `REVOKED` |
| `startDate` / `endDate` | Inklusives Intervall; null end = offen |
| `revokeReason` | `PREREQUISITES_LOST`, `REMOVED_FROM_CATALOG` |

`isActiveGrantOn(date)` <=> `GRANTED` und Datum im Intervall.

`QualificationOrgDecisionIntervals`: Overlap-Checks nur unter `GRANTED` (angrenzende Tage **überlappen nicht**); `isRevokedCoveringDate` für Revoke-Intervalle.

### Service-API (`QualificationOrgDecisionService`)

| Methode | Verhalten |
|---------|-----------|
| `grant` / `updateGrant` | `GRANTED`-Intervall anlegen/aktualisieren |
| `autoRevoke(..., reason)` | Nur Decisions mit Status `GRANTED` **und** `isActiveGrantOn(today)` → `REVOKED` (+ `revokeReason`, `revokedAt`) |

Grant-Guards: Definition braucht Org-Entscheidung; im Org-Katalog; nicht `PREREQUISITES_MISSING`; keine überlappenden Grants.

**Hinweis:** Exercise-Status `ORG_DECISION_DENIED` ist kein gespeicherter Status - bedeutet „noch kein Grant“.

### Controller

`UserQualificationOrganizationController` - `/api/v1/auth/organizations/{organizationId}/users/{userId}/qualifications`

| HTTP | Pfad | Aktion |
|------|------|--------|
| GET | `/status` | Status für User |
| POST/PUT | `/{qualificationConfigKey}/org-decision` | Grant / Update |

Außerdem: Org-Overview `OrganizationQualificationController` (`GET …/qualifications/status-overview?search=`: gruppierte Antwort `groups[]` mit `members[]`, Gruppen alphabetisch nach Name; kein `page`/`sort`. `search` ist eine serverseitige Token-Suche, Groß-/Kleinschreibung egal, alle Tokens müssen im selben Text passen (`ListSpecs.searchTokens`, max. 5 Wörter, sonst 400): Trifft der Gruppenname, bleiben alle Mitglieder; sonst bleiben nur passende Mitglieder (`userDisplayName`) und Gruppen ohne Treffer entfallen. `appointableMembers` bleibt unverändert; Frontend filtert nicht clientseitig), Self/Org-User-Status als paginierte Item-Listen (`ListQuery` + optional `status`) unter `GET …/qualifications/status` (`QualificationStatusItemPageDto` bzw. Self mit `organizationId` pro Zeile).

## Sync & Listener

`QualificationStatusSyncListener` (`@AfterCommitEventListener`) → `QualificationStatusSyncService`:

| Trigger | Effekt |
|---------|--------|
| Cert-Zuweisung / Training-Record / Driving-License-Record / Org-Entscheidung geändert | `syncForUser` |
| Definition geändert | `syncForQualification` |
| Katalog-Selektion geändert | `syncForCatalogChange` - aktive Grants (`GRANTED`) für aus dem Org-Katalog entfernte Defs → `autoRevoke(REMOVED_FROM_CATALOG)` |

Pro Org: bei aktivem Grant aber verlorenen Voraussetzungen → `autoRevoke(PREREQUISITES_LOST)`. Sync speichert keinen Exercise-Status und liefert kein Status-Delta; er revoked nur veraltete Org-Decisions.

## Katalog-Auflösung

`QualificationCatalogService`:

1. Org → effektiver Standard
2. `QualificationSelection`-Keys für diesen Standard
3. Aktive, nicht archivierte Definitionen für diese Keys

Leere Selektion → leerer Katalog → keine Status-Items.

## Invarianten & Fallstricke

1. Status ist **berechnet**, nie als Zeile gespeichert - keine Status-Tabelle suchen.
2. `ORG_DECISION_DENIED` ist abgeleiteter Exercise-Status, kein gespeicherter Entscheidungswert.
3. Voraussetzungen schlagen Grants; Sync revoked veraltete Grants.
4. Katalog nur Selektion (wie Cert/Training auf Standards) - keine per-Org-Materialisierung.
5. Qualification vs. Certification vs. TrainingCourse bei API- und UI-Text trennen.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Status-Enum / evaluate | `qualification/domain/QualificationExerciseStatus.java`, `service/QualificationStatusService.java` |
| Org-Entscheidungen | `domain/UserQualificationOrgDecision.java`, `service/QualificationOrgDecisionService.java` |
| Intervalle | `util/QualificationOrgDecisionIntervals.java` |
| Sync | `service/QualificationStatusSyncService.java`, `event/QualificationStatusSyncListener.java` |
| Katalog-Delete-Cleanup | `event/TenantCatalogDefinitionDeletedQualificationCleanupListener.java` |
| Katalog | `service/QualificationCatalogService.java` |
| Definition | `tenant/TenantQualificationDefinition.java`, `service/TenantQualificationDefinitionService.java` |
| Standard-Domain | `standard/QualificationStandardDomain.java` |
| Controller | `controller/UserQualificationOrganizationController.java`, `OrganizationQualificationController.java`, `UserQualificationSelfController.java` |

Siehe auch: [Standard](standard.md) (Katalog-Selektion), [Organisation & Mitgliedschaft](organization.md) (Org-Scope).
