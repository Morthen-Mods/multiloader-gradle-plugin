# Vereinheitlichte Run-Configurations

Entwurf für eine Abstraktionsschicht über den Run-Containern von Fabric Loom, ModDevGradle (MDG) und ForgeGradle 7 (FG).

Grundlage sind die Quellen der Versionen aus `libs.versions.toml`: Loom 1.19.3, MDG 2.0.148 und ForgeGradle 7.0.40. Stellen, die ich nicht im Quellcode nachgeprüft habe, sind mit **(unverifiziert)** markiert.

---

## 1. Ist-Stand der drei Plugins

| | Loom | ModDevGradle | ForgeGradle 7 |
|---|---|---|---|
| Container | `loom.runConfigs` (Alias `loom.runs`) | `neoForge.runs` / `modDevGradle.runs` | `minecraft.dependency(...).runs` |
| Element-Typ | `RunConfigSettings` (`RunConfiguration`) | `RunModel` | `SlimeLauncherOptions` |
| Standard-Runs | `client` und `server` existieren schon | keine, alles muss angelegt werden | keine **(unverifiziert)** |
| Namensregel | frei | Regex `[a-zA-Z][\w-]*` | frei |
| Gradle-Task | `run<Name>` (`RunGameTask`) | `run<Name>` (`RunGameTask`) | `JavaExec`, Name **(unverifiziert)** |
| Eigenes Provider-API | ja (`Property`) | ja (`Property`) | ja (`Property`) |

### Property-Vergleich

| Konzept | Loom | MDG | FG |
|---|---|---|---|
| Typ (client/server/data) | `runtimeEnvironment: Property<String>`, `client()`, `server()` | `type: Property<String>`, `client()`, `clientData()`, `data()`, `server()`, `serverData()` | über den Run-Namen und die UserDev-JSON **(unverifiziert)** |
| Anzeigename in der IDE | `displayName` | `ideName` | nicht vorhanden |
| IDE-Ordner | `ideConfigFolder` | `ideFolderName` | nicht vorhanden |
| IDE-Config erzeugen | `generateRunConfig: Boolean` | `disableIdeRun()` (setzt `ideName` auf `""`) | nicht vorhanden |
| Arbeitsverzeichnis | `runDirectory: DirectoryProperty` | `gameDirectory: DirectoryProperty` | `workingDir: DirectoryProperty` |
| Main-Klasse | `mainClass` | `mainClass` | `mainClass` (Slime-Launcher-Ziel) |
| Programm-Argumente | `programArguments: ListProperty<String>` | `programArguments: ListProperty<String>` | `args: ListProperty<String>` |
| JVM-Argumente | `jvmArguments: ListProperty<String>` | `jvmArguments: ListProperty<String>` | `jvmArgs: ListProperty<String>` |
| System-Properties | `systemProperties: MapProperty<String, String>` | `systemProperties: MapProperty<String, String>` | `systemProperties: MapProperty<String, String>` |
| Umgebungsvariablen | `environmentVars: MapProperty<String, Object>` | `environment: MapProperty<String, String>` | `environment: MapProperty<String, String>` |
| Heap | nur über `jvmArguments` | nur über `jvmArguments` | `minHeapSize`, `maxHeapSize` |
| SourceSet | `sourceSet: Property<String>` | `sourceSet: Property<SourceSet>` | `with(sourceSet) { }` und `sources(...)` |
| Geladene Mods | global über `loom.mods` | `loadedMods: SetProperty<ModModel>` pro Run | `mods`-Container pro Run |
| Zusätzliche Libs | nein | `additionalRuntimeClasspathConfiguration` | `classpath`, `extraLibraries` |
| Vorab-Tasks | manuell über `tasks.named("runX").dependsOn` | `taskBefore(...)` / `tasksBefore` | manuell |
| Logging | nein | `logLevel`, `loggingConfigFile` | nein |
| Dev-Login | nein | `devLogin` | nein |
| Args/JVM-Args von der Vorlage erben | nein | nein | `inheritArgs`, `inheritJvmArgs` |
| Run von anderem Run erben | `inherit(parent)` | nein | nein |
| Task nachträglich konfigurieren | `task { }` | `tasks.named("runX")` | `tasks.named(...)` |
| Sonstiges | `appendProjectPathToDisplayName`, `preferGradleTask`, `devLaunchMainClass`, `startFirstThread()`, `serverWithGui()` | – | – |

---

## 2. Zielbild

