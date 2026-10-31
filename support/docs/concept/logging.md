# Logging

Design-Notizen für Backend-Logging über SLF4J / Lombok `@Slf4j` und Spring Boot Logback. Package roots: Domänen-`@Slf4j`, `com.emberone.platform.logging` (`RequestIdFilter`), MDC über `TenantContextSupport`. Eng verwandt: [Multi-Tenancy](multi-tenancy.md) (Holder + MDC), [Globale Exception-Behandlung](global-exception-handling.md) (`AccessDeniedLogSupport`), [Geplante Jobs](scheduled-jobs.md) (`TenantBatchRunner`), [Aufbewahrung und Löschung - Betrieb](retention-and-erasure.md#betrieb) (Log-Retention beim Betreiber).

Dieses Dokument regelt fachliche Ereignisse, Message-Stil, MDC-Kontext, ECS-JSON auf der Console und Request-Korrelation. Ohne einheitliche Regeln entsteht „jeder Service loggt anders“, und Korrelation über Mandant und Request bricht außerhalb des HTTP-Pfads.

## Geltungsbereich

| In Scope (verbindlich) | Nicht in diesem Dokument |
|------------------------|---------------------------|
| Levels, wann/was loggen, Message-Konvention | Frontend (`console.*`) |
| MDC `tenantId`, `userId`, `requestId` | OpenTelemetry / Trace-IDs in Logs |
| Structured Console (ECS JSON) | Log-Shipper, Retention und Alerting (Deploy/Betrieb; Retention-Platzhalter siehe [Betrieb](retention-and-erasure.md#log-retention)) |
| HTTP `X-Request-Id` (Accept / Generate / Echo) | Änderung der Exception-HTTP-Verträge |
| Batch- und WebSocket-Korrelation über `requestId` | |

## Mental Model

```
Ereignis (Mutation / Skip / Failure / Integration)
        │
        ▼
Logger der Klasse (Kategorie)  +  Message (was passiert ist)
        │
        ▼
MDC (Kontext: tenantId, userId, requestId)
        │
        ▼
Appenders / Format (Default: ECS JSON; lokal optional plain)
```

- **Logger-Klasse** = Kategorie (Package/Typ).
- **Message** = fachliches oder technisches Ereignis (Englisch, siehe Konvention).
- **MDC** = Request-/Mandanten-/Korrelationskontext - nicht in jeder Message wiederholen.
- **Exception-Anhang** nur bei echten Failures (`log.error("…", ex)` bzw. `warn` mit Ursache, wenn Stack relevant).

Keine zweite Logging-API, kein Domain-Event-Bus „nur für Logs“.

## Levels

| Level | Wann |
|-------|------|
| `ERROR` | Unerwartet; braucht Aufmerksamkeit. Exception mitgeben. Batch: Fehler pro Mandant isolieren, andere Mandanten weiterlaufen lassen. |
| `WARN` | Erwartbar, aber bemerkenswert: Access denied, Skip mit Grund, Retry, Client-Fehler in Exception-Handlern. |
| `INFO` | Fachliche Zustandsänderungen und Integrations-Outcomes, die man im Betrieb nachvollziehen will. |
| `DEBUG` | Technische Details (z. B. Mail gesendet, Client-Disconnect, STOMP-Internals). Default: aus (`root: INFO`). |

`TRACE` nur für sehr seltene diagnostische Fälle; nicht als Standard für Domänenarbeit.

## Wann loggen (fachlich)

### Ja

- Erfolgreiche Domain-Mutationen: Create / Update / Delete / Approve / Reject / Promote / Deactivate / Membership-Ende usw.
- Externe Systeme: Authentik, Object Storage, Mail-Fehler / -Retries.
- Security-Denials (über Handler / `AccessDeniedLogSupport`).
- Batch-Zusammenfassungen und Tenant-Failures in geplanten Jobs.

### Nein

- Reine Reads / Lookups ohne Seiteneffekt.
- Idempotente No-Ops ohne Skip-Grund.
- Derselbe Erfolg in Service **und** Controller (Primärort: Service oder Workflow-Handler).

## Message-Konvention

- Englisch (Code-Sprache).
- Klarer Action-Satz, bevorzugt past tense („Created …“, „Applied …“, „Skipping …“).
- SLF4J-Platzhalter `{}`; stabile Entity-IDs.
- Keine Secrets (Tokens, Passwörter), keine Klartext-PII wo IDs reichen (keine E-Mail-Inhalte, Adressen, Klartext-Namen in Routine-Logs).
- Felder, die bereits im MDC stehen (`tenantId`, `userId`, `requestId`), nicht redundant in jede Message schreiben.
  - Ausnahme: Security-Helfer wie `AccessDeniedLogSupport` (explizite Felder in der Message bleiben Absicht).

## MDC-Vertrag

| Key | Bedeutung | Wann gesetzt |
|-----|-----------|--------------|
| `tenantId` | Aktueller Mandant | Immer wenn `TenantContextHolder` einen Tenant setzt (HTTP-Filter, Admin-Override, Batch, WebSocket-Bind) |
| `userId` | Aktueller EmberOne-User | Wenn Holder einen User hat (JWT-/Principal-Pfad); Batch oft ohne User |
| `requestId` | Korrelations-ID | HTTP-Filter; Batch pro Tenant-Iteration; WS beim Bind |

- Sync: `TenantContextSupport.syncMdcFromHolder()` nach Holder-Schreiben; Clear in `finally` zusammen mit Holder-Clear.
- Admin-Pfad: Override nur `tenantId` (User unverändert) - weiterhin über Sync vom Holder, nicht per direktem `MDC.put` an Call-Sites.
- Trace-IDs (OpenTelemetry) werden später ergänzt - nicht manuell in MDC befüllen.

Details Holder ↔ HTTP: [multi-tenancy.md](multi-tenancy.md), [token-and-tenant-context.md](token-and-tenant-context.md).

## Wo loggen

| Ort | Rolle |
|-----|-------|
| Domänen-Services, Workflow-Handler | Primärort für `INFO` zu Mutationen |
| `@RestControllerAdvice` / Exception-Handler | `WARN` / `ERROR` gemäß [global-exception-handling.md](global-exception-handling.md) |
| Controller | Nur wenn kein Service-Log möglich oder request-spezifischer Rand (z. B. Upload-Fehler); Erfolg nicht doppelt |
| Platform (Storage, Mail, Realtime, Batch) | Integration / Infrastruktur; Erfolgs-Noise eher `DEBUG` |

## Structured Console (ECS JSON)

Spring Boot 4 nativ - **keine** zusätzliche Logstash-/Encoder-Dependency:

```yaml
logging:
  level:
    root: INFO
  structured:
    format:
      console: ecs
```

- `spring.application.name` (`emberone-app`) erscheint als ECS `service.name`.
- MDC-Keys landen in der strukturierten Ausgabe und sind für externe Log-Collection anschlussfähig.
- **Tests:** `src/test/resources/application.yml` überschreibt das ECS-Console-Format aus der Haupt-`application.yml` nicht.
- **Lokal:** ECS ist Default. Für menschenlesbare Console: Env `CONSOLE_LOG_STRUCTURED_FORMAT` leer setzen (Spring-Boot-Standard-Override).

Das ECS-Format ändert nur die Ausgabe, nicht die fachlichen Level- und Wann-Regeln oben.

## Request-Korrelation

HTTP-Header: `X-Request-Id`.

1. Eingehenden Wert übernehmen, wenn sicher (alphanumerisch plus `-`/`_`, Längenlimit ≤ 128); sonst UUID generieren.
2. `MDC.put("requestId", …)` und denselben Wert als Response-Header echoen.
3. `finally`: Key entfernen (eigenständig vom Tenant-MDC-Clear).
4. **Batch:** pro Tenant-Iteration eine UUID als `requestId` (ein Feld reicht; kein separates `jobId` nötig).
5. **WebSocket:** Session- oder Message-Korrelations-ID beim Bind; Clear mit MDC-Cleanup.

Kein W3C `traceparent` / OpenTelemetry in der App-eigenen Korrelation - `requestId` kann später neben Trace-IDs koexistieren.

## Checkliste „neuer Log“

1. Richtiger **Level**?
2. Richtiger **Ort** (Service/Handler statt doppelter Controller)?
3. Message: Englisch, Action, `{}`, IDs - **keine** Secrets/PII?
4. Kontext schon im **MDC**? Nicht redundant wiederholen.
5. Exception nur anhängen, wenn Diagnose nötig?
6. Passt das Ereignis zu „Wann loggen“ (Mutation / Integration / Security / Batch)?
