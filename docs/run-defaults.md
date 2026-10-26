# Gemeinsame Werte im `runs { }`-Block

Frage: Kann man im `runs { }`-Block auch einzelne Werte (zum Beispiel Booleans wie `devLogin`) setzen, die für alle Runs gelten?

**Kurzantwort:** Ja. Es gibt zwei Wege. Ein selbst gebauter `runs`-Block mit Defaults ist sauberer und robuster als `configureEach`.

Das Verhalten von `configureEach` habe ich mit einem kleinen Gradle-Test (Gradle 9.8.1, Groovy, außerhalb des Projekts) nachgeprüft. Die Snippets unten habe ich nicht kompiliert.

---

## 1. Weg A: `configureEach` (ohne eigenen Code)

`runs` ist ein `NamedDomainObjectContainer<Run>`. Der Nutzer kann schon heute schreiben:

```kotlin
multiloader {
    runs {
        configureEach { devLogin.set(true) }
        register("client") { client() }
        register("server") { server() }
    }
}
```

Das funktioniert, hat aber eine Falle: **Die Reihenfolge entspricht der Reihenfolge, in der die Actions registriert wurden.** Mein Test:

```
configureEach (vor register)  -> läuft zuerst, flag=false, setzt true
register-Action               -> läuft danach, überschreibt den Wert
configureEach (nach register) -> läuft zuletzt und überschreibt wieder
```

Folgen:

- Ein `configureEach` **vor** dem `register` wirkt wie ein Default. Der Run kann ihn überschreiben.
- Ein `configureEach` **nach** dem `register` überschreibt dagegen den Wert des einzelnen Runs.
- Der Nutzer muss die Reihenfolge selbst kennen. Und es ist kein „Default“ im Sinne von Gradle (`convention`), sondern ein normales `set`. Ein späteres `unset` oder ein `convention` auf dem Run ist nicht mehr möglich.

---

## 2. Weg B: eigener `runs`-Block mit Defaults (empfohlen)

Der Block bekommt eigene Properties, und jeder neu angelegte `Run` verknüpft sich per `convention` damit:

```kotlin
multiloader {
    runs {
        devLogin = true                 // Default für alle Runs
        jvmArgs("-Xmx4G")               // siehe Abschnitt 3, Listen

        register("client") { client() }
        register("server") {
            server()
            devLogin = false            // einzelner Run überschreibt den Default
        }
    }
}
```

### Umsetzung

```kotlin
/** Werte, die für alle Runs gelten, solange ein Run sie nicht selbst setzt. */
abstract class RunDefaults {
    abstract val devLogin: Property<Boolean>
    abstract val jvmArgs: ListProperty<String>
    abstract val programArgs: ListProperty<String>
    abstract val systemProperties: MapProperty<String, String>
    abstract val environment: MapProperty<String, String>
    abstract val maxHeap: Property<String>
    abstract val generateIdeConfig: Property<Boolean>
}
```

Der Block selbst ist ein Objekt, das den Container und die Defaults hält:

```kotlin
abstract class RunsBlock @Inject constructor(objects: ObjectFactory) : RunDefaults() {
    val container: NamedDomainObjectContainer<Run> = objects.domainObjectContainer(Run::class.java) { name ->
        objects.newInstance(Run::class.java, name).also { run ->
            run.devLogin.convention(devLogin)
            run.generateIdeConfig.convention(generateIdeConfig)
            run.maxHeap.convention(maxHeap)
            // Listen und Maps siehe Abschnitt 3
        }
    }

    fun register(name: String, action: Action<Run>) = container.register(name, action)
    fun named(name: String) = container.named(name)
    fun configureEach(action: Action<Run>) = container.configureEach(action)
}
```

In `MultiloaderExtension`:

```kotlin
@get:Nested abstract val runs: RunsBlock
fun runs(action: Action<RunsBlock>) = action.execute(runs)
```

Hinweise:

- Der Container wird **nicht** mehr direkt als `NamedDomainObjectContainer` ausgeliefert. Wenn du die Gradle-eigene Syntax `runs { create("x") { } }` weiter möchtest, musst du die Methoden `create`, `register`, `named`, `getByName`, … durchreichen. Das Minimum ist `register`. Ob du das komplette Interface `NamedDomainObjectContainer<Run>` per Kotlin-Delegation implementieren willst, hängt davon ab, wie viel der Container-API der Nutzer braucht.
- Die Fabrik des Containers muss die Instanz über `ObjectFactory.newInstance` erzeugen, damit `@Inject` und die abstrakten Properties funktionieren. Das ist dasselbe Muster wie bei `Testmod` und `Gametest` in deinem Projekt.
- Ob `abstract val runs: RunsBlock` mit `@Nested` in einer als Extension registrierten Klasse sauber instanziiert wird, ist **(unverified)**. Als Fallback in `MultiloaderExtension` ein `val runs = objects.newInstance(RunsBlock::class.java)`.

