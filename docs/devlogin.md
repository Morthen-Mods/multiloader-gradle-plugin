# DevLogin für alle Loader

Frage: Kann das Plugin `devLogin` für alle Loader anbieten?

**Kurzantwort:** Ja, für alle drei. **MDG** hat es nativ, bei **Loom** geht es mit überschaubarem Aufwand (und Loom hat zusätzlich eine eigene Login-Lösung). Bei **ForgeGradle 7** geht es mit dem Brute-Force-Ansatz aus deinem bestehenden `ForgeGradle.kt` (`configureBootstrapLaunch`), weil er die Main-Klasse schon zur Konfigurationszeit festlegt. Siehe 2.4.

Grundlage: Quellen von Loom 1.19.3, MDG 2.0.148 und ForgeGradle 7.0.40 aus dem Gradle-Cache sowie die README von [DevLogin](https://github.com/covers1624/DevLogin). Ungeprüftes ist als **(unverified)** markiert. Ich habe nichts davon ausprobiert.

---

## 1. Wie DevLogin funktioniert

DevLogin (`net.covers1624:DevLogin`) ist ein **Wrapper um die Main-Klasse**:

1. Das Jar liegt im Runtime-Classpath des Runs.
2. Die Main-Klasse des Prozesses wird `net.covers1624.devlogin.DevLogin`.
3. Die ursprüngliche Main-Klasse steht in der System-Property `devlogin.launch_target` (alternativ als Programm-Argument `--launch_target <klasse>`).
4. DevLogin meldet sich per Microsoft-Device-Code an (`microsoft.com/link`), hängt `--accessToken`, `--uuid`, `--username` und `--userType msa` an die Argumente und startet dann das Ziel.

Weitere Properties laut README: `devlogin.launch_profile` (Profil, Default `default`), `devlogin.storage` (Ort der Tokens), `devlogin.http_engine`.

Randbedingungen laut README: Java 8+, Minecraft 1.12+, Gson auf dem Classpath, Tokens liegen unverschlüsselt in `~/.devlogin/`. Die README nennt weder Maven-Koordinaten noch Gradle-Setup. MDG nutzt `net.covers1624:DevLogin:0.1.0.5` (`RunUtils.DEV_LOGIN_GAV`).

DevLogin ist nur für **Client-Runs** sinnvoll. Ein Server braucht keinen Account.

---

## 2. Stand pro Loader

| | MDG | Loom | ForgeGradle 7 |
|---|---|---|---|
| DevLogin nativ | ✅ `RunModel.devLogin` | ❌ | ❌ |
| Alternative | – | ✅ eigener Login: `microsoftLogin` / `microsoftLogout` | – |
| Main-Klasse des Prozesses | DevLaunch (`RunUtils.DEV_LAUNCH_MAIN_CLASS`) | DLI (`net.fabricmc.devlaunchinjector.Main`) | Slime Launcher, die Ziel-Klasse kommt per `--main` |
| Wo steht die „echte“ Main? | `RunModel.mainClass` bzw. UserDev-Config | `RunConfiguration.mainClass` (als `-Dfabric.dli.main`) | `SlimeLauncherOptions.mainClass`, Default aus der UserDev-JSON |
| Umsetzbarkeit für das Plugin | trivial | mittel | mittel (mit `configureBootstrapLaunch`) |

### 2.1 MDG (nativ)

`RunModel.getDevLogin()` (Default `false`) macht in `PrepareRunOrTest` genau den Wrapper-Trick:

```java
if (devLogin && mainClass != null) {
    sysProps.put("devlogin.launch_target", mainClass);
    mainClass = RunUtils.DEV_LOGIN_MAIN_CLASS;   // net.covers1624.devlogin.DevLogin
}
```

Die Dependency kommt über die Configuration `devLaunchConfig`, aber nur, wenn mindestens ein Run `devLogin` aktiviert hat (`ModDevRunWorkflow`). Im Plugin reicht also:

```kotlin
model.devLogin.set(run.devLogin)
```

### 2.2 Loom (nachbauen)

Loom kennt kein DevLogin, aber der Aufbau ist ähnlich wie bei MDG:

- `RunConfiguration.devLaunchMainClass` ist die Klasse, die der Prozess tatsächlich startet. Default: `net.fabricmc.devlaunchinjector.Main` (`Constants.DLI_ENTRYPOINT`).
- Die eigentliche Spiel-Main steht in `-Dfabric.dli.main=<mainClass>` (`DefaultRunConfigurationSettings`).
- `devLaunchMainClass` wird in den Gradle-Task, die IntelliJ-Config, die Eclipse-Config und die VS-Code-Config übernommen (`AbstractRunTask`, `ApplicationIntellijRunConfigWriter`, `GenEclipseRunsTask`, `GenVsCodeProjectTask`).

Daraus folgt der Plan:

```kotlin
if (run.devLogin.get() && run.type.get() == RunType.CLIENT) {
    cfg.systemProperties.put("devlogin.launch_target", cfg.devLaunchMainClass)   // vorherigen Wert merken, siehe unten
    cfg.devLaunchMainClass.set("net.covers1624.devlogin.DevLogin")
    // Dependency: net.covers1624:DevLogin:<version> auf den Runtime-Classpath des Source-Sets
}
```

Offene Punkte:

- **Reihenfolge:** `devLaunchMainClass` hat in Loom eine Convention und wird mit `finalizeValue()` abgeschlossen (`DefaultRunConfigurationSettings.finialiseValues`). Den alten Wert vor dem Überschreiben lesen, und `devlogin.launch_target` lazy aus der Convention ableiten, nicht aus einem festen String. Außerdem kann eine Sandbox-Konfiguration `devLaunchMainClass` selbst setzen (`SandboxConfiguration`).
- **Dependency:** Welche Configuration ist richtig? Das DLI hängt in Loom an `LOOM_DEVELOPMENT_DEPENDENCIES`. Ob DevLogin dort ebenfalls funktioniert oder auf `runtimeOnly` muss, ist **(unverified)**.
- **Obfuskierte Versionen:** `modImplementation` und Remapping betreffen nur Mods. DevLogin ist eine normale Library, `runtimeOnly` sollte genügen **(unverified)**.
- **IDE-Configs:** Weil Loom `devLaunchMainClass` überall übernimmt, sollten Gradle-Task und IDE-Run denselben Wrapper nutzen.
- **Zwei Login-Systeme:** Wenn `microsoftLogin` in Loom benutzt wird, bekommt der Gradle-Task `runClient` bereits die Auth-Argumente (`addMicrosoftAuthenticationArguments`). DevLogin zusätzlich würde das doppeln. Das gilt nur für den Gradle-Task und nicht für IDE-Runs.

### 2.3 Loom: Alternative ohne DevLogin

Loom hat eigene Tasks `microsoftLogin` und `microsoftLogout` (`LoomTasks.registerMicrosoftAuthTasks`). Der Account wird verschlüsselt gespeichert (`EncryptionKeyStoreFactory`), und `runClient` bekommt die Argumente automatisch (`getMicrosoftAuthenticationEnabled` ist nur für den Run namens `client` gesetzt). Mit `--offline` am Task lässt sich das abschalten.

Einschränkung: Das greift nur beim Starten über den Gradle-Task. Wer den Run direkt aus der IDE startet, bekommt keinen Login. Wenn dir das reicht, brauchst du für Loom gar kein DevLogin.

### 2.4 ForgeGradle 7

Ausgangslage ohne Zusatzwissen:

- Der Prozess startet Slime Launcher. Das Spiel-Main wird als `--main <klasse>` übergeben (`SlimeLauncherExec.exec`).
- `SlimeLauncherOptions.mainClass` ist `@Optional`. Der Wert kommt aus der UserDev-JSON (`inheritFromJson`), und danach überschreiben die eigenen Werte des Nutzers (`target.inherit(this)`).
- Die JSON wird **erst in `exec()`** gelesen. Das Plugin kennt die echte Main-Klasse zur Konfigurationszeit also nicht, und `devlogin.launch_target` hätte keinen Wert.

**Dein Ansatz** (`ForgeGradle.kt`, `SlimeLauncherOptions.configureBootstrapLaunch(client)`) löst das: Er setzt `mainClass` auf `net.minecraftforge.bootstrap.ForgeBootstrap` und schreibt die JVM-Args, `MCP_MAPPINGS` und die Launch-Args selbst. Damit ist die Ziel-Klasse zur Konfigurationszeit bekannt, und der Wrapper-Trick funktioniert wie bei MDG:

```kotlin
private fun SlimeLauncherOptions.configureBootstrapLaunch(client: Boolean, devLogin: Boolean) {
    mainClass.set("net.minecraftforge.bootstrap.ForgeBootstrap")
    // ... jvmArgs, environment, args wie bisher ...

    if (devLogin && client) {
        systemProperty("devlogin.launch_target", "net.minecraftforge.bootstrap.ForgeBootstrap")
        mainClass.set("net.covers1624.devlogin.DevLogin")
    }
}
```

Die echte Main-Klasse bitte nur einmal als Konstante halten, damit `mainClass` und `launch_target` nicht auseinanderlaufen.

Wichtig dabei:

- **Der Ansatz muss für alle Runs gelten, auch für `client` und `server`.** Aktuell nutzt du ihn laut KDoc nur für eigene Namen wie `testmodClient`. Bei `client` und `server` füllt FG die Main-Klasse selbst aus der JSON. Mit `devLogin` müsstest du die Main-Klasse dort ebenfalls selbst setzen, sonst fehlt `launch_target`. Ob ein selbst gesetztes `mainClass` die JSON sicher überschreibt, folgt aus der Reihenfolge in `inherit` (erst JSON, dann eigene Werte), ist aber nicht ausprobiert **(unverified)**.
- **Die Dependency darf nicht über `options.classpath` kommen.** In `SlimeLauncherExec.exec` ersetzt ein nicht-leeres `options.getClasspath()` den ganzen Classpath des Tasks (`setClasspath(options.getClasspath())`), also auch Slime Launcher. Besser: DevLogin über eine eigene Configuration (wie MDG mit `devLaunchConfig`) oder `runtimeOnly` auf den Classpath des Source-Sets legen. Wie der Default-Classpath von FG genau zusammengesetzt wird, habe ich nicht geprüft **(unverified)**.
- **Versionskopplung:** Dein KDoc sagt selbst, dass die Werte an Forge 26.1.2-64.1.0 und FG 7.0.30 hängen. Bei einem Update von Forge kann sich `ForgeBootstrap` ändern, und `devLogin` bricht dann mit. Das ist dasselbe Risiko wie bisher, nur mit einem zusätzlichen Glied in der Kette.
- **Zusammenspiel mit Slime Launcher:** Slime ruft die `--main`-Klasse auf, DevLogin ruft dann `ForgeBootstrap` auf. Ob diese Kette im Slime-Classloader ohne Probleme läuft, ist **(unverified)**. Das müsstest du einmal mit einem Client-Start testen.
- **Argumente:** DevLogin hängt `--accessToken`, `--uuid`, `--username` und `--userType msa` an und reicht alles an das Ziel weiter. Deine Launch-Args (`--launchTarget`, `--gameDir`, …) bleiben unverändert, aber ob `ForgeBootstrap` die zusätzlichen Argumente akzeptiert, ist **(unverified)**.

Nebenbefund zu FG: In `SlimeLauncherExec.exec` setzt die Zeile für `maxHeapSize` versehentlich `setMinHeapSize(options.getMaxHeapSize().get())` (Zeilen 174 und 175). Das betrifft meinen früheren Entwurf in `docs/run-configurations.md`, wo `maxHeap` auf FG nativ gemappt wird. Dort besser `-Xmx` in `jvmArgs` schreiben, bis das behoben ist. Ob das in neueren FG-Versionen anders ist, ist **(unverified)**.

---

## 3. Vorschlag für das gemeinsame Modell

```kotlin
abstract class Run {
    // ...
    abstract val devLogin: Property<Boolean>   // Default false, nur für CLIENT

    init { devLogin.convention(false) }
}
```

| Loader | Umsetzung | Hinweis |
|---|---|---|
| MDG | `model.devLogin.set(run.devLogin)` | nativ |
| Loom | Wrapper über `devLaunchMainClass` + `devlogin.launch_target` + Dependency | Verhalten prüfen, siehe 2.2 |
| FG | `configureBootstrapLaunch` mit DevLogin-Wrapper | Main-Klasse für alle Runs selbst setzen, Dependency nicht über `options.classpath`, siehe 2.4 |

Regeln für den Adapter:

- Bei `RunType.SERVER` und gesetztem `devLogin` warnen und ignorieren.
- Bei Loom und aktivem `devLogin` nicht gleichzeitig `microsoftLogin` bewerben, sonst gibt es zwei Systeme.
- Wenn der FG-Ansatz für ein Forge-Versions-Update nicht mehr passt, lieber mit klarer Fehlermeldung abbrechen als still ohne Login zu starten.

---

## 4. Offene Fragen

- Reicht dir für Loom die eingebaute `microsoftLogin`-Lösung (nur Gradle-Task), oder soll `devLogin` auch IDE-Runs abdecken?
- Welche DevLogin-Version soll das Plugin pinnen? MDG nutzt `0.1.0.5`, mit Renovate-Kommentar. Das Maven-Repo ist laut README `nexus.covers1624.net` **(unverified: genaue URL)**.
- Soll `devLogin` einen globalen Default in `multiloaderSettings { }` bekommen, wie `minecraftVersion` und Co.?
- FG: Soll `configureBootstrapLaunch` künftig auch `client` und `server` abdecken, damit `devLogin` dort funktioniert?

---

## 5. Einschätzung: Lohnt sich der Einbau?

**Empfehlung: ja, aber als Opt-in-Option und nicht als Default, und zuerst für MDG und Loom.**

Dafür spricht:

- **Nutzen:** Ein Login im Dev-Client ist nötig für Multiplayer-Tests, Skins/Capes, Realms und alles, was Mojang-Services prüft. Ohne Login läuft der Client mit einem Offline-Account.
- **Aufwand bei MDG:** praktisch null, `RunModel.devLogin` existiert.
- **Aufwand bei Loom:** klein bis mittel (Wrapper über `devLaunchMainClass`). Loom bietet mit `microsoftLogin` zusätzlich eine Alternative für den Gradle-Task.
- **Passt ins Modell:** Eine Property `devLogin` auf `Run` (Default `false`) ist harmlos, solange nichts anderes davon abhängt.

Dagegen spricht:

- **FG ist teuer:** Der Ansatz hängt an hartcodierten Werten (`ForgeBootstrap`, Launch-Args), die an Forge-Versionen gekoppelt sind. Ein Fehler zeigt sich erst beim Start und nicht beim Build.
- **Mehr Wartung:** Eine zusätzliche Third-Party-Dependency (`net.covers1624:DevLogin`), deren Version du pflegen musst. Die README nennt weder Maven-Repo noch Koordinaten.
- **Sicherheit/Komfort:** Tokens liegen unverschlüsselt in `~/.devlogin/`. Das ist Sache des Nutzers, sollte aber in der Doku des Plugins stehen.
- **Ersatz vorhanden:** Wer nur den Gradle-Task nutzt, kommt auf Loom schon mit `microsoftLogin` aus.
- **Nur Client:** Für Server-Runs und Datagen bringt es nichts.

Vorschlag für die Reihenfolge:

1. `Run.devLogin` einführen, Default `false`, nur für `CLIENT` wirksam.
2. MDG: durchreichen.
3. Loom: Wrapper bauen oder bewusst auf `microsoftLogin` verweisen.
4. FG: zurückstellen, bis `configureBootstrapLaunch` ohnehin für `client` und `server` gilt. Bis dahin Warnung statt stiller Ignoranz.
