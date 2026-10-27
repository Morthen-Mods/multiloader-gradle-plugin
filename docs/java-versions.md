# Java-Versionen je Minecraft-Version

Quelle: `javaVersion.majorVersion` aus den offiziellen Mojang-Versions-JSONs
(URLs aus `~/.gradle/caches/fabric-loom/mojang_versions_manifest.json`). Geprüft wurden die
Grenzversionen; Versionen dazwischen wurden nicht einzeln abgefragt, die Bereiche sind
daraus abgeleitet.

| Minecraft-Version | Java |
|---|---|
| bis 1.16.5 | 8 |
| 1.17 – 1.17.1 | 16 |
| 1.18 – 1.20.4 | 17 |
| 1.20.5 – 1.21.11 | 21 |
| 26.1 und neuer (bis 26.3 geprüft) | 25 |

Gemessene Werte: 1.16.5 = 8, 1.17 = 16, 1.17.1 = 16, 1.18 = 17, 1.18.2 = 17, 1.19.4 = 17,
1.20 = 17, 1.20.1 = 17, 1.20.4 = 17, 1.20.5 = 21, 1.20.6 = 21, 1.21 = 21, 1.21.1 = 21,
1.21.11 = 21, 26.1 = 25 (26.1.1, 26.1.2, 26.2, 26.3 ebenfalls 25).

Snapshots und Versionen vor 1.16.5 (z. B. 1.12.2 -> Java 8) wurden nicht abgefragt.

## Abgleich mit `MinecraftVersion.javaVersion`

Die Funktion liefert `17` unter 1.20.5, `21` unter 26 und sonst `25`. Das stimmt für
1.18 und neuer. Für 1.17.x (Mojang: 16) und 1.16.5 und älter (Mojang: 8) liefert sie
fälschlich `17`. Relevant nur, falls diese Versionen unterstützt werden sollen.
