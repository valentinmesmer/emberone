# Mitwirken an EmberOne

Die Entwicklung an EmberOne ist derzeit **dem Maintainer vorbehalten**. Pull Requests von außen werden aktuell nicht angenommen.

Willkommen sind:

- Fehlerberichte und Verbesserungsvorschläge als Issue (Deutsch oder Englisch)
- Sicherheitsmeldungen laut [`SECURITY.md`](SECURITY.md) (keine öffentlichen Issues)

Der Code steht unter der [AGPL-3.0-or-later](LICENSE) und darf im Rahmen der Lizenz geforkt und weiterentwickelt werden. Name und Logo siehe [`TRADEMARK.md`](TRADEMARK.md).

## Konventionen

- Code und Commits: Englisch, [Conventional Commits](https://www.conventionalcommits.org/). Dokumentation: Deutsch.
- Setup: [`support/dev/DEV.md`](support/dev/DEV.md), Git-Hooks: `./support/scripts/install-git-hooks.sh`.
- Arbeitsregeln für KI-Assistenten: [`AGENTS.md`](AGENTS.md).

## Checks vor einem Merge

```bash
cd backend && ./gradlew build
cd frontend && bun run lint && bun run build
python3 support/scripts/add-copyright-headers.py --check
```

Verhalten sich APIs, Berechtigungen oder Lebenszyklen anders, passen die Docs unter `support/docs/` an.

## Verhaltenskodex

[`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md)
