# ورد — Wird

A Compose Multiplatform app for tracking the five daily prayers, built for Android and
the browser from one shared codebase.

Wird is an Arabic, right-to-left product. The UI is Arabic only, and layout direction is
forced RTL app-wide rather than inherited from the host locale, so the app looks the same
on an English phone as on an Arabic one.

## Targets

| Target | Minimum | Output |
| --- | --- | --- |
| Android | minSdk 26 | `composeApp/build/outputs/apk/debug/` |
| Web | wasmJs, browser | `composeApp/build/dist/wasmJs/productionExecutable/` |

## Build

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDistribution
```

Both targets must build after every change.

### Running the web build

The web target stores data in the Origin Private File System, which requires cross-origin
isolation. Serve the distribution with these response headers, or the database will not
open:

```
Cross-Origin-Opener-Policy: same-origin
Cross-Origin-Embedder-Policy: require-corp
```

`.mjs` files must be served with a JavaScript MIME type.

## Verification

```bash
./gradlew :composeApp:checkDomainPurity     # layering, fails the build on a violation
./gradlew :composeApp:testDebugUnitTest     # unit tests
```

## Architecture

Three layers, with the dependency rule pointing inward: `ui` and `data` may depend on
`domain`; `domain` depends on nothing but the Kotlin standard library, kotlinx-datetime
and kotlinx-coroutines. That rule is enforced mechanically by a Gradle task wired into
every Kotlin compilation, not by convention.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full layout and
[CLAUDE.md](CLAUDE.md) for the working rules, including the commit and pull request
conventions this repository follows.

## Stack

Kotlin Multiplatform · Compose Multiplatform · Room (KMP, with the OPFS SQLite driver on
web) · Koin · androidx lifecycle and navigation · kotlinx-datetime · kotlinx-serialization
· adhan for prayer time calculation.

Exact versions live in [`gradle/libs.versions.toml`](gradle/libs.versions.toml). There are
no version literals in build scripts.
