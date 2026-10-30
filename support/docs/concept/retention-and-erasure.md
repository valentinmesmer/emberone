# Aufbewahrung und Löschung (Retention)

Design-Notizen zu automatischen Hard Deletes nach DIN-66398-Begriffen: **eine Löschklasse = eine Frist = ein Handler**. Fristen kommen aus dem Aufbewahrungsprofil des Bundeslands (`FederalStateBaselines.retentionProfile()`); heute gibt es je Bundesland genau ein Profil (Bayern: `BY`). Alle Fristen sind Platzhalter bis zur Freigabe durch den Datenschutzbeauftragten des Betreibers.

**Open-Source-Betrieb:** Das Repository enthält ein lauffähiges Bayern-Profil (`BayernRetentionProfile` / `BY`) mit Platzhalterfristen. Vor produktivem Einsatz müssen Betreiber die Fristen mit dem DSB abstimmen, im Verzeichnis der Verarbeitungstätigkeiten dokumentieren und bei Abweichungen ein eigenes Profil oder angepasste Perioden einführen. Der Scheduler und der Startup-Lauf löschen Daten gemäß dem aktiven Profil - nicht blind in Produktion deployen.

Siehe auch: [Geplante Jobs](scheduled-jobs.md), [User-Löschung: Fremdschlüssel](user-delete-foreign-keys.md), [Federal state](federal-state.md), [Benachrichtigungssystem](notification-system.md), [Admin](admin.md).

## Mental Model

```
RetentionCategory (Enum)
  ├─ Tabellen       → RetentionHandler#tables() (Abdeckungstest)
  ├─ Startzeitpunkt → nur im Handler (Austritt, Event-Datum, Erstellung …)
  └─ Frist          → RetentionProfile des Bundeslands des Tenants
```

