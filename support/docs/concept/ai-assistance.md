# KI-Assistenz (Hilfe-Chat, Nachweis-Extraktion)

Design-Notizen für optionale KI-Funktionen. **Status: Prototyp (Chat, Extraktion); Mandanten-Schalter noch Konzept.** Package: `com.emberone.platform.ai` (Querschnitt); fachliche Endpunkte liegen in ihren Domänen. Abweichungen des Prototyps vom Konzept: siehe [Prototyp-Stand](#prototyp-stand). Verwandt: [Sicherheit & Berechtigungen](security-and-permissions.md), [Multi-Tenancy](multi-tenancy.md), [Token & Mandantenkontext](token-and-tenant-context.md), [Workflow-Engine](workflow-engine.md), [Attachment & Dokument-Promotion](attachment-and-document-promotion.md), [Aufbewahrung und Löschung](retention-and-erasure.md), [Logging](logging.md).

Zwei Funktionen, sonst keine:

1. **Hilfe-Chat**: „Wie reiche ich meine G26.3 ein?“ → kurze Anleitung plus Button, der den passenden Antrag startet.
2. **Nachweis-Extraktion (OCR)**: Beim Hochladen eines Nachweises füllt ein Vision-Modell das Formular vor; der User prüft und sendet selbst ab.

**Nicht im Scope:** KI-Entscheidungen in Workflows, Bewertung oder Prognose von Personen (Zuverlässigkeit, Verfügbarkeit), Chat über Org-Daten, Vektorindex mit Personendaten, Alarmierung/Einsatzführung, Training oder Fine-Tuning auf Mandantendaten.

## Prototyp-Stand

Umgesetzt sind die Phasen 0-2 (Gateway, Verfügbarkeits-Guard, Hilfe-Chat, Nachweis-Extraktion für Zertifizierung, Lehrgang, Führerschein). Alles ist standardmäßig aus (`emberone.ai.enabled=false`). Welchem Modell-Endpunkt welche Daten anvertraut werden, entscheidet allein der Betreiber (Auswahl des Endpunkts, AVV, Datenschutzprüfung); die Anwendung unterscheidet nicht nach Vertrauensstufen. Lokal einschalten: [DEV.md](../../dev/DEV.md) (Ollama + `gemma4`).

| Thema | Konzept | Prototyp | Später |
|-------|---------|----------|--------|
| Opt-in | Betreiber-Env **und** Tenant-Admin je Mandant/Funktion | Nur global per Env (`EMBERONE_AI_*`); kein Mandanten-Schalter, weil dafür eine Flyway-Migration nötig wäre | Mandanten-Setting (Tabelle/Spalte), Admin-UI, Prüfung zusätzlich im Guard und in `capabilities` |
| PDF | Vision-Modell liest Nachweis | Bilder (JPEG, PNG, WEBP, GIF) und PDFs. Bei PDFs rendert der Server bis zu drei Seiten mit Apache PDFBox zu JPEGs, schneidet leere Ränder ab (Karte klein auf A4) und schickt alle Seiten gemeinsam ans Modell. Die `/extract`-Endpunkte nehmen bis zu fünf Dateien (Multipart-Feld `file`, wiederholt, höchstens sechs Bilder) und senden alle als ein Dokument ans Modell, z. B. Vorder- und Rückseite; HEIC/HEIF (iPhone-Standard, oft als .jpeg benannt) wird bereits im Browser (`FileInput`, `heic-to`, siehe [Third-Party Notices](../../../THIRD_PARTY_NOTICES.md)) in JPEG umgewandelt; der Server lehnt HEIC mit klarer Meldung ab. Je Seite (max. 2048 px, höchstens 200 DPI); verschlüsselte oder leere PDFs werden abgelehnt | Mehrseitige PDFs auswerten |
| Rate-Limit | je User, Circuit Breaker | Je User und Minute, **in-memory pro Instanz** (`AiRateLimiter`); kein Circuit Breaker | Geteiltes Limit (Redis), Breaker |
| Strukturierte Ausgabe | `response_format` JSON-Schema | `JSON_SCHEMA` (Default, gegen Ollama geprüft); `JSON_OBJECT` mit Schema im Prompt als Fallback für Anbieter ohne Schema-Unterstützung (`EMBERONE_AI_STRUCTURED_OUTPUT`) | - |
| Reasoning | - | `reasoning_effort` wird mitgesendet (`EMBERONE_AI_REASONING_EFFORT`, Default `none`). Denkende Modelle wie `gemma4` auf Ollama brechen sonst die strukturierte Antwort ab (nur `{"text": "` kommt an); mit `none` auch deutlich schneller. Leer = Parameter wird nicht gesendet (für Anbieter, die ihn ablehnen) | - |
| Zertifizierung | Modell rät `config_key` | Der User wählt zuerst den Nachweis im Formular; `…/extract` verlangt diese Auswahl (Plausibilität gegen das Intervall der Definition). Der Typ-Rat des Modells erzeugt nur einen Hinweis (`TYPE_MISMATCH`) | - |
| Feldmarkierung | Felder einzeln als „KI-Vorschlag“ markiert | Hinweisblock im Formular „KI-Vorschlag - bitte prüfen“ mit Liste der vorbefüllten Felder und Hinweisen; keine Markierung je Feld | Markierung je Feld in den Formular-Komponenten |
| `USER_DOCUMENT_SUBMISSION` | Phase 2 danach | Umgesetzt: „Aus Dokument ausfüllen“ schlägt Titel, Beschreibung und bis zu 5 Tags vor (keine Auswahl nötig; Tags werden zu den schon gesetzten ergänzt) | - |
| Adresse, Name, Rang | Phase 2 danach | Umgesetzt (Anträge `ADDRESS_CHANGE`, `NAME_CHANGE`, `RANK_REQUEST`): Adressfelder, Vor-/Nachname bzw. Rang aus dem aktiven Rangkatalog. Der Nachweis ist teils optional, der Button erscheint, sobald eine Datei gewählt ist | - |
| Admin-Erfassung | Phase 2 danach | Umgesetzt: Die Formulare für Zertifizierung, Lehrgang und Führerschein in der Benutzerverwaltung lesen den Nachweis über `POST /api/v1/auth/organizations/{id}/users/{userId}/{certifications\|training-courses\|driving-license}/extract` (Schreibrecht der Organisation, Katalog des Mitglieds) | - |
| Onboarding (Rang-Nachweis) | Phase 2 danach | Nicht umgesetzt: eigener Zugangsweg per Onboarding-Token, braucht einen eigenen Endpunkt mit Token-Prüfung | Eigener Endpunkt am Onboarding-Zugang |
| Beobachtbarkeit | Zähler inkl. „Vorschlag unverändert übernommen“ | Micrometer: `emberone.ai.requests{function,outcome}`, `emberone.ai.tokens{function}` (ohne Inhalte); „unverändert übernommen“ fehlt | Zähler aus dem Frontend/Workflow-Open |

**Verfügbarkeit im Code:** `AiAvailabilityGuard` (`isAvailable` für Frontend/`capabilities`, `require` vor jedem Modellaufruf) prüft `enabled` und `base-url`. Der Guard wird dreifach durchlaufen: (1) `DocumentExtractionService` vor dem Lesen des Uploads, (2) `HelpChatService` vor dem Laden der Daten, (3) `AiGateway` unmittelbar vor dem Client. Ist KI nicht verfügbar, wirft er eine `AiUnavailableException` (`ConflictException`, 409 über den globalen Handler); Modellfehler sind `AiProviderException` (502), Rate-Limit `AiRateLimitException` (429), beide über `AiExceptionHandler`.

**Kein Zugriff am Service vorbei:** Chat und Extraktion laden Daten ausschließlich über bestehende Services im Request-Kontext des Users (`BeginnableWorkflowTypeService`, `WorkflowService.getOpenStateForCurrentUser`, `*CatalogService.list…ForUser`); `platform.ai` hat keine Repositories und setzt keinen Tenant-/User-Kontext. Die Extraktions-Endpunkte tragen dasselbe `@PreAuthorize` wie der Antrag (`canBeginSelfServiceWorkflow(<Typ>)`). Der Upload wird nur im Speicher verarbeitet (JDK-`ImageIO`: Alpha auf Weiß, Verkleinerung auf 2048 px, sonst unverändert), nichts wird gespeichert oder geloggt.

**Endpunkte:**

| Endpunkt | Berechtigung |
|----------|--------------|
| `GET /api/v1/self/ai/capabilities` | `isSelfServiceUser()` |
| `POST /api/v1/self/ai/help-chat` | `isSelfServiceUser()` |
| `POST /api/v1/auth/workflows/certification-record/extract` (Multipart `file`, `tenantCertDefinitionId`) | `canBeginSelfServiceWorkflow(CERTIFICATION_RECORD)` |
| `POST /api/v1/auth/workflows/user-document-submission/extract` (Multipart `file`) | `canBeginSelfServiceWorkflow(USER_DOCUMENT_SUBMISSION)` |
| `POST /api/v1/auth/workflows/{address-change\|name-change\|rank-request}/extract` (Multipart `file`) | `canBeginSelfServiceWorkflow(<Typ>)` |
| `POST /api/v1/auth/organizations/{id}/users/{userId}/{certifications\|training-courses\|driving-license}/extract` | `has{Certification\|TrainingCourse\|DrivingLicense}WritePermission(id)` |
| `POST /api/v1/auth/workflows/training-course-record/extract` | `canBeginSelfServiceWorkflow(TRAINING_COURSE_RECORD)` |
| `POST /api/v1/auth/workflows/driving-license-record/extract` | `canBeginSelfServiceWorkflow(DRIVING_LICENSE_RECORD)` |

**Hilfe-Chat:** Verlauf höchstens 10 Nachrichten à 2000 Zeichen (Bean-Validation), letzte Nachricht vom User. Das Modell wählt je Action genau ein `target` aus einer festen Enum (strukturierte Ausgabe), dazu `create` und `configKey`. Zwei Arten von Zielen:

- **Anträge** (`kind = WORKFLOW`): nur Typen, die der User starten darf (`SELF`-Scope, `beginnable-types`), nicht blockiert (`typeBlocked`), mit Hilfetext (`resources/ai/help/workflows/<WORKFLOW_TYPE>.md`). Ein `configKey` muss im Katalog des Typs liegen, sonst fällt die Action weg; bei Typen ohne Katalog wird er ignoriert. `catalogId`/`label` setzt der Server.
- **Seiten** (`kind = TOPIC`): `HelpTopic` (Enum, Bereich `SELF` / `ORGANIZATION` / `ADMIN`) mit Hilfetext unter `resources/ai/help/topics/<TOPIC>.md`. `HelpTopicAvailabilityService` entscheidet aus denselben Quellen wie die Frontend-Navigation: `SELF` für jeden Self-Service-User; `ORGANIZATION` je Organisation über die effektive Permission-Map (`UserPermissionService`, Lese-Permission des Themas), Anlegen nur in Organisationen mit der Schreib-Permission; `ADMIN` nur für Tenant- oder Global-Admins (`SecurityService`), mandantenbezogene Seiten nur mit Tenant im Token. Nicht verfügbare Themen sieht das Modell gar nicht. Die Hilfetexte sind sprachneutral (Englisch): App-Labels stehen als Platzhalter `{myArea}` im Text und werden je Mandanten-Locale aus `messages_<locale>.properties` (`help.label.*`) eingesetzt; Wörter, die Nutzer statt der Menü-Labels sagen, stehen je Thema unter `help.aliases.<TOPIC>` und erscheinen als Zeile „Also called“. Ein Test stellt sicher, dass jedes Thema Aliase hat und jeder Platzhalter für jede `SupportedLocale` aufgelöst wird. Die Reihenfolge der Themen im Enum folgt Hub bzw. Navigation und bestimmt die Reihenfolge im Prompt.

`HelpChatActionValidator` verwirft unbekannte oder nicht verfügbare Ziele (höchstens 3 Actions) und setzt `create` zurück, wenn der User dort nicht anlegen darf. **Organisationsnamen und -IDs gehen nie an das Modell**; der Server hängt die zulässigen Organisationen erst an die validierte Action. Das Frontend (`lib/ai/helpChatActions.ts`) bildet das Ziel auf eine feste Route ab (Anträge: Modal über `WorkflowBeginModalHost` mit `presetCatalogId`, bzw. Seite Lehrgangsanmeldung): bei genau einer Organisation direkt, bei mehreren zeigt der Chat „Für welche Organisation?“ mit einem Button je Organisation. `create` hängt `?create=1` an (`withCreateIntent`); die Listenseite öffnet dann ihren Anlegen-Dialog, falls der User dort schreiben darf (`useCreateIntent`, siehe [Frontend-Aktionen](frontend-actions.md)). Umgesetzt für Dienstleistung, Einsatz, Termine, Lehrgangsangebote sowie Mein Bereich Abwesenheiten, Notfallkontakt und Zeiterfassung.

**Sprache:** Hilfetexte und Prompt sind englisch (eine Quelle, kleine Modelle folgen englischen Anweisungen zuverlässiger). Oberflächenbegriffe stehen in Anführungszeichen genau so, wie die App sie zeigt (heute Deutsch), und bleiben unübersetzt. Die Antwortsprache kommt aus der Sprache des Mandanten (`TenantLocaleService`, `SupportedLocale`). Kommt eine zweite Sprache dazu, müssen die zitierten Oberflächenbegriffe je Sprache gepflegt werden (offener Punkt).

Der Chat hängt global im authentifizierten Layout (`AuthLayout` → `HelpChatPanel`), nur bei `capabilities.helpChat.available`; Antworten tragen das Label „KI-Antwort“.

**Extraktion:** SPI `DocumentExtractionSchema` (Spring-Bean je Domäne: `CertificationRecordExtractionSchema`, `TrainingCourseRecordExtractionSchema`, `DrivingLicenseRecordExtractionSchema`) liefert Prompt, JSON-Schema mit den erlaubten `config_key`s und den deterministischen Validator. Der Validator verwirft statt zu reparieren: nur strikte ISO-Daten, kein Datum in der Zukunft, Reihenfolgen, Stunden in (0, 1000] mit höchstens 2 Nachkommastellen, Texte nur innerhalb der Feldlängen, `config_key` ∈ Katalog (auf IDs abgebildet). Die Antwort enthält `fields` (Namen wie im Payload des Antrags) und `hints` mit stabilen Codes (`FUTURE_DATE`, `DATE_ORDER`, `VALIDITY_EXCEEDS_INTERVAL`, `TYPE_MISMATCH`, `VALUE_REJECTED`, `NOTHING_RECOGNIZED`); das Frontend übersetzt sie (`ai.extract.hints.*`).

## Grundsatz: Die Grenze liegt in der Architektur, nicht im Prompt

Eine KI lässt sich per Anweisung nicht zuverlässig begrenzen. Deshalb bekommt sie **keine eigenen Rechte**. Jede Funktion läuft mit dem Token bzw. Security-Kontext des Users über bestehende API-Endpunkte; dort greifen `@PreAuthorize` → `SecurityService` und FORCE RLS (`app.tenant_id`, `app.user_id`) wie bei einem Klick im Frontend. Was der User nicht darf, kann die KI technisch nicht - unabhängig davon, was im Prompt, in einem Dokument oder in einer Chat-Nachricht steht.

| Regel | Bedeutung |
|-------|-----------|
| **R1 Kein eigener Zugriff** | Kein DB-Zugriff, kein Service-Account, kein Systemkontext, kein `TenantContextHolder`-Override. KI-Code ruft nur Services/Endpunkte auf, die bereits per `@PreAuthorize` geschützt sind. |
| **R2 KI schreibt nicht selbst** | Schreibende Aktionen sind entweder Anträge (`WorkflowService.open`, danach entscheidet ein Mensch) oder Formular-Vorbefüllungen, die der User selbst absendet. |
| **R3 Code erzeugt Ziele, nicht das Modell** | Das Modell liefert nur IDs aus einer festen Menge (`WorkflowType`, `config_key`). Links baut der Server über `AppNavigationResolver` bzw. das Frontend aus einer festen Map. Ungültige IDs werden verworfen. |
| **R4 Keine KI-eigene Datenhaltung** | Prompts und Antworten werden weder geloggt noch persistiert. Kein Vektorindex mit Personendaten (wäre eine Schattenkopie am Hard-Delete-Modell vorbei). Übernommene Werte sind normale Fachdaten mit normalen Löschfristen. |
| **R5 Ohne KI funktioniert alles** | Jede Funktion ist ein Zusatz. Modell aus, Timeout oder Fehler → manueller Weg bleibt unverändert. |
| **R6 Opt-in** | Betreiber schaltet per Env ein, Tenant-Admin je Mandant und Funktion. Default: aus. |

## Mental Model

```
                 ┌────────────────────────── User-Kontext (JWT, SecurityContext, RLS) ──────────────────────────┐
                 │                                                                                                │
 Frontend ─Chat──┼─► HelpChatService ──► beginnable-types + Themen je Org (User) + Hilfetexte ──► AiGateway ──► Validierung ──► Actions
                 │                                                                                                │
 Frontend ─OCR───┼─► …/extract (gleiche Permission wie Antrag) ──► eigenes Upload + Schema ──► AiGateway ──► Validierung
                 └────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                                     │
                                                    AiAvailabilityGuard (KI aktiviert, Endpunkt gesetzt?)
                                                                     │
                                                 OpenAI-kompatibler Endpunkt: lokal (Ollama, vLLM) oder EU-Anbieter
```

Bei Chat und OCR ruft das Backend das vom Betreiber konfigurierte Modell auf.

## Funktion 1: Hilfe-Chat

Der Chat braucht **keine Personendaten**. Eingaben an das Modell:

1. Kurze Hilfetexte je Workflow-Typ und Katalogeintrag (statisch, versioniert).
2. Die Typen, die der User starten darf: `GET /api/v1/self/workflows/beginnable-types` (`canBeginSelfServiceWorkflow`), serverseitig im User-Kontext geladen.
3. Die Katalognamen (`config_key` + Anzeigename) der Nachweistypen des Mandanten, damit „G26.3“ einem Zertifizierungstyp zugeordnet werden kann.

**Ablauf:**

```
User: "Wie ändere ich meinen Namen?"
HelpChatService: beginnable-types + Hilfetexte + Katalognamen → Modell (strukturierte Ausgabe)
Modell:   { "text": "...", "actions": [ { "workflowType": "NAME_CHANGE" } ] }
Validierung: workflowType ∈ beginnable-types (und nicht typeBlocked)?  config_key ∈ Katalog?
Antwort: Text + Actions mit Ziel aus fester Route-Map (R3)
```

- Erfundene oder nicht erlaubte Typen fallen still weg; der Text erklärt dann ggf., dass der Antrag nicht verfügbar ist.
- Bei Nachweisen öffnet die Action den Antrag mit vorausgewähltem Typ (z. B. `CERTIFICATION_RECORD` + `config_key`).
- Verlauf nur im Browser; kein Server-Verlauf, keine Logs mit Inhalt.
- Freitext des Users kann Personendaten enthalten (User erzählt etwas). Der Chat fordert keine an.

## Funktion 2: Nachweis-Extraktion (OCR)

Gilt für alle Flows mit Nachweis-Upload: `CERTIFICATION_RECORD`, `TRAINING_COURSE_RECORD`, `DRIVING_LICENSE_RECORD`, Onboarding-Schritte mit Nachweis, ggf. `USER_DOCUMENT_SUBMISSION`.

```
Draft-Upload (bestehend, User-Kontext)
   ▼
POST …/extract  - gleiche Permission/Prüfung wie der zugehörige Antrag
   ▼
Eingabe: nur dieses Dokument + Feld-Schema des Ziel-Formulars + erlaubte config_keys
   ▼
AiGateway (Vision, strukturierte Ausgabe) → Validierung → Vorschlag
   ▼
Formular vorbefüllt, Felder markiert "KI-Vorschlag - bitte prüfen"; User sendet selbst ab
```

- **Ein Baustein, ein Schema je Nachweistyp.** Die Domäne deklariert Felder, erlaubte Werte und Plausibilitätsregeln (z. B. Gültigkeit nicht länger als das Intervall der Definition, Datum nicht in der Zukunft). Keine zweite Engine, kein Sonderweg pro Typ.
- Das Modell sieht **nur das gerade hochgeladene Dokument des Users** und den Katalog - keine anderen Datensätze.
- Validierung verwirft statt zu reparieren: Schema, `config_key` ∈ Katalog, parsebare Daten, deterministische Plausibilität im Code.
- Für Genehmiger nur Hinweise am Antrag (z. B. „Name im Dokument weicht vom Profil ab“), nie eine Entscheidung.
- Es wird nichts zusätzlich gespeichert; Dokument und Promotion laufen wie in [Attachment & Dokument-Promotion](attachment-and-document-promotion.md).

**Prompt-Injection im Dokument** (z. B. „Gültig bis 2099“): Wert ist für User und Genehmiger sichtbar und läuft durch die Plausibilitätsregeln.

## Plattform-Baustein `platform.ai`

| Baustein | Aufgabe |
|----------|---------|
| `AiGateway` | `complete(prompt, jsonSchema)`, Bild-Eingabe; einziger Weg zum Modell |
| `OpenAiCompatibleClient` | Schlanker Client über `RestClient` gegen die OpenAI-kompatible API (deckt Ollama, vLLM, llama.cpp-Server und gängige EU-Anbieter ab); Timeout, Rate-Limit je User, Circuit Breaker |
| `AiProperties` | `enabled`, `base-url`, `api-key`, Modell je Fähigkeit (`text`, `vision`) |
| `AiAvailabilityGuard` | Verweigert Aufrufe bei `enabled=false` oder fehlender `base-url`; liefert dem Frontend, welche Funktionen verfügbar sind |
| Mandanten-Schalter | Chat und OCR je Mandant; Default aus |

Kein Modellaufruf aus dem Browser; Keys bleiben im Backend.

**Betriebsprofile** (gleicher Code): aus · gemeinsamer lokaler Endpunkt beim Betreiber oder Landkreis (GPU) · Anbieter mit AVV. Die Eignung des Endpunkts für die gesendeten Daten verantwortet der Betreiber.

**Beobachtbarkeit:** Nur Micrometer-Zähler je Funktion (Aufrufe, Fehler, Tokens, „Vorschlag unverändert übernommen“). Keine Prompt-/Antwortinhalte in Logs (siehe [Logging](logging.md)). Falls Kostenzuordnung je Mandant nötig wird: Tabelle ohne Inhalte mit eigener `RetentionCategory`.

**Transparenz:** KI-Ausgaben sind in der UI als solche gekennzeichnet (Art. 50 AI Act).

## Tests

Jede Funktion bekommt einen `*SecurityTest`:

- Chat: Action nur, wenn Typ in `beginnable-types` des Users; erfundener Typ / fremder `config_key` wird verworfen.
- OCR: `…/extract` verlangt dieselbe Berechtigung wie der Antrag.
- Gateway: Funktion bei `enabled=false` oder fehlendem Mandanten-Schalter nicht verfügbar (Prototyp: nur `enabled`).

Qualität der Extraktion: kleiner Testsatz aus anonymisierten Musterdokumenten je Nachweistyp, vor Freischaltung gemessen.

## Reihenfolge

| Phase | Inhalt |
|-------|--------|
| 0 | `platform.ai`, Properties, Verfügbarkeits-Guard, Mandanten-Schalter, Security-Tests (Prototyp: ohne Mandanten-Schalter) |
| 1 | Hilfe-Chat (nutzt bestehendes `beginnable-types`) - Prototyp umgesetzt |
| 2 | Nachweis-Extraktion für Zertifizierung, Lehrgang, Führerschein (Prototyp umgesetzt, nur Bilder); danach Onboarding |

## Offene Punkte

| Thema | Klärung |
|-------|---------|
| Abhängigkeiten | Eigener schlanker Client (empfohlen) vs. Bibliothek - vorab abstimmen. |
| Migration | Der Mandanten-Schalter braucht Flyway - vorab abstimmen. Prototyp umgeht das mit Env/Property (siehe [Prototyp-Stand](#prototyp-stand)). |
| Modul vs. Mandanten-Setting | `Module` gruppiert `PermissionType`s; ohne eigene Permissions passt ein Mandanten-Setting besser. |
| Hilfetexte | Wo gepflegt (Repo, Admin-UI je Mandant) und wer sie aktuell hält. Prototyp: statisch im Repo (`resources/ai/help`). |
| Modellqualität | Prototyp getestet mit Ollama + `gemma4` (liest Bilder, auch wenn `ollama list` keine Vision-Fähigkeit ausweist). Antwortqualität und Extraktionsgenauigkeit sind noch nicht gemessen (Testsatz je Nachweistyp fehlt). |
| Betreiber-Doku | Vorlage für Verzeichnis der Verarbeitungstätigkeiten, AVV-Hinweise. |

## Quellcode-Karte

| Thema | Ort |
|-------|-----|
| Properties, Guard | `platform/ai/AiProperties.java`, `AiAvailabilityGuard.java` |
| Gateway, Client, Rate-Limit | `platform/ai/AiGateway.java`, `OpenAiCompatibleClient.java`, `AiRateLimiter.java`, `DocumentImageNormalizer.java` |
| Fehlerabbildung | `platform/ai/AiUnavailableException.java` (409), `AiProviderException.java` (502), `AiRateLimitException.java` (429), `AiExceptionHandler.java` |
| Capabilities | `platform/ai/AiCapabilitiesService.java`, `AiSelfController.java` |
| Hilfe-Chat | `platform/ai/helpchat/` (`HelpChatService`, `HelpChatSelfController`, `HelpChatActionValidator`, `HelpTextProvider`, `HelpTopic`, `HelpTopicAvailabilityService`), Hilfetexte `resources/ai/help/*.md` (Anträge) und `resources/ai/help/topics/*.md` (Seiten) |
| Extraktion (generisch) | `platform/ai/extraction/` (`DocumentExtractionService`, SPI `DocumentExtractionSchema`, `PreparedExtraction`, `ExtractionJson`) |
| Extraktion (je Domäne) | `certification/service/CertificationRecordExtractionSchema.java`, `trainingcourse/service/TrainingCourseRecordExtractionSchema.java`, `drivinglicense/service/DrivingLicenseRecordExtractionSchema.java` |
| Extract-Endpunkte | `workflow/certificationrecord/`, `workflow/trainingcourse/`, `workflow/drivinglicense/` (`…WorkflowController#extract`, gleiches `@PreAuthorize` wie `open`) |
| Startbare Workflow-Typen | `workflow/service/BeginnableWorkflowTypeService.java` (gemeinsam für `WorkflowSelfController` `/beginnable-types` und Chat), `workflow/dto/WorkflowTypeCatalogDto.java` |
| Zentrale In-App-Routen | `platform/navigation/AppNavigationResolver.java`, `platform/navigation/domain/AppLinkType.java` |
| Handler-SPI (Start-Gating) | `workflow/spi/WorkflowHandler.java` |
| Nachweis-Definitionen | `certification/tenant/TenantCertDefinition.java` |
| Audience-Validierung | `OAUTH2_AUDIENCES`, siehe [Token & Mandantenkontext](token-and-tenant-context.md) |
| Frontend API/Store | `frontend/lib/api/ai.ts`, `frontend/lib/store/queries/ai.ts` (`useAiCapabilities`, `useHelpChat`, `useExtractProofFromDocument`), `frontend/types/ai.ts` |
| Frontend Chat | `frontend/components/ai/HelpChatPanel.tsx` (eingebunden in `components/auth/AuthLayout.tsx`), feste Route-Map `frontend/lib/ai/helpChatActions.ts`, Start-Vorauswahl `WorkflowBeginModalHost` (`presetCatalogId`), Anlegen-Link `lib/navigation/createIntent.ts` + `hooks/useCreateIntent.ts` |
| Frontend Extraktion | `frontend/components/my-area/ai/AiExtractionAssist.tsx`, eingebunden in `CertificationRecordSubmitModal`, `TrainingCourseRecordSubmitModal`, `DrivingLicenseRecordSubmitModal` |
| Frontend-Start-Katalog | `frontend/lib/api/workflows.ts`, `frontend/lib/store/queries/workflows.ts` |
| Tests | `platform/ai/**` (Guard, Gateway, Client, Rate-Limit, Normalizer, Chat, Extraktion, `*SecurityTest`), `*ExtractionSchemaTest` je Domäne, `…WorkflowControllerSecurityTest#extract*` |