### Warum `convention` statt `set`?

- Der Wert des einzelnen Runs hat immer Vorrang, egal in welcher Reihenfolge der Nutzer schreibt.
- Der Default wird lazy gelesen. Wer `devLogin = true` erst **nach** dem `register` setzt, wirkt trotzdem auf alle Runs.
- Der Wert eines einzelnen Runs bleibt ein normales `set`, und `unset` fällt auf den Default zurück.

---

## 3. Listen und Maps

`convention` auf einem `ListProperty` oder `MapProperty` **ersetzt** den Wert, wenn der Run selbst etwas setzt. Das ist bei Argumenten selten gewollt: Wer `jvmArgs("-Xmx4G")` als Default setzt und im Run `jvmArg("-Dfoo")` ergänzt, erwartet beides.

Zwei Möglichkeiten:

1. **Im Adapter zusammenführen** (empfohlen): Beim Anwenden des Runs erst die Defaults, dann die Werte des Runs übernehmen:

   ```kotlin
   cfg.jvmArguments.addAll(defaults.jvmArgs)
   cfg.jvmArguments.addAll(run.jvmArgs)
   ```

   Der Run selbst enthält dann nur seine eigenen Werte. Der Nachteil: Ein Run kann den Default nicht gezielt entfernen.

2. **Per `convention`** (Ersetzen-Semantik) für Booleans, Strings und Einzelwerte. Hier ist das Ersetzen korrekt.

Faustregel:

| Typ | Verhalten | Mechanismus |
|---|---|---|
| `Property<Boolean>`, `Property<String>` (`devLogin`, `maxHeap`, `generateIdeConfig`) | Run überschreibt Default | `convention` |
| `ListProperty` (`jvmArgs`, `programArgs`) | Defaults + Run | Zusammenführen im Adapter |
| `MapProperty` (`systemProperties`, `environment`) | Defaults + Run, Run gewinnt bei gleichem Key | Zusammenführen im Adapter (erst Defaults, dann Run) |

---

## 4. Defaults pro Typ (Client/Server)

Manche Werte gelten nur für Clients (`devLogin`) oder nur für Server. Zwei Optionen:

- `devLogin` ignoriert der Adapter ohnehin für `RunType.SERVER` (siehe `docs/devlogin.md`), der globale Default `devLogin = true` ist also unkritisch.
- Wer wirklich getrennte Defaults braucht, ergänzt in `RunsBlock` zwei Unterblöcke: `clients { }` und `servers { }` mit je einer `RunDefaults`-Instanz, und die Fabrik setzt die Convention je nach `run.type`. Der Typ steht aber erst nach `client()`/`server()` fest, also erst nach der Fabrik. Das macht die Verkabelung komplizierter, deshalb nur bei echtem Bedarf **(unverified)**.

---

## 5. Zusammenspiel mit `multiloaderSettings { }`

Du hast bereits die Hierarchie Root (`multiloaderSettings`) → Projekt (`multiloader`). Dieselbe Kette passt für Run-Defaults:

```
Run  ←convention←  multiloader.runs (Defaults)  ←convention←  multiloaderSettings.runs (Defaults)
```

Dann kann `devLogin = true` einmal im Root-Projekt stehen und gilt in allen Loader-Projekten. Das ist derselbe Mechanismus wie bei `minecraftVersion` in `MultiloaderSettingsExtension`. Wie die Verknüpfung zwischen den beiden Extensions bei dir aktuell läuft, habe ich hier nicht geprüft.

---

## 6. Empfehlung

1. `RunDefaults` mit den wenigen sinnvollen Einzelwerten anlegen (`devLogin`, `generateIdeConfig`, `maxHeap`, vielleicht `jvmArgs` und `systemProperties`).
2. Booleans und Strings per `convention` an den `Run` binden, Listen und Maps im Adapter zusammenführen.
3. `configureEach` bleibt trotzdem verfügbar, und die Reihenfolge-Falle aus Abschnitt 1 gehört in die Plugin-Doku.
4. Die Root-Ebene (`multiloaderSettings`) kann später folgen.