Das Plugin bekommt ein eigenes `Run`-Modell. Es ist die **Schnittmenge** aller drei Plugins plus ein paar Properties, die sich auf jedem Loader emulieren lassen. Was nur ein Plugin kennt, geht über Escape-Hatches (Abschnitt 5).

```kotlin
multiloader {
    runs {
        register("client") {
            client()
            jvmArgs("-Xmx4G")
            systemProperty("foo", "bar")
        }
        register("server") {
            server()
            programArgs("nogui")
        }
    }
}
```

Data-Runs sind vorerst nicht Teil des Modells, siehe [Zusatz: Data-Runs](#9-zusatz-data-runs-später).

Das bestehende `abstract val runs: NamedDomainObjectContainer<Run>` in `MultiloaderExtension` bleibt unverändert, es bekommt nur ein gefülltes `Run`.

### 2.1 Das `Run`-Modell

```kotlin
package net.morthen.gradle.multiloader.dsl

enum class RunType { CLIENT, SERVER }

abstract class Run @Inject constructor(private val name: String) : Named {
    override fun getName() = name

    // Identität
    abstract val type: Property<RunType>
    abstract val mainClass: Property<String>          // optional, sonst Default des Loaders
    abstract val sourceSetName: Property<String>      // Default "main"

    // Prozess
    abstract val runDirectory: DirectoryProperty      // Default: <project>/run
    abstract val programArgs: ListProperty<String>
    abstract val jvmArgs: ListProperty<String>
    abstract val systemProperties: MapProperty<String, String>
    abstract val environment: MapProperty<String, String>
    abstract val minHeap: Property<String>            // z.B. "1G"
    abstract val maxHeap: Property<String>

    // IDE
    abstract val ideName: Property<String>            // Loom: displayName, MDG: ideName
    abstract val ideFolder: Property<String>
    abstract val generateIdeConfig: Property<Boolean> // Default true

    // Mods
    abstract val loadedMods: SetProperty<String>      // Mod-IDs, leer = alle

    // Task-Verkettung
    abstract val dependsOn: ListProperty<Any>

    init {
        sourceSetName.convention("main")
        generateIdeConfig.convention(true)
    }

    // Convenience-API
    fun client() = type.set(RunType.CLIENT)
    fun server() = type.set(RunType.SERVER)

    fun programArg(arg: String) = programArgs.add(arg)
    fun programArgs(vararg args: String) = programArgs.addAll(*args)
    fun jvmArg(arg: String) = jvmArgs.add(arg)
    fun jvmArgs(vararg args: String) = jvmArgs.addAll(*args)
    fun systemProperty(key: String, value: String) = systemProperties.put(key, value)
    fun environment(key: String, value: String) = environment.put(key, value)
}
```

Entscheidungen:

- **`environment` ist `Map<String, String>`.** Loom nimmt `Object`, MDG und FG nur `String`. `String` ist der kleinste gemeinsame Nenner, Loom bekommt `toString()`.
- **`sourceSetName` ist ein `String`.** Das passt zu `Testmod.sourceSetName` und `Gametest.sourceSetName`. MDG löst den Namen beim Anwenden zu einem `SourceSet` auf.
- **`heap` ist eigenes Property.** Nur FG kennt es nativ, auf Loom und MDG wird `-Xms` und `-Xmx` in die JVM-Args geschrieben. Das ist ein reiner Komfort, aber vermeidet doppelte Flags.
- **`ideName` und `generateIdeConfig` bleiben getrennt.** MDG kodiert „aus“ als leeren `ideName`, Loom hat ein eigenes Boolean. Der Adapter übersetzt.

### 2.2 Typ-Mapping

| `RunType` | Loom | MDG | FG |
|---|---|---|---|
| `CLIENT` | `client()` | `client()` | Run-Name `client` **(unverifiziert)** |
| `SERVER` | `server()` (fügt `nogui` hinzu) | `server()` | Run-Name `server` **(unverifiziert)** |

Data-Runs sind bewusst ausgelassen, siehe Abschnitt 9.

---

## 3. Adapter-Schicht

Jeder Loader bekommt einen Adapter. Das passt zur bestehenden Struktur (`plugins/FabricLoom.kt` hat ein `applyFabricLoom`).

```kotlin
interface RunApplier {
    fun apply(run: Run)
}
```

Die Runs werden **nach** der Konfiguration des Nutzers angewendet. Das ist wichtig: Loom hat selbst Conventions für `displayName` und Co., und `convention(absentProvider)` würde diese Defaults löschen. Deshalb nur setzen, wenn der Wert im `Run` gesetzt ist (`orNull`).

```kotlin
// in applyFabricLoom, nach loom.mods { }
target.afterEvaluate {
    ext.runs.forEach { run -> LoomRunApplier(loom, target, ext).apply(run) }
}
```

### 3.1 Loom

```kotlin
class LoomRunApplier(
    private val loom: LoomGradleExtensionAPI,
    private val project: Project,
) : RunApplier {
    override fun apply(run: Run) {
        // client und server existieren schon, alles andere wird angelegt
        val cfg = loom.runConfigs.findByName(run.name) ?: loom.runConfigs.create(run.name)

        when (run.type.orNull) {
            RunType.CLIENT -> cfg.client()
            RunType.SERVER -> cfg.server()
            null -> {}
        }

        run.mainClass.orNull?.let { cfg.mainClass.set(it) }
        cfg.sourceSet.set(run.sourceSetName)
        cfg.runDirectory.set(run.runDirectory)
        run.ideName.orNull?.let { cfg.displayName.set(it) }
        run.ideFolder.orNull?.let { cfg.ideConfigFolder.set(it) }
        cfg.generateRunConfig.set(run.generateIdeConfig)

        cfg.programArguments.addAll(run.programArgs)
        cfg.jvmArguments.addAll(run.jvmArgs)
        cfg.jvmArguments.addAll(run.heapFlags())                  // -Xms / -Xmx
        cfg.systemProperties.putAll(run.systemProperties)
        cfg.environmentVars.putAll(run.environment)

        // loadedMods: Loom kennt das nicht pro Run, die Mods sind global (loom.mods)
        run.dependsOn.orNull?.forEach { dep ->
            project.tasks.named("run${run.name.replaceFirstChar(Char::uppercase)}") { dependsOn(dep) }
        }
    }
}
```

Hinweise:

- Dein bisheriges `generateRunConfig.set(false)` in `configureEach` wird durch `generateIdeConfig` ersetzt. Der Default sollte dort `true` bleiben, das Plugin kann ihn pro Projekt (zum Beispiel für `common`) abschalten.
- Der Task-Name ist `run<Name>`, siehe `LoomTasks.getRunConfigTaskName`.
- `startFirstThread()` kannst du als Bool `Run.startOnFirstThread` anbieten (setzt `-XstartOnFirstThread` nur auf macOS). Das geht für alle drei Loader über die JVM-Args.

### 3.2 ModDevGradle

```kotlin
class MdgRunApplier(
    private val neoForge: NeoForgeExtension,
    private val project: Project,
) : RunApplier {
    override fun apply(run: Run) {
        val model = neoForge.runs.maybeCreate(run.name)   // Name muss [a-zA-Z][\w-]* erfüllen

        when (run.type.orNull) {
            RunType.CLIENT -> model.client()
            RunType.SERVER -> model.server()
            null -> {}
        }

        run.mainClass.orNull?.let { model.mainClass.set(it) }
        model.sourceSet.set(project.the<JavaPluginExtension>().sourceSets.named(run.sourceSetName))
        model.gameDirectory.set(run.runDirectory)

        if (run.generateIdeConfig.get()) {
            run.ideName.orNull?.let { model.ideName.set(it) }
        } else {
            model.disableIdeRun()
        }
        run.ideFolder.orNull?.let { model.ideFolderName.set(it) }

        model.programArguments.addAll(run.programArgs)
        model.jvmArguments.addAll(run.jvmArgs)
        model.jvmArguments.addAll(run.heapFlags())
        model.systemProperties.putAll(run.systemProperties)
        model.environment.putAll(run.environment)

        // Mods: Mod-IDs über neoForge.mods auflösen. Leer = MDG-Default (alle)
        if (run.loadedMods.get().isNotEmpty()) {
            model.loadedMods.set(run.loadedMods.map { ids -> ids.map { neoForge.mods.getByName(it) } })
        }

        run.dependsOn.get().forEach { dep ->
            model.taskBefore(project.tasks.named(dep.toString()))
        }
    }
}
```

Hinweise:

- MDG verlangt den Namen im Konstruktor und prüft ihn. Deshalb sollte `Run` beim Anlegen denselben Regex prüfen, damit der Fehler früh und mit einer klaren Meldung kommt.
- MDG setzt den `ideName` selbst auf `<Projekt> - <Name>`. Setze ihn nur, wenn der Nutzer einen Namen gesetzt hat.

### 3.3 ForgeGradle 7

```kotlin
class ForgeRunApplier(
    private val dependency: MinecraftDependency,
    private val project: Project,
) : RunApplier {
    override fun apply(run: Run) {
        val opts = dependency.runs.maybeCreate(run.name)

        run.mainClass.orNull?.let { opts.mainClass.set(it) }
        opts.workingDir.set(run.runDirectory)

        opts.args.addAll(run.programArgs)
        opts.jvmArgs.addAll(run.jvmArgs)
        run.minHeap.orNull?.let { opts.minHeapSize.set(it) }
        run.maxHeap.orNull?.let { opts.maxHeapSize.set(it) }
        opts.systemProperties.putAll(run.systemProperties)
        opts.environment.putAll(run.environment)

        // SourceSet-spezifisch: with(...) statt eigener Property
        val sourceSet = project.the<JavaPluginExtension>().sourceSets.getByName(run.sourceSetName.get())
        opts.with(sourceSet) { /* hier könnten sourceSet-spezifische Args/Props landen */ }
    }
}
```

Hinweise:

- FG hat **keinen** `RunType`. Die Run-Konfiguration kommt aus der UserDev-JSON und wird über den Namen zugeordnet (`inherit(configs, sourceSetName, name)` in `SlimeLauncherOptionsImpl`). Wie genau die Namen heißen müssen, habe ich nicht geprüft **(unverifiziert)**. Pragmatisch: `RunType` wird in den Namen übersetzt (`client`, `server`, `data`), und `Run.name` wird nur als Anzeige genutzt, oder man verbietet abweichende Namen für Typ-Runs.
- `inheritArgs` und `inheritJvmArgs` sind FG-spezifisch. Standard-Verhalten (erben) ist für das Plugin richtig, der Escape-Hatch (Abschnitt 5) reicht zum Abschalten.
- Auf FG gibt es keine IDE-Properties. `ideName`, `ideFolder` und `generateIdeConfig` werden dort ignoriert (siehe Abschnitt 4).
- Das Arbeitsverzeichnis hat bei FG den Default `runs/<sourceSet>/<name>`, bei den anderen `run/`. Wenn dir ein einheitlicher Default wichtig ist, muss das Plugin ihn selbst setzen: `run.runDirectory.convention(layout.projectDirectory.dir("run"))`.

---

## 4. Was nicht auf jedem Loader geht

| `Run`-Property | Loom | MDG | FG |
|---|---|---|---|
| `ideName` | ✅ | ✅ | ❌ ignorieren |
| `ideFolder` | ✅ | ✅ | ❌ ignorieren |
| `generateIdeConfig` | ✅ | ✅ (über `disableIdeRun`) | ❌ ignorieren |
| `minHeap` / `maxHeap` | ✅ als JVM-Arg | ✅ als JVM-Arg | ✅ nativ |
| `loadedMods` | ❌ global über `loom.mods` | ✅ | ✅ über `mods`-Container **(unverifiziert)** |
| `dependsOn` | ✅ über Task | ✅ nativ (`taskBefore`) | ✅ über Task |

Empfehlung: **Nicht still ignorieren, sondern einmal pro Run warnen** (`logger.warn`), wenn eine Property gesetzt ist, die der Loader nicht unterstützt. Sonst wundert sich der Nutzer, warum ein Wert keine Wirkung hat.

---

## 5. Escape-Hatches für Loader-spezifisches

Was nur ein Loader kennt, gehört nicht in das gemeinsame Modell, sollte aber erreichbar sein:

```kotlin
abstract class Run ... {
    internal var loomAction: Action<RunConfigSettings>? = null
    internal var mdgAction: Action<RunModel>? = null
    internal var forgeAction: Action<SlimeLauncherOptions>? = null

    fun loom(action: Action<RunConfigSettings>)  { loomAction = action }
    fun neoforge(action: Action<RunModel>)       { mdgAction = action }
    fun forge(action: Action<SlimeLauncherOptions>) { forgeAction = action }
}
```

```kotlin
register("client") {
    client()
    loom { appendProjectPathToDisplayName.set(false); startFirstThread() }
    neoforge { devLogin.set(true); logLevel.set(Level.DEBUG) }
    forge { inheritJvmArgs.set(false) }
}
```

Der jeweilige Adapter ruft seine Action am **Ende** von `apply` auf, damit der Nutzer immer das letzte Wort hat.

Problem: Das Plugin hat die Loader-Plugins nur als `compileOnly`. Die Typen in `Run` (`RunConfigSettings`, `RunModel`, …) würden beim Laden der Klasse aufgelöst, auch wenn das Plugin gar nicht auf dem Classpath liegt. Zwei Wege:

1. **Typ löschen:** `Action<Any>` plus Cast im Adapter. Einfach, aber keine IDE-Hilfe im Build-Skript.
2. **Getrennte Extension-Objekte** (`loom`, `neoforge`, `forge` als Extensions, die nur existieren, wenn das Plugin angewendet wurde). Saubere Typen, aber mehr Aufwand.

Für den Anfang reicht Weg 1.

---

## 6. Gametest und Testmod

Du hast schon `Gametest` und `Testmod` mit `sourceSetName` und `modId`. Daraus können Runs automatisch entstehen:

```kotlin
ext.testmod?.let {
    ext.runs.register("testmod") {
        client()
        sourceSetName.set(it.sourceSetName)
        loadedMods.addAll(ext.modId, it.modId)
    }
}
ext.gametest?.let {
    ext.runs.register("gametest") {
        server()
        sourceSetName.set(it.sourceSetName)
        systemProperty("fabric-api.gametest", "true")   // nur Fabric, siehe Abschnitt 5 (loom { })
    }
}
```

Auch `systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")` aus deinem `FabricLoom.kt` ist loader-spezifisch. Das gehört in den Loom-Adapter als Default für jeden Run, nicht ins gemeinsame Modell.

---

## 7. Empfohlene Reihenfolge

1. `Run` mit den Properties aus 2.1 und `RunType` anlegen (`dsl/Run.kt`).
2. `RunApplier` plus `LoomRunApplier` (Loom ist bei dir schon eingebunden und hat bereits `client` / `server`).
3. Standard-Runs `client` und `server` automatisch registrieren, falls der Nutzer sie nicht selbst definiert. Damit ist das Verhalten auf Loom und MDG gleich.
4. `MdgRunApplier`.
5. `ForgeRunApplier`, sobald die offenen Fragen aus Abschnitt 8 geklärt sind.
6. Warnungen für nicht unterstützte Properties (Abschnitt 4).
7. Escape-Hatches (Abschnitt 5).

---

## 8. Offene Fragen

- **FG-Typen:** Welche Run-Namen erwartet die UserDev-JSON für `client` und `server`? Müsste man im Quellcode von `SlimeLauncherOptionsImpl.inherit` oder an einem echten Projekt prüfen.
- **Task-Name bei FG:** Wie heißt der `JavaExec`-Task, den FG erzeugt? Wichtig für `dependsOn`.
- **Mods pro Run:** Loom ist global, MDG und FG pro Run. Reicht `loadedMods` als Liste von Mod-IDs, oder brauchst du pro Run andere Source-Sets?
- **Default-Verzeichnis:** Einheitlich `run/` (wie Loom und MDG) oder `runs/<sourceSet>/<name>` (wie FG)?
- **`afterEvaluate`:** Lässt sich das mit deinem Aufbau vereinbaren, oder sollen die Runs über `configureEach` und `convention` lazy gebunden werden? Das Lazy-Modell ist sauberer, braucht aber pro Property eine eigene Convention-Strategie, damit Loom-Defaults nicht gelöscht werden.

---

## 9. Zusatz: Data-Runs (später)

Data-Runs sind nicht Teil der ersten Umsetzung. `RunType` hat vorerst nur `CLIENT` und `SERVER`. Wenn sie dazukommen, wird `RunType` um `DATA` erweitert, und die Adapter bekommen je einen Zweig:

| | Loom | MDG | FG |
|---|---|---|---|
| Native Unterstützung | **nein**, kein Data-Typ im Run-Container | `data()`, `clientData()`, `serverData()` | über Run-Name und UserDev-JSON **(unverifiziert)** |
| Umsetzung | Fabric-API-Datagen (`fabricApi.configureDataGeneration`) oder eigene Server-Run-Config mit Datagen-Argumenten | `model.data()` | Run mit Namen `data` **(unverifiziert)** |

Offene Punkte dazu:

- **Loom:** Fehler werfen, auf `server` mit eigenen Argumenten ausweichen oder an `fabricApi.configureDataGeneration` anbinden? Dein `Loader`-Enum hat schon `DATAGEN`, das spricht für ein eigenes Datagen-Projekt.
- **MDG:** Reicht ein `DATA`, oder braucht es `CLIENT_DATA` und `SERVER_DATA`?
- **FG:** Welcher Run-Name gilt für Data in der UserDev-JSON?
- Das Convenience-API bekäme dann `fun data() = type.set(RunType.DATA)`.
