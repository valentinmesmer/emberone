# Lehrgangsangebot

Design-Notizen für Lehrgangsangebote einer Org. Package: `com.emberone.courseoffering`. Umgesetzt in Backend und Frontend: Org-Angebote, Mein Bereich, Teilnehmer/Warteliste, Absage und Abschluss. Anwesenheit und Prüfungserfassung sind bewusst zurückgestellt (erweiterbar später).

## Mental Model

Ein **Angebot** gehört einer Anbieter-Org und vergibt einen Lehrgang aus dem Lehrgangs-Katalog ([Training & Zertifizierung](trainingcourse-and-certification.md)). Es hat mehrere **Termine** (Theorie, Praxis, Prüfung). Der Gesamtzeitraum (`firstSessionStart`, `lastSessionEnd`) wird aus den Terminen abgeleitet und als Cache am Angebot gehalten, damit Listen danach sortieren und filtern können. Einzige Schreibstelle: `CourseOffering.syncSchedule()` im Service-`save`.

## Was es besitzt

- `CourseOffering` (`course_offerings`), `CourseSession` (`course_sessions`), Zielgruppe mit optionalem Kontingent (`course_offering_audiences`), Voraussetzungen (`course_offering_prerequisites`), Anmeldungen (`course_registrations`) und die anbietbaren Lehrgangstypen der Org (`org_course_offering_types`), alle mit FORCE RLS.
- Status: `TEMPLATE` (Vorlage, siehe unten), `DRAFT` → `PUBLISHED` → `COMPLETED` (Abschluss, siehe [Abschluss](#abschluss-kiss)) oder `CANCELLED` (Absage, siehe [Absage des Angebots](#absage-des-angebots)). Bearbeiten ist in `TEMPLATE`, `DRAFT` und `PUBLISHED` erlaubt, Löschen nur in `DRAFT` und `TEMPLATE`.
- Veröffentlichen verlangt: Anmeldezeitraum, mindestens einen Termin, Anmeldeschluss vor dem ersten Termin, bei `hasExam` einen Prüfungstermin. Diese Regeln gelten nach jeder Änderung an einem veröffentlichten Angebot weiter.
- Der Lehrgangstyp ist nach dem Anlegen unveränderlich. Ein Typ mit Angeboten ist nicht löschbar, nur archivierbar.

## Anbietbare Lehrgänge (Organisation)

Der Standard schaltet nur das Modul `COURSE_OFFERINGS` an oder aus (wie bei [Management](management.md)). Welche Lehrgangstypen eine Org anbieten darf, wählt der Admin am **Org-Fenster** (`/admin/tenants/[id]/organizations/[orgId]`, Karte „Anbietbare Lehrgänge“, nur sichtbar bei aktivem Modul) und speichert es in `org_course_offering_types` (`OrganizationCourseOfferingTypeService`, `tenant_id`, `organization_id`, `definition_config_key`, FORCE RLS, `ON DELETE CASCADE` an der Org). Opt-in: eine Org bietet nichts an, bis Typen gewählt sind. Anbietbar ist ein Typ, wenn er für die Org selektiert **und** im Lehrgangs-Katalog der Org verfügbar ist (`CourseOfferingCatalogService`).

Admin-API (`canAccessTenant`): `GET` und `PUT /api/v1/auth/admin/tenants/{tenantId}/organizations/{organizationId}/course-offering-types` (`{ enabled, definitionConfigKeys }`; `PUT` ersetzt die Liste, abgelehnt mit 409 bei ausgeschaltetem Modul sowie unbekanntem oder archiviertem Typ). Bei ausgeschaltetem Modul liefert `GET` `enabled=false` und keine Schlüssel. Umbenennen, Löschen und Archivieren eines Typs ziehen die Selektionen mit.

## Zielgruppe (Audience)

**Sichtbarkeit:** Keine Einträge = **öffentlich** (gesamter Mandant). Genau ein Eintrag `OWN_ORG` = **privat** (nur anbietende Org). Öffentlich lässt sich optional mit `ORG`, `ORG_TYPE` und `ORG_SUBTREE` (frei kombinierbar, je mit `refId`) einschränken; Duplikate sind verboten. Ersetzt wird die gesamte Liste (`PUT …/audiences`).

Berechtigt ist, wer in mindestens einer Org eine **aktive** Mitgliedschaft hat, die einen Eintrag trifft (jede Mitgliedschaft, nicht nur die Hauptorg). Ohne aktive Mitgliedschaft ist ein User für nichts berechtigt, auch nicht bei öffentlichen Angeboten. Aufgelöst wird beim Lesen gegen den aktuellen Org-Baum: `CourseAudienceScopeService` liefert Mitglieds-Orgs, diese samt allen Vorfahren (ein Teilbaum-Eintrag trifft über einen Vorfahren) und die Org-Typen; `CourseOfferingListSpecs.eligibleFor` setzt das als `EXISTS`-Subquery um. Verschobene Orgs wirken sich nur auf neue Zugriffe aus.

## Voraussetzungen

Pro Angebot beliebig viele Voraussetzungen (`course_offering_prerequisites`) aus Lehrgängen, Nachweisen und Führerscheinen, je per `config_key` aus dem Mandanten-Katalog, mit `ALL`/`ANY` je Art (Spalten am Angebot). Ersetzt wird die ganze Liste inklusive Modi (`PUT …/prerequisites`); unbekannte Keys und Duplikate werden abgelehnt.

Die Auswertung liegt bewusst nicht im Angebot, sondern im geteilten `PrerequisiteEvaluationService` (Package `qualification`), den auch `QualificationStatusService` nutzt: `loadHoldings(userId)` lädt einmal, was ein User besitzt (Lehrgangstypen, Führerscheinklassen, gültige Nachweise), danach prüfen `trainingCoursesMet` / `certDefinitionsMet` / `drivingLicensesMet` je Art. Leere Art = erfüllt, unbekannte Keys = nicht erfüllt. `CoursePrerequisiteService.check` fasst das für ein Angebot zusammen; `GET /api/v1/self/course-offerings/{id}/prerequisites` zeigt dem User, was fehlt (nur für Angebote, die er sehen darf). Die verbindliche Prüfung geschieht beim Öffnen des Anmelde-Workflows (siehe [Anmeldung](#anmeldung)).

## Vorlagen

Eine Vorlage ist ein `CourseOffering` mit Status `TEMPLATE` und gesetztem `templateName` (DB-Check: genau dann gesetzt). Gleiche Tabellen, gleiche Bearbeitung wie bei Angeboten; sie nimmt nie Anmeldungen an, ist nicht veröffentlichbar, erscheint nicht in Self-Listen und nie in der Angebotsliste (`status=TEMPLATE` wird dort abgelehnt). Vorlagen haben eine eigene paginierte Liste `GET …/templates` (Suche Vorlagenname/Lehrgangstyp, Sortierung nach Name).

`CourseOfferingTemplateService` kopiert zwischen Angebot und Vorlage; die Kopie hat keine Verbindung zur Quelle:

- Vorlagen entstehen nur aus Angeboten: `POST …/{id}/save-as-template` kopiert ein bestehendes Angebot (unverschoben). Es gibt keinen Weg, eine Vorlage von Grund auf anzulegen.
- `POST …/{id}/instantiate` mit `firstSessionDate` erzeugt einen `DRAFT` mit Feldern, Zielgruppe, Voraussetzungen und Terminen. Alle Termine und der Anmeldezeitraum werden um dieselbe Zahl **Kalendertage** (Org-Zeitzone) verschoben; lokale Uhrzeiten und Abstände bleiben, auch über Sommerzeit-Wechsel. Eine Vorlage ohne Termine wird unverschoben kopiert. Der Lehrgangstyp muss beim Verwenden noch anbietbar sein.

## Anmeldung

Ein User meldet sich per Workflow `COURSE_OFFERING_REGISTRATION` (Typ 17) an: `POST /api/v1/auth/workflows/course-offering-registration` mit `offeringId`. Der Handler (`CourseOfferingRegistrationWorkflowHandler`) ruft beim Öffnen `CourseRegistrationService.requireRegistrable` auf; abgelehnt wird (400, unberechtigt 404), wenn

- das Angebot nicht `PUBLISHED` ist oder der Anmeldezeitraum nicht läuft (Start inklusive, Ende exklusive),
- der User nicht berechtigt ist (aktive Mitgliedschaft in einer passenden Org, siehe Zielgruppe; bei einer Vorlage nie),
- der User für dieses Angebot schon eine aktive Anmeldung hat (`PENDING`, `PARTICIPANT`, `WAITLIST`; zusätzlich DB-Unique-Index und Open-Concurrency-Key `course-offering:{id}`),
- die Voraussetzungen nicht erfüllt sind (geteilte Auswertung aus `PrerequisiteEvaluationService`),
- die Option "Freigabe der Hauptorg" gesetzt ist, der User aber keine Hauptorg hat.

**Entscheider.** Kontext-Org ist immer die Anbieter-Org. Ist `requiresHomeOrgApproval` gesetzt, kommt die Hauptorg des Users als dynamische Genehmiger-Org dazu (`WorkflowMode.DYNAMIC`). Approver-Slots sind pro Org eindeutig; sind beide Orgs dieselbe, entsteht genau ein Slot und damit eine Entscheidung. Die Hauptorg liefert nur Freigabe, keine Platzvergabe. Der Payload speichert einen Schnappschuss (`trainingCourseTypeId` und Anzeigename, Anbieter-Org, Hauptorg, `alreadyHeld`), damit alle Entscheider den Kontext ohne weitere Abfragen sehen. **`alreadyHeld`** ist der Hinweis "Lehrgang bereits besucht": der Lehrgangstyp steckt schon in einem Nachweis des Users. Anmelden bleibt möglich; der Workflow zeigt den Hinweis allen Entscheidern. Eine Anmeldung kann nicht bearbeitet und kein Input-Change angefordert werden; der Opener bricht ab und meldet sich neu an. Die Bayern-Baseline legt Workflow-Definition und Type-Mapping (Genehmiger-Gruppe: Personal) für den Typ an; eine Org ohne Mapping kann keine Anmeldungen annehmen.

**Registrierung.** `CourseRegistration` (`course_registrations`) entsteht beim Öffnen mit Status `PENDING` und folgt dem Workflow:

| Ereignis | Ergebnis |
|----------|----------|
| Alle Slots genehmigt | `PARTICIPANT` bei freiem Platz, sonst `WAITLIST`; `approvedAt` = Genehmigungszeitpunkt |
| Ablehnung | `REJECTED` (beendet) |
| Abbruch durch den User | `CANCELLED`, `endedBy = USER` |
| Absage-Workflow genehmigt (siehe [Absage](#absage)) | `CANCELLED`, `endedBy = USER` |
| Rückzug von der Warteliste (siehe [Absage](#absage)) | `CANCELLED`, `endedBy = USER` |
| Org entfernt Teilnehmer oder Wartelistenplatz | `CANCELLED`, `endedBy = ORGANIZATION`, Pflicht-Begründung in `endReason`, `endedByUserId` |

Anmeldung ist auch bei vollem Angebot möglich (`SelfCourseOfferingDto.full` für den Hinweis vor dem Absenden: Warteliste, kein Platz garantiert). Eine beendete Anmeldung (`REJECTED`, `CANCELLED`) ist final; ein neuer Anlauf erzeugt eine neue Anmeldung. Wird der Workflow ohne Handler-Callback beendet (User gelöscht oder deaktiviert), bleibt die Zeile bis zur Löschung der Person bestehen.

**Kapazität ohne Überbuchung.** Jede Änderung der Platzbelegung (Genehmigung, Nachrücken, Entfernen) nimmt zuerst einen pessimistischen Zeilen-Lock auf dem Angebot (`CourseOfferingRepository.findForUpdate`, `SELECT … FOR UPDATE`) und zählt erst danach die Teilnehmer gegen `maxParticipants`. Gleichzeitige Genehmigungen und Nachrück-Aktionen desselben Angebots laufen dadurch nacheinander; überbuchen ist nicht möglich. Gezählt wird gegen das Gesamtmaximum; Kontingente ergänzen das um die Prüfung des Eintrags (siehe [Kontingente](#kontingente)). Ein freier Platz gilt auch dann als frei, wenn die Warteliste nicht leer ist (Nachrücken bleibt manuell, die Genehmigung vergibt nur freie Plätze).

**Warteliste.** Reihenfolge nach `approvedAt` (dann `id`); `waitlistPosition` im DTO ist die 1-basierte Stelle. Nachrücken geschieht nie automatisch: die Org wählt eine Anmeldung und `promote` setzt sie auf `PARTICIPANT`, solange ein Platz frei ist (sonst 409). Die Org darf frei wählen, die Reihenfolge ist keine Pflicht. Bei der Genehmigung gilt weiterhin: ist das Angebot nicht mehr `PUBLISHED`, schlägt sie fehl (die Entscheider lehnen dann ab).

**Entfernen.** Teilnehmer und Wartelistenplätze entfernt die Org jederzeit, solange das Angebot `PUBLISHED` ist, mit Pflicht-Begründung und ohne Workflow. Der User erhält die Benachrichtigung `COURSE_REGISTRATION_REMOVED` (nach Commit, mit Thema und Begründung), der Platz wird frei. Offene Anmeldungen (`PENDING`) lehnt die Org über die normale Workflow-Entscheidung ab.

**Mitgliederansicht.** `GET /api/v1/auth/organizations/{orgId}/users/{userId}/course-registrations` (paginiert, `search` über den Lehrgangsnamen, Sort `startDate` (Standard absteigend) / `registeredAt` / `courseType`, Filter `status` (mehrfach) und `upcoming=true` für nur Angebote, deren letzte Sitzung noch nicht vorbei ist; die UI setzt `upcoming` standardmäßig) zeigt Angebot, Lehrgang, anbietende Org, Zeitraum und Status. Berechtigung ist `TRAINING_COURSE_READ` der Org (Basisbereich), **nicht** `COURSE_OFFERING_READ`: die Anmeldungen sind Mitgliederdaten und können Angeboten anderer Orgs gehören, deshalb darf das Modul Lehrgangsangebote der eigenen Org keinen Einfluss haben. Der User muss aktives Mitglied der Org sein; ausgeliefert werden nur Status-Informationen.

**Lehrgangsanmeldungen der Org.** `GET /api/v1/auth/organizations/{orgId}/course-registrations` (paginiert, Suche nach Name des Mitglieds, Sort `startDate` (Standard aufsteigend) / `name`, Filter `status` (mehrfach) und `upcoming=true` für nur anstehende Angebote; die UI setzt `upcoming` und die Status `PENDING`, `PARTICIPANT`, `WAITLIST` standardmäßig) listet die Anmeldungen zu den Angeboten der Org, mit Mitglied, Lehrgang, Zeitraum und Status. Berechtigung ist ebenfalls `TRAINING_COURSE_READ`. Der Bereich liegt im Menü unter Mitglieder (`/organizations/[id]/course-registrations`); Zeile → Angebot.

**Aufbewahrung.** `course_registrations` hängt per `ON DELETE CASCADE` an der Person und läuft mit `USER_MASTER` ab; der Bezug zum Anmelde- und zum Absage-Workflow (`workflow_id`, `cancellation_workflow_id`) ist `ON DELETE SET NULL`, damit die Workflow-Retention die Anmeldung nicht berührt.

**Anmeldung als Nachweis der Gutschrift.** Der Link hat keinen Fremdschlüssel (polymorph). Deshalb gilt als Invariante: `course_registrations` und `training_course_user_submissions` liegen beide in `USER_MASTER` (Test `CourseCreditRetentionArchitectureTest`), die Anmeldung überlebt die Workflow-Retention (`SET NULL`) und wird nie einzeln gelöscht. Angebote mit Anmeldungen (`COMPLETED`, `CANCELLED`) löscht keine Retention, denn das Löschen des Angebots würde die Anmeldungen per Cascade entfernen. Ein künftig erzeugtes Dokument (z. B. Teilnahmebescheinigung) entsteht direkt nach `createSubmissionFromCourseRegistration` über `UserDocumentSystemWriter` und trägt nur den Link `TRAINING_COURSE_RECORD` auf den Nachweis; `COURSE_REGISTRATION` bleibt Herkunftslink ohne Dokumente (`NON_DOCUMENT_LINK_TYPES`), die Löschung läuft über die bestehende Abdeckung des Lehrgangs.

## Absage

**Absage-Workflow.** Ein Teilnehmer sagt per Workflow `COURSE_REGISTRATION_CANCELLATION` (Typ 18) ab: `POST /api/v1/auth/workflows/course-registration-cancellation` mit `registrationId` (Subject `COURSE_REGISTRATION`). Der Handler (`CourseRegistrationCancellationWorkflowHandler`) ruft beim Öffnen `CourseRegistrationService.requireCancellable` auf; abgelehnt wird (400, fremde oder unbekannte Anmeldung 404), wenn

- die Anmeldung nicht dem User gehört,
- sie nicht `PARTICIPANT` ist (eine offene Anmeldung `PENDING` bricht der User über ihren Workflow ab, ein Wartelistenplatz wird direkt zurückgezogen, eine beendete Anmeldung ist final),
- schon eine Absage offen ist (`cancellationWorkflowId` gesetzt; zusätzlich Open-Concurrency-Key `course-registration:{id}`),
- der erste Termin begonnen hat (`now` nicht vor `firstSessionStart`) oder das Angebot keine Termine hat,
- das Angebot nicht mehr `PUBLISHED` ist,
- der User keine Hauptorg hat.

**Entscheider.** Kontext-Org ist die Anbieter-Org, die Hauptorg des Users kommt **immer** als dynamische Genehmiger-Org dazu (`WorkflowMode.DYNAMIC`). Sind beide dieselbe Org, entsteht genau ein Slot und damit eine Entscheidung. Die Bayern-Baseline legt Definition und Type-Mapping (Genehmiger-Gruppe: Personal) an. Der Payload speichert einen Schnappschuss (Thema, Lehrgang, erster Termin, beide Orgs), Änderungen am Antrag gibt es nicht.

**Wirkung.** `onOpened` verknüpft den Workflow unter Zeilen-Lock auf dem Angebot mit der Anmeldung (`cancellationWorkflowId`) und prüft dabei erneut, dass sie noch `PARTICIPANT` ohne offene Absage ist. Die Anmeldung wird dafür nach dem Lock neu aus der DB gelesen (`EntityManager.refresh`), weil `validateOpen` sie in derselben Transaktion schon geladen hat; sonst sähe die Prüfung den alten Stand und schriebe ihn zurück. Dasselbe gilt für Genehmigung und Absage-Genehmigung. Eine parallele Entfernung durch die Org oder eine zweite Absage scheitert damit. Der Platz bleibt bis zur Genehmigung belegt.

| Ereignis | Ergebnis |
|----------|----------|
| Alle Slots genehmigt | `CANCELLED`, `endedBy = USER`, `endedAt`; der Platz wird frei (nächste Genehmigung oder manuelles Nachrücken, kein automatisches Nachrücken). `cancellationWorkflowId` bleibt als Verlauf stehen |
| Ablehnung oder Abbruch | Anmeldung unverändert (`PARTICIPANT`), `cancellationWorkflowId` wird gelöscht; eine neue Absage ist möglich, solange der erste Termin nicht begonnen hat |

Genehmigt wird auch nach Beginn des ersten Termins, wenn die Absage rechtzeitig gestellt wurde; Voraussetzung ist nur, dass das Angebot noch `PUBLISHED` ist (sonst schlägt die Genehmigung fehl und die Entscheider lehnen ab). Hat die Org den Teilnehmer inzwischen entfernt, bleibt diese Entscheidung bestehen und die Genehmigung ändert nichts.

**Gegenseitiger Ausschluss.** Anmeldung und Absage derselben Person schließen sich aus: Absage nur für `PARTICIPANT` (nie bei offener Anmeldung oder ohne Teilnehmerstatus), keine zweite Absage parallel, und solange die Absage offen ist, bleibt die Anmeldung aktiv, sodass der Unique-Index auf aktiven Anmeldungen und `requireRegistrable` eine neue Anmeldung für dasselbe Angebot verhindern.

**Rückzug von der Warteliste.** Ohne Workflow: `POST /api/v1/self/course-registrations/{registrationId}/withdraw` (204) für die **eigene** `WAITLIST`-Anmeldung; setzt `CANCELLED`, `endedBy = USER`. Fremde Anmeldungen sind 404, andere Status 400. Wie Nachrücken und Entfernen nimmt der Rückzug den Zeilen-Lock auf dem Angebot, damit er nicht mit einem gleichzeitigen Nachrücken kollidiert.

## Kontingente

Optional lässt sich `maxParticipants` auf die Zielgruppen-Einträge verteilen (`course_offering_audiences.quota`, z. B. 20 Plätze: Teilbaum X 7, Teilbaum Y 13). Regeln, geprüft in `CourseOfferingService.requireValidQuotas` bei jedem Ändern des Angebots (`PUT …/{id}`), jedem Ersetzen der Zielgruppe und beim Veröffentlichen (auch bei Entwurf und Vorlage, und nach jeder Änderung an einem veröffentlichten Angebot):

- **Alle oder keiner:** entweder haben alle Einträge ein Kontingent (≥ 1) oder keiner; die **Summe darf** `maxParticipants` **nicht überschreiten**. Darunter ist erlaubt: nicht zugeordnete Plätze bleiben ohne Kontingent (UI fragt vorher per Bestätigung nach).
- **`OWN_ORG` steht allein und hat kein Kontingent** (DB-Check und Service lehnen es ab): ein Kontingent auf dem einzigen Eintrag wäre nur eine Wiederholung des Maximums. Ein Angebot mit genau einem `ORG`/`ORG_TYPE`/`ORG_SUBTREE`-Eintrag darf ein Kontingent haben (dann gleich dem Maximum).
- **Maximum ändern:** Das Maximum und die Kontingente müssen zusammen stimmig sein. Damit beides in einem Schritt änderbar ist, nimmt `PUT …/{id}` optional `audienceQuotas` (`audienceId`, `quota`): die Liste setzt die Kontingente aller bestehenden Einträge (nicht genannte verlieren es, leere Liste entfernt alle), `null` lässt sie unverändert. Beim Anlegen ist `audienceQuotas` nicht erlaubt (es gibt noch keine Einträge).
- **Zielgruppe ersetzen** gleicht Einträge über `kind` + `refId` ab: ein bestehender Eintrag behält seine Id (und damit die ihm zugeordneten Plätze), nimmt nur das neue Kontingent an.
- **Bei vorhandenen Plätzen** (`PARTICIPANT`/`WAITLIST`, nur veröffentlicht): Das Maximum lässt sich (auch ohne Kontingente) nicht unter die Zahl der Teilnehmer senken. Kontingente lassen sich nicht an- oder abschalten, ein Eintrag mit Teilnehmern oder Wartelistenplätzen nicht entfernen, und ein Kontingent nicht unter die Zahl seiner Teilnehmer senken. Umverteilen ist darüber hinaus erlaubt.
- `PUT …/{id}` und `PUT …/audiences` nehmen dafür den Zeilen-Lock auf dem Angebot (`findForUpdate`), denselben wie die Platzvergabe; eine Änderung kann also nicht mit einer Genehmigung interleaven.

**Platzvergabe** (`CourseRegistrationService.place`, unter dem Angebots-Lock): Ohne Kontingente zählt nur das Gesamtmaximum. Mit Kontingenten ermittelt `CourseAudienceMatcher` (neben `CourseAudienceScope`; dieselben Treffer-Regeln wie `CourseOfferingListSpecs.eligibleFor`, nur im Speicher) die Einträge, die der User gegen seine **aktuellen** Mitgliedschaften und den aktuellen Org-Baum trifft, sortiert nach Spezifität: einzelne Org (`ORG`) vor Teilbaum (`ORG_SUBTREE`, tieferer Teilbaum vor höherem; die Tiefe liefert `CourseAudienceScope.depthByOrganizationId`) vor Org-Typ (`ORG_TYPE`); Gleichstand entscheidet die Eintrags-Id. Der User bekommt den ersten Eintrag mit freiem Platz (und nur, solange auch das Gesamtmaximum nicht erreicht ist) als `PARTICIPANT`. Ist keiner frei, kommt er auf die Warteliste des spezifischsten Eintrags. Die Zuordnung steht an der Anmeldung (`course_registrations.audience_id`, nur für Teilnehmer und Wartelistenplätze gesetzt, nach Ende der Anmeldung als Verlauf erhalten; FK `ON DELETE SET NULL`). Trifft der User bei der Genehmigung keinen Eintrag mehr (z. B. Mitgliedschaft beendet), schlägt die Genehmigung fehl (die Entscheider lehnen dann ab).

**Warteliste und Nachrücken.** Die Warteliste ist pro Eintrag: `waitlistPosition` zählt innerhalb des Eintrags (ohne Kontingente: im ganzen Angebot). `promote` setzt eine Anmeldung nur auf `PARTICIPANT`, wenn das Gesamtmaximum **und** das Kontingent **ihres** Eintrags frei sind (sonst 409); freie Plätze in anderen Einträgen zählen bewusst nicht, damit das Nachrücken ein Kontingent nicht heimlich umgeht. Entfernen und Absage geben den Platz im jeweiligen Kontingent frei (gezählt werden nur `PARTICIPANT`).

**Sichten.** `GET …/registrations/summary` liefert zusätzlich `quotas` (je Eintrag `audienceId`, `kind`, `refId`, `quota`, `participants`, `waitlist`, `freeSeats`; leer ohne Kontingente), die Registrierungs-DTO der Org trägt `audienceId`. `SelfCourseOfferingDto.full` ist mit Kontingenten nutzerbezogen: voll ist ein Angebot für den User, wenn das Gesamtmaximum erreicht ist oder alle von ihm getroffenen Einträge ausgeschöpft sind (Anmeldung landet dann auf der Warteliste).

**Vorlagen.** Kopieren (`save-as-template`, `instantiate`) übernimmt die Kontingente mit; Maximum und Summe bleiben unverändert und damit stimmig.

## Abschluss (KISS)

`CourseCompletionService` schließt ein `PUBLISHED`-Angebot ab. Die Org legt pro Teilnehmer (`PARTICIPANT`) manuell fest, ob er bestanden hat (`course_registrations.final_passed`). Es gibt **keinen** System-Vorschlag aus Anwesenheit oder Prüfung; Anwesenheit und Prüfungserfassung sind für eine spätere Erweiterung vorgesehen. `hasExam` am Angebot bleibt als Stammdatum erhalten.

**Endergebnis.** `PUT …/registrations/{registrationId}/final-result` (Body `passed`; `null` nimmt eine Entscheidung zurück) speichert `final_passed` samt `final_decided_by_user_id` und `final_decided_at`. Der Verlauf liegt im `action_log`. Setzen ist erst am oder nach dem **ersten Termintag** (Org-Zeitzone) erlaubt; vorher 400.

**Bestätigen.** `POST …/complete` nimmt den Zeilen-Lock auf dem Angebot (`findForUpdate`), prüft `PUBLISHED` und dass das heutige Kalenderdatum in der Org-Zeitzone **nicht vor dem ersten Termintag** liegt (dieselbe Grenze wie beim Endergebnis). Nur Teilnehmer mit `final_passed = true` bekommen den Lehrgang gutgeschrieben; offene Endergebnisse (`null`) zählen als nicht bestanden. Danach werden offene Anmeldungen (`PENDING`) und Wartelistenplätze wie bei der Absage des Angebots beendet, ihre Workflows über `WorkflowService.cancelActiveWorkflows` geschlossen (ohne Handler-Callback und **ohne** `WORKFLOW_CANCELLED`-Benachrichtigung) und das Angebot wird `COMPLETED` (`completed_at`). Eine zweite Bestätigung scheitert am Status (400).

**Gutschrift.** `TrainingCourseRecordService.createSubmissionFromCourseRegistration` legt einen Lehrgangsnachweis an (Name = Lehrgangstyp, Zeitraum = erster bis letzter Termin in Org-Zeitzone, Ort = erster Termin mit Ort, Hinweis = optionaler Lehrgangsinhalt (`content`), **Stunden** = Summe der Terminminuten geteilt durch 60, zwei Nachkommastellen), mit App-Link `COURSE_REGISTRATION` (`linkId` = Id der Anmeldung, die den Lehrgang erbracht hat; das Angebot erreicht man darüber). Archivierte Typen blockieren nicht. Es gibt keine eigene Benachrichtigung „Lehrgang gutgeschrieben“; wie bei anderen Nachweisen löst `TrainingCourseRecordChangedEvent` nur eine Datenaktualisierung aus (kein neuer Notification-Typ).

**API.** `/api/v1/auth/organizations/{orgId}/course-offerings/{id}`: `GET /completion`, `PUT /registrations/{registrationId}/final-result`, `POST /complete` (`COURSE_OFFERING_READ` / `WRITE`).

## Absage des Angebots

`CourseOfferingCancellationService.cancel` (`POST …/course-offerings/{id}/cancel`, Body `reason` Pflicht, `COURSE_OFFERING_WRITE`) sagt ein `PUBLISHED`-Angebot ab. Entwürfe werden gelöscht, Vorlagen sind nie anmeldbar, `COMPLETED` ist endgültig und `CANCELLED` lässt sich nicht erneut absagen; alles andere als `PUBLISHED` ergibt 400. Die Antwort ist das Angebot (mit `cancelledAt` und `cancelReason`).

Alles geschieht in einer Transaktion unter dem Zeilen-Lock auf dem Angebot (`findForUpdate`), demselben Lock wie bei Genehmigung, Nachrücken und Entfernen:

- Alle Anmeldungen in `PENDING`, `PARTICIPANT` und `WAITLIST` enden als `CANCELLED` mit `endedBy = ORGANIZATION`, `endedByUserId` (die absagende Person) und der Begründung als `endReason`. Auch offene Anmeldungen enden so (nicht als `REJECTED`), weil nicht ein Entscheider ablehnt, sondern das Angebot wegfällt.
- Die offenen Anmelde-Workflows (`PENDING`) und offenen Absage-Workflows (`cancellationWorkflowId`) werden über `WorkflowService.cancelActiveWorkflows` geschlossen: Status `CANCELLED`, Exklusivitäts-Locks frei, bewusst **ohne** Handler-Callback und **ohne** Lifecycle-Event (kein `WORKFLOW_CANCELLED` neben `COURSE_OFFERING_CANCELLED`). Bereits beendete Workflows bleiben unberührt.
- Das Angebot wird `CANCELLED` (`cancelled_at` und `cancel_reason`, DB-Check: genau dann gesetzt). Eine parallele Genehmigung scheitert danach am Angebotsstatus oder an der nicht mehr offenen Anmeldung.
- Ein `CourseOfferingCancelledEvent` mit allen betroffenen Usern (eine Anfrage, nicht pro User) löst nach Commit die Benachrichtigung `COURSE_OFFERING_CANCELLED` (Thema, Begründung) aus.

## Benachrichtigungen

Die Lehrgangs-Benachrichtigungstypen (`COURSE_REGISTRATION_REMOVED`, `COURSE_OFFERING_CANCELLED`, `COURSE_OFFERING_SESSIONS_CHANGED`, `COURSE_OFFERING_START_REMINDER`) sind `IN_APP + EMAIL`, haben keinen App-Link und stehen in [Benachrichtigungssystem](notification-system.md).

**Erinnerung vor Lehrgangsbeginn** (`COURSE_OFFERING_START_REMINDER`): `CourseOfferingReminderScheduler` (täglich, siehe [Geplante Jobs](scheduled-jobs.md)) ruft pro Mandant `CourseReminderService.sendRemindersForTenant` auf. Fällig sind `PUBLISHED`-Angebote, deren erster Termin in der Zukunft liegt und innerhalb von `start-reminder-lead` (Standard 72 h). Empfänger sind `PARTICIPANT` und `WAITLIST`. Idempotenz über `start_reminder_sent_at` am Angebot; der Marker wird nur gesetzt, wenn mindestens ein Empfänger erinnert wurde (später genehmigte Teilnehmer erhalten die Erinnerung noch). Verschiebt die Org den ersten Termin (`syncSchedule`), wird der Marker zurückgesetzt. Zeitangaben in den Texten stehen in der Org-Zeitzone. Konfiguration: `emberone.course-offerings.reminder-cron`, `start-reminder-lead` (Env siehe [Geplante Jobs](scheduled-jobs.md)). Da der Job täglich läuft, kann die Erinnerung bis zu 24 h später als die Vorlaufzeit kommen.

**Terminänderung.** Ändert die Org Termine eines `PUBLISHED`-Angebots (`addSession`, `updateSession`, `deleteSession`), nimmt der Service dafür den Zeilen-Lock und veröffentlicht nach dem Speichern ein `CourseOfferingSessionsChangedEvent` mit den Usern, die in diesem Moment `PARTICIPANT` oder `WAITLIST` sind. Das ist ein Änderungssatz pro Aufruf: ein User erhält pro Aufruf genau eine Benachrichtigung `COURSE_OFFERING_SESSIONS_CHANGED`, nicht eine pro Termin. Eine Änderung an Typ oder Hinweis eines Termins (Datum, Zeit und Ort unverändert) benachrichtigt nicht, ebenso wenig Entwurf, Vorlage oder ein Angebot ohne Teilnehmer und Warteliste. Mehrere Aufrufe hintereinander ergeben mehrere Benachrichtigungen; es gibt keine Bündelung über Aufrufe.

## Berechtigungen

Modul `COURSE_OFFERINGS` (abschaltbar pro Standard), `COURSE_OFFERING_READ` (33) und `COURSE_OFFERING_WRITE` (34), siehe [Sicherheit](security-and-permissions.md).

## API (Org)

`/api/v1/auth/organizations/{orgId}/course-offerings`: `GET` (paginiert, ohne Vorlagen, Filter `status`, Suche Lehrgangstyp/Inhalt/Hinweise, Standard-Sort `startDate,asc` (Kursbeginn), weiterer Sort-Key `trainingCourseType`), `GET /templates` (paginiert, `COURSE_OFFERING_READ`; nur Vorlagen), `GET /list-filter-options` (Filterwerte der Liste), `GET /offerable-types` (`@BoundedList`), `GET /audience-options` (`@BoundedList`, `COURSE_OFFERING_READ`: aktive Orgs und Org-Typen des Mandanten nur mit Id/Name für die Zielgruppen-Auswahl), `GET/PUT/DELETE /{id}`, `POST /{id}/publish`, `POST /{id}/cancel` (Body `reason`), `POST /{id}/save-as-template`, `POST /{id}/instantiate`, `PUT /{id}/audiences` (Einträge mit optionalem `quota`), `PUT /{id}/prerequisites`, `POST/PUT/DELETE /{id}/sessions[/{sessionId}]`.

Anmeldungen eines Angebots, `/{id}/registrations`: `GET` (paginiert; Suche nach Name, Filter `status` (mehrfach), Sort `registeredAt` (Standard) / `approvedAt` / `name`; `COURSE_OFFERING_READ`), `GET /{registrationId}` (eine Anmeldung des Angebots), `GET /summary` (Plätze: `maxParticipants`, `participants`, `waitlist`, `pending`, `freeSeats`, `quotas`), `POST /{registrationId}/promote` und `POST /{registrationId}/remove` (Body `reason`; beide `COURSE_OFFERING_WRITE`). Mitgliederansicht siehe oben. Abschluss: `GET /{id}/completion`, `PUT /{id}/registrations/{registrationId}/final-result`, `POST /{id}/complete` (siehe [Abschluss](#abschluss-kiss)). `CourseOfferingDto` enthält `cancelledAt` und `cancelReason` für abgesagte Angebote.

## API (Self)

`GET /api/v1/self/course-offerings` (`ListQuery`, Standard-Sort `startDate,asc`): veröffentlichte Angebote, deren Anmeldeschluss noch nicht vorbei ist und die zur Zielgruppe des Users passen, **plus** Angebote mit eigener aktiver Anmeldung (`PENDING` / `PARTICIPANT` / `WAITLIST`). Lehrgänge, die der User schon besitzt (Lehrgangstyp in einem Nachweis), sind ausgeblendet; `includeHeld=true` blendet sie ein, `alreadyHeld` markiert sie, `full` zeigt ein volles Angebot (Anmeldung landet auf der Warteliste). Angebote, deren Voraussetzungen der User nicht erfüllt, sind ausgeblendet; `includeUnfulfilled=true` blendet sie ein, `prerequisitesMet=false` markiert sie. Optional `registration` (Mehrfachauswahl: `OPEN` = Anmeldefenster offen, `CLOSED` = nicht offen, `FULL` = ausgebucht) und `participation` (Mehrfachauswahl: `REQUESTED` = eigene Anmeldung `PENDING`, `PARTICIPANT`, `WAITLIST`). Innerhalb eines Parameters gilt Vereinigung, zwischen beiden UND; ohne Parameter keine Einschränkung. Das Frontend setzt `registration=OPEN` standardmäßig. `myRegistration` am DTO trägt Status und Aktionen der eigenen aktiven Anmeldung. Die UI zeigt eine flache, serverseitig gefilterte und sortierte Liste ohne clientseitige Gruppierung.

`GET /api/v1/self/course-offerings/{id}` zeigt ein Angebot, das der User sehen darf: veröffentlicht, Anmeldeschluss nicht vorbei, zur Zielgruppe passend, unabhängig davon, ob der User den Lehrgangstyp schon besitzt (`alreadyHeld`). Die Sichtbarkeit ist identisch zur Liste ohne `includeHeld`, aber nicht über den Listenfilter steuerbar; ein Parameter existiert nicht. Hat der User eine eigene Anmeldung auf dem Angebot, gilt das unabhängig von allen Regeln, auch nach Absage des Angebots.

`GET /api/v1/self/course-registrations` ("Meine Anmeldungen", `ListQuery`, Sort `startDate` (Standard absteigend) / `registeredAt`): alle Anmeldungen des Users, jeweils mit Status, Wartelistenplatz, `endedBy`/`endReason` und was noch geht: `canCancel` (Absage-Workflow möglich, gleiche Regeln wie beim Öffnen), `cancellationPending` (Absage offen, Platz bleibt bis zur Genehmigung), `canWithdraw` (Wartelistenplatz direkt zurückziehbar), dazu `workflowId` und `cancellationWorkflowId` für die Workflow-Anzeige. `GET /api/v1/self/course-registrations/{id}` liefert eine eigene Anmeldung. `POST /api/v1/self/course-registrations/{registrationId}/withdraw` zieht einen Wartelistenplatz zurück. Beide nur für Self-Service-User und immer auf den Aufrufer begrenzt.

## Frontend

Getrennte Flächen wie im Backend ([Detail-Routing](frontend-detail-routing.md)): Self `/my-area`, Org `/organizations/[id]`, Admin Standard-Editor. Daten über `lib/api/courseOfferings.ts`, Store-Hooks in `lib/store/queries/courseOfferings.ts`, Listen über `lib/list/courseOfferingList.ts`.

| Fläche | Route | Gate / Hinweis |
|--------|-------|----------------|
| **Org-Hub** | Kachel im Abteilungs-Hub | `COURSE_OFFERING_READ`, Modul `COURSE_OFFERINGS` |
| **Org-Liste** | `/organizations/[id]/course-offerings` | `CourseOfferingListView`, Filter `status`; „Vorlagen“ öffnet ein Modal mit allen Vorlagen (Klick → Detailseite zum Bearbeiten, Löschen per Zeilenaktion; angelegt werden Vorlagen über „Als Vorlage speichern“ am Angebot); „Hinzufügen“ fragt „Neues Angebot“ oder „Aus Vorlage“ (Vorlage wählen, Datum des ersten Termins → Entwurf) |
| **Org-Anmeldungen** | `/organizations/[id]/course-offerings/[offeringId]/registrations` | Teilnehmerliste mit Suche, Status-Filter und Sortierung; offene Anträge hervorgehoben, Zeilen-Actions Nachrücken/Entfernen |
| **Org-Anmeldung** | `…/registrations/[registrationId]` | Details (Status, Zeiten, Begründung), Link zum Anmeldeantrag (Freigabe/Ablehnung läuft im Workflow), Entscheidungen Nachrücken/Entfernen |
| **Org-Zielgruppe** | `…/[offeringId]/audiences/[audienceId]` | `CourseOfferingAudienceGroupDetailView`; Detail einer Zielgruppen-Gruppe mit Kontingent |
| **Org-Voraussetzung** | `…/[offeringId]/prerequisites/[kind]` | `CourseOfferingPrerequisiteKindDetailView`; Voraussetzungen einer Art (Lehrgang, Nachweis, Führerschein) |
| **Org-Termin** | `…/[offeringId]/sessions/[sessionId]` | `CourseOfferingSessionDetailView`; Detail eines Termins |
| **Org-Ergebnisse** | `…/completion` bzw. `…/completion/[registrationId]` | Liste der Teilnehmerergebnisse (Bestanden und Lehrgang gutschreiben); Detail mit Switch; Zeilen-Actions auf Desktop, auf Mobile die Detailseite |
| **Org-Detail** | `/organizations/[id]/course-offerings/[offeringId]` | Stammdaten, Termine, Zielgruppe, Voraussetzungen; veröffentlicht: Belegung mit Links zu Teilnehmer und Ergebnisse; Aktion „Lehrgang abschließen“ (Confirm) in Aktionen; Absage (`CourseOfferingCancelDialog`); abgesagt: `CourseOfferingCancelledBanner` |
| **Admin Org** | `/admin/tenants/[id]/organizations/[orgId]` Karte „Anbietbare Lehrgänge“ | `AdminOrganizationCourseOfferingTypeSection`, API `…/organizations/{orgId}/course-offering-types`, nur bei aktivem Modul |
| **Mein Bereich** | `/my-area/course-registrations` | Eine Liste berechtigter Angebote inkl. eigener aktiver Anmeldung (`myRegistration` am `SelfCourseOfferingDto`); Zeile → `…/offerings/[id]`; Anmelden nur per Listen-Action bzw. auf der Detailseite (`CourseOfferingRegisterDialog`) |
| **Mitglied** | Org-Benutzerverwaltung, Section `course-registrations` | `UserCourseRegistrationsSection`, `TRAINING_COURSE_READ` (nicht `COURSE_OFFERING_READ`) |
| **Org Mitglieder** | `/organizations/[id]/course-registrations` | `OrganizationCourseRegistrationListView`, Menüpunkt unter Mitglieder, `TRAINING_COURSE_READ` (nicht `COURSE_OFFERING_READ`) |

Workflow-Anzeige: Input-Plugins `courseOfferingRegistration` und `courseRegistrationCancellation` in `lib/workflows/inputDisplay/plugins/` (Payload-Parsing in `payloadParsers.ts`, Typen in `types/workflow.ts`). Öffnen: `openCourseOfferingRegistration` / `openCourseRegistrationCancellation` in `lib/api/workflows.ts`.

## Quellcode-Karte (Frontend)

| Thema | Ort |
|-------|-----|
| Org-UI | `frontend/components/organization/course-offerings/*`, Pages unter `app/organizations/[id]/course-offerings/` |
| Mein Bereich | `frontend/components/my-area/courseOfferings/*`, `app/my-area/course-registrations/page.tsx` |
| Navigation | `organizationNavItems.tsx`, `myAreaSections.ts`, `userManagementSections.ts` |
| Utils / Typen | `lib/utils/courseOffering.ts`, `types/courseOffering.ts` |
