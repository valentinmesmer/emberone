# EmberOne

Mandantenfähige Feuerwehr-Verwaltungsplattform für Organisation, Personal, Qualifikation, Einsätze und Dienste, Termine und Freigabeprozesse.

*Multi-tenant management platform for fire departments (organization, personnel, qualifications, operations and duty events, appointments, approval workflows). Documentation is in German; English issues are welcome.*

EmberOne ist ein modularer Monolith (Spring Boot) mit Next.js-Frontend. Identitäten kommen über einen externen OIDC-IdP (derzeit Authentik), Domänendaten liegen in PostgreSQL mit Row Level Security.

## Stack

| Schicht | Technologie |
|---------|-------------|
| Backend | Java 25, Spring Boot 4.x, Flyway |
| Frontend | Next.js (App Router), React 19, Bun |
| Persistenz / Cache | PostgreSQL (FORCE RLS), Redis |
| Identity | OIDC/JWT (derzeit Authentik) |
| Storage / Mail | S3-kompatibel, SMTP |
| Realtime | STOMP über WebSocket |

## Lokale Entwicklung

Voraussetzungen: Docker (Compose v2), Java 25, Bun, Python 3.

```bash
git clone https://github.com/valentinmesmer/emberone.git && cd emberone
./support/dev/setup.sh                   # erzeugt Env-Dateien mit Dev-Secrets, aktiviert Git-Hooks
./support/dev/scripts/start-docker.sh    # Postgres, Redis, S3, Mailpit, Authentik
./support/dev/scripts/run-backend.sh     # http://localhost:8080
cd frontend && bun install && bun dev    # http://localhost:3000
```

Anmelden mit `akadmin` und dem Passwort `AUTHENTIK_BOOTSTRAP_PASSWORD` aus `support/dev/config/.env`. Ports, Dienste, Tests und Details: [`support/dev/DEV.md`](support/dev/DEV.md).

## Dokumentation

| Pfad | Inhalt |
|------|--------|
| [`support/docs/arc42/`](support/docs/arc42/00-index.md) | Architektur (Kontext, Bausteine, Deploy, ADRs, Risiken) |
| [`support/docs/concept/`](support/docs/concept/00-index.md) | Domänen- und Querschnittskonzepte (Workflow, Security, Org, …) |
| [`AGENTS.md`](AGENTS.md) | Arbeitsanweisung für KI-Modelle (Englisch) |

Code, Identifier und Commit-Subjects: Englisch. Projektdokumentation (`support/docs/`, Root-Markdown): Deutsch. [`AGENTS.md`](AGENTS.md) (KI-Modelle): Englisch. Commits: [Conventional Commits](https://www.conventionalcommits.org/). Formatierung und Commit-Form prüft Git (`./support/scripts/install-git-hooks.sh`).

## Mitwirken & Sicherheit

Siehe [`CONTRIBUTING.md`](CONTRIBUTING.md), [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) und [`SECURITY.md`](SECURITY.md).

## Lizenz

> **EmberOne ist Source Available, nicht Open Source.** Der Code steht unter der [Elastic License 2.0 (ELv2)](LICENSE) mit Zusatzerlaubnis.
>
> - **Erlaubt und dauerhaft kostenlos:** Selbst hosten, nutzen, anpassen und weitergeben, insbesondere durch Feuerwehren, Kommunen und öffentliche Träger.
> - **Verboten:** EmberOne (auch angepasst) als gehosteten oder verwalteten Dienst (SaaS) für Dritte anzubieten.
> - **Zusatzerlaubnis:** Öffentliche Feuerwehren, Gemeinden, Landkreise und andere öffentlich-rechtliche Träger, Feuerwehrverbände und Feuerwehr- bzw. Fördervereine sowie öffentlich-rechtliche oder vollständig kommunale IT-Dienstleister (z. B. kommunale Rechenzentren) dürfen EmberOne gemeinnützig, höchstens zum Selbstkostenpreis, für Feuerwehren hosten. Sie dürfen dafür auch einen Dienstleister beauftragen, der ausschließlich in ihrem Auftrag betreibt. Die Erlaubnis wird für bereits veröffentlichte Versionen nicht widerrufen. Den genauen Wortlaut regelt der Abschnitt „Additional Permission“ in der [`LICENSE`](LICENSE).
> - **Andere Konstellationen:** auf Anfrage an [emberone@valentinmesmer.de](mailto:emberone@valentinmesmer.de).
> - **Beiträge:** Die Entwicklung ist dem Maintainer vorbehalten; externe Beiträge sind künftig nur über eine CLA vorgesehen, siehe [`CONTRIBUTING.md`](CONTRIBUTING.md).

Die Lizenz gilt für den Code, nicht für Name oder Logos; siehe [`TRADEMARK.md`](TRADEMARK.md). Drittkomponenten (Schriften, Kartendaten): [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
