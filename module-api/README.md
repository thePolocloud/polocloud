# PoloCloud Module API

`module-api` is the SDK for writing **node modules**: plain jars that a node loads from
`local/modules/` to extend what it does — sync DNS records, expose a REST endpoint, bridge
to an external system, whatever. This README is for module authors.

Modules run in-process with the node, in their own classloader, and go through a managed
lifecycle (load → enable → disable → unload) driven by the node itself. If you want the
internals of how that loader works, see [`node`'s module system](../node/src/main/kotlin/de/polocloud/node/module).

## Contents

- [Quick start](#quick-start)
- [`module.yml` reference](#moduleyml-reference)
- [Lifecycle](#lifecycle)
- [`ModuleScope`: one instance per node, or one per cluster](#modulescope-one-instance-per-node-or-one-per-cluster)
- [Talking to the node](#talking-to-the-node)
- [Per-module config](#per-module-config)
- [Using the cluster SDK (events, groups, services)](#using-the-cluster-sdk-events-groups-services)
- [Declaring dependencies](#declaring-dependencies)
- [Building and installing](#building-and-installing)
- [Managing modules at runtime](#managing-modules-at-runtime)
- [Testing](#testing)

## Quick start

**1. Add the module as a Gradle subproject** (or a standalone project depending on the
published `module-api` artifact) and depend on it as `compileOnly` — at runtime the classes
are loaded from the node's own classpath, not bundled into your jar:

```kotlin
// build.gradle.kts
plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    compileOnly(projects.moduleApi) // or "de.polocloud:module-api:<version>" outside this repo
}

kotlin {
    jvmToolchain(25)
}
```

**2. Write your module class**, extending [`PolocloudModule`](src/main/kotlin/de/polocloud/moduleapi/PolocloudModule.kt):

```kotlin
package com.example.hello

import de.polocloud.moduleapi.PolocloudModule

class HelloModule : PolocloudModule() {

    override fun onEnable() {
        logger.info("Hello from node '{}'!", node.nodeName)
    }

    override fun onDisable() {
        logger.info("Goodbye!")
    }
}
```

A subclass **must have a public no-arg constructor** — the loader instantiates it via
reflection before `descriptor`/`node` even exist, then wires them in.

**3. Add a `module.yml`** at the root of your jar's resources (`src/main/resources/module.yml`):

```yaml
name: hello
version: 1.0.0
main: com.example.hello.HelloModule
description: Says hello on startup
authors: [ your-name ]
```

**4. Build the jar and drop it into a node's `local/modules/` folder.** It's picked up
immediately if the node is running (see [Managing modules at runtime](#managing-modules-at-runtime)), or on next start otherwise.

## `module.yml` reference

Read from the root of your jar by [`ModuleDescriptor`](src/main/kotlin/de/polocloud/moduleapi/ModuleDescriptor.kt):

```yaml
name: cloudflare                                     # required, unique across all loaded modules
version: 1.0.0                                        # required, free-form
main: de.polocloud.modules.cloudflare.CloudflareModule # required, your PolocloudModule subclass
description: Keeps a Cloudflare DNS record in sync with this cluster's online proxies
authors: [ your-name ]
scope: SINGLE_ACTIVE                                   # EVERY_NODE (default) or SINGLE_ACTIVE
api-version: 3.0.0                                     # minimum polocloud version this module needs
depends: []                                            # hard requirements — load fails if missing
soft-depends: []                                       # load-order hints only, missing ones are ignored
dependencies:                                          # extra Maven coordinates, resolved at load time
  - com.squareup.okhttp3:okhttp:4.12.0
```

| Field | Required | Meaning |
|---|---|---|
| `name` | yes | Unique module id. Also the name of its data folder — keep it filesystem-safe. |
| `version` | yes | Free-form version string, shown in `module list`. |
| `main` | yes | Fully-qualified name of your `PolocloudModule` subclass. |
| `description`, `authors` | no | Informational only. |
| `scope` | no | See [`ModuleScope`](#modulescope-one-instance-per-node-or-one-per-cluster). Defaults to `EVERY_NODE`. |
| `api-version` | no | `major.minor.patch` you built against. The running node's major version must match exactly, and its minor/patch must be at least this. Skips the check entirely if omitted. |
| `depends` | no | Other module names that must already be loaded, or this module's load fails. |
| `soft-depends` | no | Other module names to load first *if present* — missing ones are silently ignored. |
| `dependencies` | no | `group:artifact:version` Maven coordinates resolved against Maven Central and added to your module's classloader before it loads. No transitive resolution — list exactly what you import directly. |

## Lifecycle

```
onLoad() → onEnable() → (running) → onDisable()
```

- **`onLoad()`** — called once, right after your module is instantiated and attached, before
  *any* module in this batch is enabled. Register state that other modules' `onEnable()`
  might depend on here (e.g. reading your config), but don't start doing real work yet.
- **`onEnable()`** — called once this module (and, for `SINGLE_ACTIVE` modules, this node) is
  actually active. Start listeners, schedulers, network clients here.
- **`onDisable()`** — called on node shutdown, on a `reload`, or when a `SINGLE_ACTIVE`
  module's node loses cluster head status. Release exactly what `onEnable()` acquired
  (unsubscribe listeners, shut down executors, close clients) — `onEnable()` may be called
  again later on the same instance without a fresh `onLoad()` in between.

Every hook runs behind a timeout on the node side — a hook that blocks forever (an
un-timed-out network call, for example) gets abandoned rather than hanging the node.
Don't rely on it running to completion if it's still executing when the timeout hits;
design `onDisable()` to be safe to abandon mid-way (e.g. `ExecutorService.shutdownNow()`,
not a blocking drain).

```kotlin
class HelloModule : PolocloudModule() {

    private var scheduler: ScheduledExecutorService? = null

    override fun onEnable() {
        val executor = Executors.newSingleThreadScheduledExecutor {
            Thread(it, "hello-module-tick").apply { isDaemon = true }
        }
        executor.scheduleWithFixedDelay({ logger.info("tick") }, 0, 30, TimeUnit.SECONDS)
        scheduler = executor
    }

    override fun onDisable() {
        scheduler?.shutdownNow()
        scheduler = null
    }
}
```

## `ModuleScope`: one instance per node, or one per cluster

Set via `scope:` in `module.yml`:

- **`EVERY_NODE`** (default) — every node that has the jar loaded runs its own independent
  instance. Use this for anything stateless per-node (e.g. a module exposing that node's own
  metrics on a local port).
- **`SINGLE_ACTIVE`** — only one instance is ever *enabled* across the whole cluster at a
  time, even if the jar sits in `local/modules/` on every node. `onEnable()` only runs on
  the current cluster head; every other node's instance stays loaded but disabled
  (standby). If the head changes, the old head is disabled and the new head enabled
  automatically — no manual pinning needed. Use this for anything that would conflict with
  itself if run twice (e.g. registering this cluster's proxies with an external DNS/LB
  provider — exactly what the [`cloudflare-module`](../modules/cloudflare-module) example does).

Head status can still change *while* a `SINGLE_ACTIVE` module is enabled (right between
`onEnable()` and some later action), so re-check before anything that would conflict if two
nodes did it simultaneously:

```kotlin
override fun onEnable() {
    if (!node.isHead()) return // belt-and-braces; the loader already gates this for you
    // ...
}

private fun onSomeEvent() {
    if (!node.isHead()) return // but re-check here — head status can drift after onEnable()
    registerWithExternalService()
}
```

## Talking to the node

[`ModuleNode`](src/main/kotlin/de/polocloud/moduleapi/ModuleNode.kt) (available as `node`
inside your module once attached) exposes facts specific to *this* node:

```kotlin
node.nodeId     // this node's cluster-unique UUID
node.nodeName   // this node's configured name
node.dataFolder // local/modules/data/<your module name>/ — yours to read/write freely
node.isHead()   // whether this node is currently the elected cluster head
```

`PolocloudModule` also gives you a ready-made per-module logger and a shortcut to the data
folder:

```kotlin
logger.info("Loaded on node '{}'", node.nodeName) // logs as "module/<your module name>"
dataFolder                                         // same as node.dataFolder
```

Cluster-wide operations (groups, services, cross-node events) aren't part of `ModuleNode` —
`module-api` depends on `api`, so reach those the same way any other SDK consumer does, via
[`de.polocloud.api.Polocloud`](#using-the-cluster-sdk-events-groups-services).

## Per-module config

[`ModuleConfig<T>`](src/main/kotlin/de/polocloud/moduleapi/config/ModuleConfig.kt) gives
you a hand-editable YAML file (`config.yml`) in your module's data folder. `T` needs a
public no-arg constructor and mutable (`var`) properties — a Kotlin data class where every
property has a default satisfies both:

```kotlin
data class HelloConfig(
    var greeting: String = "Hello",
    var intervalSeconds: Long = 30,
)

class HelloModule : PolocloudModule() {

    private lateinit var config: ModuleConfig<HelloConfig>

    override fun onLoad() {
        // written to disk with its defaults the first time this runs
        config = node.config { HelloConfig() }
    }

    override fun onEnable() {
        logger.info(config.value.greeting)
    }
}
```

```kotlin
config.value              // current in-memory value
config.reload()            // re-read config.yml from disk, discarding unsaved in-memory changes
config.save(newValue)      // write newValue to config.yml and keep it as the current value
```

## Using the cluster SDK (events, groups, services)

For anything cluster-wide, use [`de.polocloud.api.Polocloud`](../api) directly — it's
always on the classpath. The [`cloudflare-module`](../modules/cloudflare-module) is a full
worked example (subscribes to service events, reconciles state on a schedule, respects
`isHead()`); the essentials:

```kotlin
import de.polocloud.api.Polocloud
import de.polocloud.shared.event.server.ServiceOnlineEvent
import de.polocloud.shared.event.server.ServerStoppedEvent
import java.util.function.Consumer

class HelloModule : PolocloudModule() {

    private val onlineListener = Consumer<ServiceOnlineEvent> { event ->
        logger.info("'{}' just came online", event.service.name())
    }

    override fun onEnable() {
        Polocloud.eventService.subscribe(ServiceOnlineEvent::class.java, onlineListener)
    }

    override fun onDisable() {
        // always unsubscribe what onEnable() subscribed — the instance may be re-enabled later
        Polocloud.eventService.unsubscribe(ServiceOnlineEvent::class.java, onlineListener)
    }
}
```

`Polocloud.groupService` and `Polocloud.serviceService` give you read/query access to groups
and running services, e.g. `Polocloud.serviceService.findByState(ServiceState.RUNNING)`.

## Declaring dependencies

**On another module** — use `depends`/`soft-depends` in `module.yml` (see the [reference
table](#moduleyml-reference) above) to control load order and require another module to be
present. Modules load in dependency order within a single load pass; a module involved in a
circular `depends`/`soft-depends` chain fails to load rather than loading in an unspecified
order.

**On a third-party library** — list it under `dependencies:` in `module.yml` instead of
shading it into your jar:

```yaml
dependencies:
  - com.squareup.okhttp3:okhttp:4.12.0
```

It's resolved against Maven Central and added to your module's own classloader before
`onLoad()` runs. There's no transitive resolution, so list every coordinate you import
directly yourself.

**On `module-api`/`api`/Kotlin itself** — don't declare these here and don't shade them
either. The node's classloader delegates `de.polocloud.moduleapi.*`, `de.polocloud.api.*`,
`de.polocloud.shared.*`, `de.polocloud.proto.*`, `de.polocloud.common.*`, `kotlin.*`,
`kotlinx.*`, `org.slf4j.*` and `java.*`/`javax.*` straight through to the node's own
classpath — that's exactly why your `build.gradle.kts` declares them `compileOnly`, not
`implementation`. Shading them in yourself would just create duplicate, incompatible class
definitions at runtime.

## Building and installing

A module is a plain jar — deliberately not shaded (see above). Build it and drop the output
straight into a node's `local/modules/`:

```sh
./gradlew :your-module:jar
cp your-module/build/libs/your-module.jar <node-folder>/local/modules/
```

## Managing modules at runtime

From the node terminal (or CLI):

```
module               # list every loaded module and any load failures
module reload         # unload and reload every module from local/modules/
module reload <name>  # unload and reload just one module
module enable <name>  # enable an already-loaded module in place
module disable <name> # disable an already-loaded module in place, without unloading it
module cluster        # show which node runs each module, cluster-wide
```

Dropping a new jar into `local/modules/`, replacing one, or deleting one is picked up
automatically while the node is running — no `reload` needed.

## Testing

Depend on `module-api` as `testImplementation` and test your module's logic directly —
there's no need for a real jar/classloader/node to unit-test business logic that doesn't
touch `PolocloudModule` lifecycle plumbing itself:

```kotlin
// build.gradle.kts
dependencies {
    testImplementation(projects.moduleApi)
    testImplementation(libs.bundles.testing) // JUnit 5, or your framework of choice
}

tasks.test {
    useJUnitPlatform()
}
```

See [`CloudflareSyncTest`](../modules/cloudflare-module/src/test/kotlin) for an example of
testing a module's reconciliation logic in isolation from the loader.