- EmberOne ist Open Source und wird vom Tenant selbst betrieben. Verantwortlicher ist die Gemeinde bzw. Feuerwehr (DSGVO, BayDSG).
- Betreibt ein Dritter die Instanz für mehrere Tenants, ist er Auftragsverarbeiter: Scheidet ein Tenant aus, muss er dessen Daten löschen oder zurückgeben (Art. 28 Abs. 3 lit. g) - siehe [Tenant-Löschung](#tenant-löschung).
- Der Tenant lässt das Profil vom DSB freigeben und dokumentiert es im Verzeichnis der Verarbeitungstätigkeiten. Eigene Fristen je Tenant gibt es vorerst nicht.
- **Archivierung** liegt beim Tenant (Art. 13 Abs. 1 BayArchivG). In der App gelten normale Löschfristen; was archivrechtlich gebraucht wird, übernimmt die Gemeinde vor Fristablauf selbst.

**Nicht im Scope:** Art.-17-Einzellanträge, Export/Archiv-Übergabe, Freitext-Erwähnungen Dritter, interne Authentik-Daten.

## Entscheidungen

| Thema | Entscheidung |
|-------|--------------|
| Löschtechnik | Nur **Hard Delete**, auch für den User-Datensatz. Keine Anonymisierung (in kleinen Wehren re-identifizierbar), kein Soft Delete als Ersatz. |
| Modell | DIN-66398-Begriffe: Löschklasse × Startzeitpunkt × Frist. Eine eigene Klasse nur, wenn Start oder Frist abweichen. |
| Statistik | Bleibt nicht in der App. Was länger gebraucht wird, archiviert die Gemeinde extern. Keine Teil-Löschung. |
| Audit | Änderungsprotokoll ohne Wertkopien (nur Spaltennamen bei UPDATE). Sonst würde jedes `DELETE` den Inhalt für die Audit-Frist wiederherstellen. |
| S3 / IdP | Löschen erst nach Commit über Outbox (`obj_pending_deletions`, `deprovision_pending_at`) mit Retry. |
| Backups | Nicht im Repo verändert. Kurze Rotation; nach Restore Backend neu starten → Startup-Retention zieht Frist-Löschungen nach. |

## Modell

- Handler löschen **mengenbasiert** (`DELETE … WHERE <Start> < :cutoff`), ohne Entities zu laden; DB-Cascades erledigen Abhängigkeiten. Native Deletes laufen in Schritten von 5000 Zeilen (`ctid`-Subquery); Handler wiederholen bis kein Batch mehr fällt.
- `RetentionScheduler` läuft nächtlich (Cron `emberone.retention.cron`). Je Tenant und Handler eine eigene Transaktion (`RetentionHandlerInvoker`, `REQUIRES_NEW`); dort `app.retention = true`, damit Hard-Deletes kein Audit erzeugen.
- `retention_runs` protokolliert nur Zähler, Profil-Schlüssel und Frist - **keine Personenbezüge**. Zeilen nur bei `count > 0`.

## Package `com.emberone.retention`

| Baustein | Aufgabe |
|----------|---------|
| `RetentionCategory` | Alle Löschklassen |
| `RetentionProfile` / `BayernRetentionProfile` (`BY`) | Frist je Klasse, eingehängt in `FederalStateBaselines` |
| `RetentionPolicyService` | Einzige Fristenquelle für Laufzeitcode |
| `RetentionHandler` (SPI) | `category()`, `tables()`, `apply(Instant cutoff)` |
| `RetentionRegistry` | Start-Validierung: genau ein Handler je Klasse. Fristen und Invarianten prüft `RetentionProfile.of` beim Laden des Profils. |
| `RetentionExecutionService` / `RetentionScheduler` | Ausführung je Tenant |

**Außerhalb des Moduls (technisches Aufräumen):** `EntityAttachmentDraftCleanupScheduler`, sofortiges Löschen offener Onboarding-Einladungen bei Deaktivierung.

## Querschnitt

1. **Audit-Log:** `action_log` ist nach `changed_at` monatlich partitioniert (2026-01 … 2031-12, `action_log_default`); PK `(id, changed_at)`; Index nur `(tenant_id, changed_at DESC)`. Speichert Tabelle, Datensatz-ID, Aktion, Akteur, Zeitpunkt; bei UPDATE nur `changed_columns` (keine Werte); bei INSERT/DELETE keine Inhalte. Trigger überspringt bei `app.purging` (Tenant-Löschung) und `app.retention` (nächtliche Retention-Läufe). Keine App-Ansicht - Auswertung nur DB-seitig durch Betreiber-Admins.
2. **S3:** Löschungen über `obj_pending_deletions` und Outbox-Job (`ObjectStorageTransactionCoordinator`). Promote = Kopie + Quell-Löschung über Outbox.
3. **Verknüpfte User-Dokumente:** Wer einen verlinkbaren Datensatz löscht, ruft im selben Schritt `UserDocumentService#deleteDocumentsForLinkTargets` auf (manuell und per Retention). Zertifikatsnachweise nutzen `deleteDocumentsByIds` über die Junction `cert_user_record_documents`, weil `link_id` bei `CERTIFICATION_RECORD` die Definitions-ID ist; Dokumente, die ein anderer Nachweis noch referenziert, bleiben.
   - **Geteilte Dokumente:** Der Datensatz besitzt das Dokument - wird er gelöscht, verschwindet es auch bei noch bestehendem Quell-Link (`WORKFLOW` / `ONBOARDING_INVITATION`). Wird nur die Quelle gelöscht, bleibt das Dokument, solange ein Datensatz-Link besteht. Signierte Eintrittsdokumente: FK `usr_entry_document_fulfillments.user_document_id` (und Events mit Dokument-Bezug) schützen das `UserDocument` vor bedingter Löschung; siehe [User-Dokumente](userdocument.md).
   - **Eintrittsdokument-Nachweise:** Tabellen `usr_entry_document_fulfillments` und `usr_entry_document_fulfillment_events` (FORCE RLS) hängen an `users` (`ON DELETE CASCADE`) und laufen mit **`USER_MASTER`** ab - nicht mit `ONBOARDING` (6 Monate nach Austritt nur die `APPROVED`-Einladung). Vorlagen-Dateien im `attachments`-Bucket (`onb_*_template_versions`) bleiben referenzpflichtig, solange ein Nachweis den `sha256_hex` referenziert; sonst Outbox-Löschung beim Template-Austausch/Entfernen.
4. **Authentik:** Deaktivierung setzt nur `account_enabled = false` und `deprovision_pending_at`. `IdentityDeprovisionScheduler` löscht den Account danach und leert `idp_user_id` / `deprovision_pending_at`. Bis dahin weist `DeactivatedUserWebFilter` API-Aufrufe mit gültigem JWT ab (403).
5. **Schema:** Personendaten `ON DELETE CASCADE`, Akteure `ON DELETE SET NULL` („Ehemaliges Mitglied“); Liste in [user-delete-foreign-keys.md](user-delete-foreign-keys.md). FKs auf `tenants` mit `CASCADE` für Tenant-Löschung.
6. **Abwesenheit:** keine strukturierten Gesundheitsdaten - siehe [Abwesenheit & Notfallkontakt](absence-and-emergencycontact.md).

## Lebensende einer Person (`USER_MASTER`)

- Start: `users.deactivated_at`. Handler löscht `users` und `idm_users`; Personendaten fallen per CASCADE weg; Akteur-Spalten werden `NULL`.
- **Invarianten** im Profil (`RetentionProfile.of`): `USER_MASTER` ≥ `DRIVING_LICENSE`; `AUDIT` < `USER_MASTER` - damit sind `action_log.user_id`-Bezüge zur Person vor dem User-Delete weg (sofern der Austritt der letzte Akteurzeitpunkt war).
- Zwischen `EXIT_IMMEDIATE` (0 Tage) und `EXIT_SETTLEMENT` (6 Monate) bleiben Stammdaten inkl. Kontakt (`user_contact_information`). `EXIT_IMMEDIATE` entfernt Notfallkontakte, Profilbild und Kanal-Overrides; Kontakt wird erst bei `EXIT_SETTLEMENT` gelöscht.
- Ohne eigene Klasse laufen mit `USER_MASTER` ab: Mitgliedschaften, Zertifikats-Records ohne `expiry_date`, Lehrgänge, Lehrgangsanmeldungen (`course_registrations`, inkl. Endergebnis), Qualifikationsentscheidungen, **Eintrittsdokument-Nachweise** (`usr_entry_document_fulfillments`, `usr_entry_document_fulfillment_events` in `UserMasterRetentionHandler#tables()`).
- Dateien unter `tenants/{tid}/users/{uid}/` (Buckets `user-documents`, `profile-images`) gehen in die S3-Outbox. Dateien in `workflows` / `attachments` gehören zum jeweiligen Datensatz-Handler.

Payloads ohne FK (`wf_instances.payload`, `todo_items.params`, `ntf_notifications.params`) laufen über eigene Klassen mit kürzeren Fristen.

**Sofort beim Austritt** (außerhalb der Fristen): API-Sperre, IdP-Deprovision-Job, Abbruch eigener/betreffender Workflows, Löschung offener Onboarding-Einladungen.

### Betriebsdaten

| Klasse | Start | Verhalten |
|--------|-------|-----------|
| `EVENT` | `starts_at` | Hard Delete Dienst/Einsatz inkl. Anwesenheiten und Event-Grants |
| `APPOINTMENT` | `ends_at` | Hard Delete inkl. Anhänge (`attachments`-Bucket) |
| `WORKFLOW_STALE` | `opened_at` | Bricht offene Workflows über `WorkflowService.cancelStaleWorkflow` ab (setzt `decided_at`, **löscht nicht**); je Workflow `REQUIRES_NEW` + `FOR UPDATE SKIP LOCKED`. Danach Frist `WORKFLOW` ab `decided_at` |
| `WORKFLOW` | `decided_at` | Hard Delete abgeschlossener/abgebrochener Vorgänge inkl. `workflows`-Bucket |
| `TODO` | `archived_at` | Hard Delete archivierter Todos |
| `ONBOARDING` | `users.deactivated_at` (nur `APPROVED`) | Während Mitgliedschaft für Eintritt-UI gehalten; Hard Delete nach Austritt + Frist; offene Einladungen beim Austritt sofort |

**Todos:** `WORKFLOW_*` archiviert `WorkflowTodoAdapter` mit dem Workflow. `CERTIFICATION_EXPIRING` schließt `CertificationReminderService` bei erfüllter/entfallener Pflicht (oder manuell).

### Persönliche Nachweise

| Klasse | Start | Verhalten |
|--------|-------|-----------|
| `ABSENCE` | `end_date` | Hard Delete; Org-Zuordnungen per CASCADE |
| `TIME_ENTRY` | `start_time` | Hard Delete |
| `DRIVING_LICENSE` | `users.deactivated_at` | Hard Delete inkl. Klassen und verknüpfter Dokumente (S3-Outbox) |

### Zertifikats-Löschklasse `EVIDENCE`

| Klasse | Start | Datensätze | Dokumente |
|--------|-------|------------|-----------|
| `EVIDENCE` | `expiry_date` | Jeder `cert_user_record` mit gesetztem `expiry_date` (unabhängig von Definition/Kürzel) | Junction → `deleteDocumentsByIds`; geteilte Scans bleiben |

- `expiry_date` ergibt sich aus expliziter Angabe oder `completed_date + interval_days` der Definition.
- Records ohne `expiry_date` laufen mit `USER_MASTER` ab (z. B. Definition ohne Intervall).
- Scans nach Freigabe in der persönlichen Akte; Prozess-Kopien im Workflows-Bucket nach Promotion/Ablehnung/Abbruch weg (alle Zertifikats-Workflows gleich).

## Bayern-Profil `BY` (Platzhalter, DSB-Freigabe ausstehend)

**Profil-Schlüssel:** `BayernRetentionProfile.KEY = "BY"` (ohne Jahreszahl). Fristenänderungen im einzigen Profil behalten den Schlüssel; Release Notes nennen die Änderung. Ein parallel wählbares weiteres Profil bekommt einen neuen Schlüssel (z. B. `BY_2`).

Alle 15 Löschklassen sind umgesetzt. Gelöscht wird beim nächsten nächtlichen Lauf (03:30 UTC) bzw. beim Start-Lauf, sobald *Start + Frist* vergangen ist. „Austritt“ = `users.deactivated_at`. Datumsspalten werden gegen den Stichtag in UTC verglichen.

| Klasse | Start | Frist | Was wird gelöscht? |
|--------|-------|-------|--------------------|
| `EXIT_IMMEDIATE` | Austritt | 0 Tage | Notfallkontakte, Profilbild, Kanal-Overrides |
| `EXIT_SETTLEMENT` | Austritt | 6 Monate | Kontakt; unverknüpfte persönliche Dokumente |
| `DRIVING_LICENSE` | Austritt | 3 Jahre | Führerscheindaten inkl. Scans |
| `EVIDENCE` | `expiry_date` | 12 Monate | Zertifikatsnachweise mit Ablaufdatum inkl. Scans |
| `ABSENCE` | `end_date` | 6 Monate | Abwesenheiten |
| `TIME_ENTRY` | `start_time` | 5 Jahre | Zeiteinträge |
| `EVENT` | `starts_at` | 5 Jahre | Dienste/Einsätze inkl. Anwesenheiten |
| `APPOINTMENT` | `ends_at` | 12 Monate | Termine inkl. Anhänge |
| `WORKFLOW_STALE` | `opened_at` | 12 Monate | Nur Abbruch offener Vorgänge |
| `WORKFLOW` | `decided_at` | 24 Monate | Abgeschlossene/abgebrochene Vorgänge |
| `TODO` | `archived_at` | 1 Monat | Archivierte Todos |
| `NOTIFICATION` | `created_at` (ungelesen); gelesen → nächster Retention-Lauf | 1 Monat | Benachrichtigungen (+ Zustellungen) |
| `ONBOARDING` | Austritt (`APPROVED`) | 6 Monate | Genehmigte Eintrittseinladung |
| `AUDIT` | `changed_at` | 12 Monate | `action_log` |
| `USER_MASTER` | Austritt | 5 Jahre | Person vollständig (Cascade + S3-Personen-Präfix) |
| `MANAGEMENT_ACCESS` | `COALESCE(ended_at, valid_until)` | 12 Monate | `org_management_activations` (Hard Delete, Protokoll der Aktivierungen; `user_id` ist dann ohnehin NULL bei gelöschter Person) |

## Erweiterung

**Neue Löschklasse:** `RetentionCategory`-Wert, `@Component`-Handler, Frist in **jedem** `RetentionProfile`, Tabellen in `tables()`. Fehlt eines → Start bricht ab. `RetentionTableCoverageArchitectureTest` verlangt jede Tabelle mit `*_user_id`-BIGINT-Spalte in `V1` in mindestens einem `tables()`.

**Neues Bundesland:** eigenes `*RetentionProfile` + `FederalStateSettings` mit `FederalStateBaselines.retentionProfile()`.

**Mehrere Profile pro Bundesland (später):** Profil-Schlüssel, `tenants.retention_profile`, Anpassung nur in `RetentionPolicyService`. Bestehende Schlüssel bleiben (`BY`); neue bekommen fortlaufende Schlüssel (`BY_2`, …).

**Admin-UI:** `/admin/tenants/{id}/retention` listet Löschklassen mit Fristbeginn und Löschfrist (nur Lesen). Detail `/admin/tenants/{id}/retention/{category}` zeigt die betroffenen Entitäten. Profilauswahl kommt mit mehreren Profilen. Siehe [Admin](admin.md).

## Betrieb

Backup-Rotation, Log-Retention und Restore liegen beim Tenant bzw. Betreiber - nicht im Code-Repo. Die App unterstützt den Betrieb nach einem Restore durch einen Retention-Lauf beim Backend-Start ([Geplante Jobs](scheduled-jobs.md)). Begründung: [Backups](#begründungen).

### Backup-Rotation

- Backups werden nicht verändert oder crypto-geshreddet (EDPB-Orientierung).
- **Vorgabe:** kurze Rotation; Platzhalter **30 Tage** (DSB/Betrieb anpassen, im Verzeichnis der Verarbeitungstätigkeiten dokumentieren).

### Log-Retention

- Anwendungslogs (ECS JSON, siehe [Logging](logging.md)) können `userId` / `tenantId` enthalten - Sammlung und Retention beim Log-Shipper.
- **Platzhalter 30 Tage**; länger nur mit DSB-Freigabe.
- App-seitiges `action_log` (`AUDIT`, 12 Monate) ersetzt die Shipper-Retention nicht.

### Restore und Backend-Neustart

Nach einem Restore **immer** das Backend neu starten. Frist-Löschungen werden beim nächsten Lauf von `RetentionScheduler` nachgezogen (Stichtag an Datumsfeldern). Manuelle Löschungen seit dem Backup-Zeitpunkt können zurückkommen und laufen erst wieder mit ihrer Klasse ab - bewusst akzeptiert, begrenzt durch kurze Backup-Rotation.

## Tenant-Löschung

Globale Admin-Aktion auf gemeinsam betriebenen Instanzen (Art. 28 Abs. 3 lit. g): `POST /api/v1/auth/admin/tenants/{tenantId}/purge` mit Bestätigung des **Mandantennamens** (`confirmationName`, exakter Abgleich). Admin-UI: zweistufig (Name tippen, dann Klick bestätigen).

Ablauf in **einer DB-Transaktion**:

1. `SET LOCAL app.purging = 'true'` (kein `action_log` während Cascades).
2. S3-Präfix `tenants/{tenantId}/` je Bucket in `obj_pending_deletions`.
3. IdP-Accounts aller `idm_users` des Mandanten synchron entfernen (`IdentityUserService#deprovisionAllIdpUsersForTenantPurge`; [Identity Provider](identity-provider-authentik.md)).
4. `DELETE` der Mandantenzeile; tenant-scoped Tabellen inkl. `action_log` per `ON DELETE CASCADE`.

Outbox-Zeilen überleben (`ON DELETE SET NULL` auf `tenant_id`); der Outbox-Job mit GUC `app.object_storage_platform` räumt sie ab. Protokollierung im Application-Log ohne Personenbezug.

**Nicht im Scope:** Export vor Löschung.

## Begründungen

Die Recherche ersetzt keine Rechtsberatung. Ableitungen sind gekennzeichnet.

**Dienstzeit / Ehrenzeichen:** Bayerisches Feuerwehr-Ehrenzeichen (25/40 Jahre) zählt Gesamtdauer ggf. mit Unterbrechungen; Verleihung auch nach Ausscheiden möglich. Default **5 Jahre** `USER_MASTER` hält Mitgliedschaften für Wiedereintritt / Nachweis. Alternative: Bescheinigung beim Austritt, dann kürzere Frist denkbar.

**Gesundheitsdaten:** Eignungsbescheinigungen (z. B. Atemschutz) und vergleichbare Ergebnisse sind nach weiter Auslegung Art.-9-Daten. Konsequenz: kurze Frist nach Ablauf über `EVIDENCE` (12 Monate ab `expiry_date`, wie für alle ablaufenden Zertifikatsnachweise); Abwesenheit ohne strukturierten Gesundheitsbezug; Scans nur Gültigkeit + Frist in der persönlichen Akte, keine dauerhaften Prozess-Kopien.

**Hard Delete statt Anonymisieren:** In kleinen Wehren sind Platzhalter mit Anwesenheiten re-identifizierbar (ErwGr. 26, EDPB). Deshalb `SET NULL` für Akteure, Statistik extern beim Tenant.

**Personalnummer:** Kennnummer i. S. v. Art. 4 Nr. 1 DSGVO, wird mit dem User gelöscht (kein Pseudonym).

**Audit:** DSK-Orientierungshilfe „Protokollierung“ - Pflicht: wer/wann/was; Werte *sollten*, entfallen bewusst, damit Inhalte nicht länger leben als ihre Löschklasse. Frist höchstens ein Jahr → `AUDIT` 12 Monate. Nachweis „wer hatte welche Rechte“ über Mitgliedschaften bis `USER_MASTER`, nicht über längeres Audit.

**Backups:** EDPB erkennt an, dass Backups oft nicht verändert werden sollen; nach Restore müssen Löschungen wiederhergestellt und nachweisbar sein. Deshalb kurze Rotation und zeitnaher nächtlicher Retention-Lauf. Für spätere Art.-17-Sofortlöschung reicht das nicht (eigene Löschliste außerhalb des Backups nötig).

**Führerschein:** Führerscheindaten (Klassen, Scan) **3 Jahre** nach Austritt (`DRIVING_LICENSE`). FSK-Nachweise mit `expiry_date` folgen `EVIDENCE` (12 Monate nach Ablauf), unabhängig vom Katalog-Kürzel.

**Archiv:** Gemeinden archivieren in eigener Zuständigkeit (BayArchivG). EmberOne setzt Dienstbetriebsfristen; Einsatzberichte/Personalakten liegen typischerweise außerhalb der App.

## Offene Punkte / später

- Freigabe der Fristen durch den DSB (`BayernRetentionProfile`); Prüfung gesetzlicher Mindestfristen (KUVB Anwesenheit, Lehrgangsnachweise).
- **Art.-17-Aktion** (nicht geplant): Löschlogik von `USER_MASTER` wiederverwenden + personenbezogene Handler-Variante + `action_log.user_id` auf NULL + Restore-feste Löschliste.
- Mehrere Profile, Reaktivierung bei Wiedereintritt, PDF-Dienstzeitbescheinigung: eigene Features, nicht Teil des Retention-Moduls.

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Tenant-Purge | `admin/tenant/service/AdminTenantPurgeService.java`, `AdminTenantController#purgeTenant` |
| Admin-Retention-Ansicht | `admin/retention/`, Frontend `app/admin/tenants/[id]/retention/` |
| S3-Buckets | `platform/storage/service/ObjectStorageBucketCatalog.java` |
| Handler | `retention/handler/*RetentionHandler.java` |
| Zertifikats-Retention | `EvidenceRetentionHandler`, `CertificationRecordRetentionSupport` |
| Bayern-Fristen | `federalstate/bayern/BayernRetentionProfile.java` |
| Abdeckungstest | `retention/RetentionTableCoverageArchitectureTest.java` |
| Dokument-Links | `userdocument/DocumentLinkDeletionCoverage.java` |
