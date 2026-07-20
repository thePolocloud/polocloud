# PoloCloud

## Versioning

PoloCloud versions look like `3.1.0` (stable) or `3.0.7-dev.42` (pre-release) — `major.minor.patch[-channel.build]`.

**Channels**, from least to most stable: `SNAPSHOT` (local dev builds) → `DEV` (CI builds from every push to `master`, tagged `vX.Y.Z-dev.<run>` and published as GitHub pre-releases) → `ALPHA` → `BETA` → `RELEASE` (stable, tagged `vX.Y.Z`).

The channel is the single source of truth for stability — it's parsed from the version string itself, not from GitHub's `draft`/`prerelease` flags, which describe how a release was published rather than how stable it is.