# PoloCloud

## Versioning

PoloCloud versions look like `3.1.0` (stable) or `3.0.7-dev.42` (pre-release) — `major.minor.patch[-channel.build]`.

**Channels**, from least to most stable: `SNAPSHOT` (local dev builds) → `DEV` (CI builds from every push to `master`, tagged `vX.Y.Z-dev.<run>` and published as GitHub pre-releases) → `ALPHA` → `BETA` → `RELEASE` (stable, tagged `vX.Y.Z`).

The channel is the single source of truth for stability — it's parsed from the version string itself, not from GitHub's `draft`/`prerelease` flags, which describe how a release was published rather than how stable it is.

Implementation:
- [`PolocloudVersion`](common/src/main/kotlin/de/polocloud/common/version/PolocloudVersion.kt) / [`PolocloudReleaseChannel`](common/src/main/kotlin/de/polocloud/common/version/PolocloudReleaseChannel.kt) — the version model
- [`PolocloudVersionParser`](common/src/main/kotlin/de/polocloud/common/version/PolocloudVersionParser.kt) — parses version strings into a `PolocloudVersion`
- [`gradle/version.gradle.kts`](gradle/version.gradle.kts) — injects the version into each module's `version.properties` at build time, from `-Ppolocloud.version.*` Gradle properties (see [`gradle.properties`](gradle.properties) for local defaults)

## Update checker

On boot, the node checks GitHub for a newer release via [`UpdateChecker`](updater/src/main/kotlin/de/polocloud/updater/UpdateChecker.kt) (in the `updater` module) and logs the result. It never blocks or affects startup — the check runs on a background thread and any failure (offline, rate-limited, ...) is swallowed.

It only ever offers an update whose channel is **at least as stable as the one currently running** — a node on `RELEASE` is never nagged about a `DEV` rolling build, while a node already on `DEV` is offered `DEV`-or-better updates. Among eligible releases it picks the highest version, independent of GitHub's listing order.