# Third-Party Notices

EmberOne selbst steht unter der [AGPL-3.0-or-later](LICENSE). Dieses Dokument nennt Fremdkomponenten, die nicht über einen Paketmanager mit eigener Lizenzangabe (`frontend/package.json`, `backend/build.gradle`) eingebunden sind.

## Schriften

Alle drei Schriften stehen unter der [SIL Open Font License 1.1](https://openfontlicense.org) (OFL-1.1). Das Frontend lädt sie über `next/font/google` (siehe `frontend/app/layout.tsx`); Next.js liefert sie beim Build selbst aus. Die E-Mail-Templates (`backend/src/main/resources/templates/mail/`) und das Brand Book (`support/brand/`) referenzieren sie per `font-family`.

| Schrift | Verwendung | Copyright | Quelle |
| --- | --- | --- | --- |
| Manrope | Fließtext und UI | Copyright 2018 The Manrope Project Authors | <https://github.com/googlefonts/manrope> |
| Sora | Überschriften | Copyright 2019 The Sora Project Authors | <https://github.com/sora-xor/sora-font> |
| JetBrains Mono | Monospace (`font-mono`) | Copyright 2020 The JetBrains Mono Project Authors | <https://github.com/JetBrains/JetBrainsMono> |

Der vollständige Lizenztext der OFL-1.1 liegt unter <https://openfontlicense.org/open-font-license-official-text/>. Die Schriften werden unverändert verwendet; ihre Reserved Font Names bleiben unangetastet.

## Abhängigkeiten

Bibliotheken des Frontends (npm) und des Backends (Gradle) behalten ihre jeweiligen Lizenzen. Sie sind in `frontend/bun.lock` bzw. über die Gradle-Abhängigkeiten nachvollziehbar.
