# Third-Party Notices

EmberOne selbst steht unter der [Elastic License 2.0 mit Zusatzerlaubnis](LICENSE). Dieses Dokument nennt Fremdkomponenten, die nicht über einen Paketmanager mit eigener Lizenzangabe (`frontend/package.json`, `backend/build.gradle`) eingebunden sind.

## Schriften

Alle drei Schriften stehen unter der [SIL Open Font License 1.1](https://openfontlicense.org) (OFL-1.1). Das Frontend lädt sie über `next/font/google` (siehe `frontend/app/layout.tsx`); Next.js liefert sie beim Build selbst aus. Die E-Mail-Templates (`backend/src/main/resources/templates/mail/`) und das Brand Book (`support/brand/`) referenzieren sie per `font-family`.

| Schrift | Verwendung | Copyright | Quelle |
| --- | --- | --- | --- |
| Manrope | Fließtext und UI | Copyright 2018 The Manrope Project Authors | <https://github.com/googlefonts/manrope> |
| Sora | Überschriften | Copyright 2019 The Sora Project Authors | <https://github.com/sora-xor/sora-font> |
| JetBrains Mono | Monospace (`font-mono`) | Copyright 2020 The JetBrains Mono Project Authors | <https://github.com/JetBrains/JetBrainsMono> |

Der vollständige Lizenztext der OFL-1.1 liegt unter <https://openfontlicense.org/open-font-license-official-text/>. Die Schriften werden unverändert verwendet; ihre Reserved Font Names bleiben unangetastet.

## Komponenten mit Copyleft-Bezug

Diese Komponenten werden unverändert und als separate Bibliothek verwendet. Sie behalten ihre Lizenz; ELv2 gilt nur für den EmberOne-Code. Die Lizenzen schränken Rechte an diesen Bibliotheken nicht ein, insbesondere nicht das Ersetzen und das Debuggen von Änderungen daran (Reverse Engineering dafür, LGPL-3.0 §4).

| Komponente | Verwendete Lizenz | Hinweis |
| --- | --- | --- |
| [OpenPDF](https://github.com/LibrePDF/OpenPDF) (`com.github.librepdf:openpdf`) | MPL-2.0 (Alternative LGPL-2.1) | Backend-JAR. Quellcode: <https://github.com/LibrePDF/OpenPDF>. Lizenztext: <https://www.mozilla.org/MPL/2.0/> |
| [Logback](https://logback.qos.ch) (`logback-classic`, `logback-core`) | EPL-2.0 (Alternative LGPL-2.1) | Backend-JAR. Quellcode: <https://github.com/qos-ch/logback>. Lizenztext: <https://www.eclipse.org/legal/epl-2.0/> |
| Jakarta EE APIs, Angus Mail (`jakarta.*`, `org.eclipse.angus`) | EPL-2.0 bzw. EDL-1.0 (Alternative GPL-2.0 mit Classpath Exception) | Backend-JAR. Quellcode: <https://github.com/jakartaee>, <https://github.com/eclipse-ee4j/angus-mail> |
| AspectJ Weaver | EPL-2.0 | Backend-JAR. Quellcode: <https://github.com/eclipse-aspectj/aspectj> |
| [libvips](https://www.libvips.org) über `@img/sharp-libvips-*` (Next.js/sharp) | LGPL-3.0-or-later | Frontend-Docker-Image, dynamisch gelinktes Binary für die Bildoptimierung; die Bibliothek ist austauschbar. Quellcode und gebündelte Komponenten: <https://github.com/lovell/sharp-libvips>, Lizenztexte: <https://www.gnu.org/licenses/lgpl-3.0.html> |
| [heic-to](https://github.com/hoppergee/heic-to) (npm `heic-to`, enthält [libheif](https://github.com/strukturag/libheif) als WebAssembly) | LGPL-3.0 | Frontend, unverändert als separate npm-Abhängigkeit. Wandelt HEIC-Fotos (iPhone) beim Auswählen im Browser in JPEG um und wird nur bei Bedarf nachgeladen; die Bibliothek ist über `frontend/package.json` austauschbar. Quellcode: <https://github.com/hoppergee/heic-to>, <https://github.com/strukturag/libheif>. Lizenztext: <https://www.gnu.org/licenses/lgpl-3.0.html> |
| [lightningcss](https://lightningcss.dev) | MPL-2.0 | Nur Build-Werkzeug des Frontends, nicht im ausgelieferten Image. |

Die Basis-Images (Eclipse Temurin/Debian bzw. Bun/Node) enthalten weitere Komponenten unter eigenen Lizenzen (u. a. GPL-Werkzeuge der Distribution). Sie liegen in den Images unverändert vor; ihre Quellen und Lizenzen stellen die jeweiligen Distributionen bereit.

## Abhängigkeiten

Bibliotheken des Frontends (npm) und des Backends (Gradle) behalten ihre jeweiligen Lizenzen. Sie sind in `frontend/bun.lock` bzw. über die Gradle-Abhängigkeiten nachvollziehbar.
