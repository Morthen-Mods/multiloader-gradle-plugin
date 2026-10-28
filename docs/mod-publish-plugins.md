# Upload-Plugins für CurseForge und Modrinth: welches lässt sich abstrahieren?

Quellen: Projekt-Dateien (`gradle/libs.versions.toml`, `README.md`, `examples/*/build.gradle.kts`), die [README von mod-publish-plugin](https://github.com/modmuss50/mod-publish-plugin) und eine Websuche. Die Details von CurseGradle und CurseForgeGradle sind **(unverifiziert)**, die Suche hat dazu nichts Belastbares geliefert. Ich habe keine Plugin-Quellen gelesen, nur Dokumentationsseiten.

## Wichtigster Befund

Das Projekt hat sich schon für ein Plugin entschieden: `me.modmuss50.mod-publish-plugin` 2.2.1 steht in `libs.versions.toml` und als `implementation` in `plugin/build.gradle.kts`. Die Beispielprojekte laden es mit `apply false`, und die `README.md` beschreibt eine geplante `withModPublish { ... }`-Abstraktion. In `MultiloaderPlugin.kt` und `MultiloaderSettingsExtension.kt` stehen dazu nur `TODO`-Kommentare. Es geht also eher darum, ob diese Wahl trägt.

## Vergleich

| Plugin | Plattformen | Bewertung für die Abstraktion |
|---|---|---|
| **mod-publish-plugin** (modmuss50) | CurseForge, Modrinth, GitHub, Discord | Eine DSL für alle Ziele, Dry-Run und Retries laut Suchergebnis. Gemeinsame Werte (`file`, `changelog`, `type`, `modLoaders`) liegen im Top-Level-Block `publishMods`, plattformspezifische in `curseforge`/`modrinth`. Diese Aufteilung passt direkt auf ein Multiloader-Plugin. |
| **Minotaur** (Modrinth, offiziell) | nur Modrinth | Zwei Plugins pro Projekt, zwei DSLs. Ein Abstraktionsschicht müsste beide DSLs vereinheitlichen. |
| **CurseGradle** / **CurseForgeGradle** | nur CurseForge | Wie Minotaur, nur für die andere Plattform. **(unverifiziert)** |
| **ModPublisher** (firstdarkdev) | Modrinth, CurseForge, GitHub | Ähnlicher Umfang. Der letzte Push im Repo war laut Suchergebnis im Juli 2024, die Pflege ist also fraglich. |
| Galacticraft `net.galacticraft.publish.*`, ModdingX ModUtils | CurseForge, Modrinth | Nischenlösungen, für ein Plugin als Abhängigkeit unattraktiv. |

## Empfehlung

Bei mod-publish-plugin bleiben. Die Gründe:

1. Ein Plugin deckt beide Plattformen ab, das Projekt muss nur eine API abbilden.
2. Das Modell "gemeinsame Basis + Ziel-Blöcke" entspricht dem geplanten `withModPublish`.
3. Die Abhängigkeit ist bereits eingebunden und gepinnt.
4. Die Doku empfiehlt ausdrücklich eine exakte Version, das Projekt macht das schon.

## Beim Abstrahieren zu beachten

Alles aus der README des Plugins, nichts davon im Code geprüft:

- Bei CurseForge muss mindestens `client` oder `server` auf `true` stehen. Das Multiloader-Plugin kann es aus dem Modtyp ableiten oder einen Default setzen.
- CurseForge-Abhängigkeiten laufen über `requires("fabric-api")` und ähnliche Aufrufe. Modrinth und CurseForge brauchen dafür vermutlich unterschiedliche IDs/Slugs. Das Plugin sollte Abhängigkeiten pro Plattform mappen können. **(unverifiziert)**
- Jeder Zielblock hat genau eine `projectId`. Mehrere Loader-Jars für dasselbe Modrinth-/CurseForge-Projekt bedeuten deshalb mehrere Uploads in dasselbe Projekt, einen pro Loader-Modul. Die README beschreibt kein Multi-Projekt-Publishing. Das Plugin muss pro Loader-Subprojekt `file` und `modLoaders` setzen und die Task-Namen eindeutig halten.
- Minecraft-Versionen und Loader müssen pro Modul gesetzt werden (`minecraftVersions.add(...)`, `modLoaders.add(...)`). Die Werte stehen schon in der Settings-Extension (`minecraftVersion` usw.).
- Token und IDs aus Umgebungsvariablen bzw. Gradle-Properties lesen, wie in der README skizziert, und das Publishing überspringen, wenn sie fehlen. Dry-Run als Standard in CI-losen Builds ist sinnvoll, ob das Plugin ihn bietet, steht nicht in der gelesenen README.
- Offen: Gradle-/Java-Mindestversion von mod-publish-plugin. Die README nennt sie nicht, die Doku-Seite https://modmuss50.github.io/mod-publish-plugin/ habe ich nicht gelesen.
