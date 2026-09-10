# Phase zero: toolchain findings

A throwaway spike ran before any project code was written, to answer one question: can a
Kotlin Multiplatform project with `androidTarget()` and `wasmJs { browser() }` resolve
Compose Multiplatform, Room 3.x and adhan for **both** targets, and actually run them?

The answer is yes, with four constraints that shaped the build and are not optional.

Every version below was checked against live repository metadata rather than guessed.

## What resolves for wasmJs

| Library | Verdict | Notes |
| --- | --- | --- |
| Compose Multiplatform 1.12.0 | ✅ | The `compose.runtime`-style DSL accessors are deprecated **to error**; artifacts must be declared explicitly |
| Compose material3 | ⚠️ `1.12.0-alpha03` | Ships on its own version stream. There is no stable 1.12.0 |
| Room 3.0.3 (`androidx.room3`) | ✅ | Gradle extension is `room3 { }`, not `room { }`. KSP runs for both targets |
| `androidx.sqlite` 2.7.1 | ✅ | `sqlite-bundled` for Android, `sqlite-web` for wasm |
| adhan (`com.batoulapps.adhan:adhan2:0.0.7`) | ✅ | Publishes `adhan2-wasm-js` |
| Koin 4.2.2 | ✅ | `koin-core`, `koin-compose`, `koin-compose-viewmodel` all publish wasm-js variants |
| kotlinx-coroutines, kotlinx-datetime, kotlinx-serialization | ✅ | — |
| **`androidx.navigation:navigation-compose`** | ❌ | **Publishes no wasm-js artifact at all.** Use `org.jetbrains.androidx.navigation` |
| `androidx.lifecycle` | ⚠️ | Google's has wasm-js only on `2.12.0-alpha*`. The JetBrains fork is stable at 2.11.0 and is what navigation depends on anyway |

Navigation and lifecycle therefore come from the JetBrains `org.jetbrains.androidx.*`
forks. That is forced, not preferred.

## Room 3 on wasmJs: yes, but bring your own SQLite

Room 3 supports web through `WebWorkerSQLiteDriver`. The catch is that
`androidx.sqlite:sqlite-web` ships **only the Kotlin driver** — no worker, no SQLite WASM
binary. Both have to be vendored. This project carries androidx's reference worker and
the official `@sqlite.org/sqlite-wasm` build under `wasmJsMain/resources`.

Three things had to be right before a single row persisted:

1. **The worker is an ES module.** It must be constructed with `type: "module"`, and
   `.mjs` must be served with a JavaScript MIME type or the browser refuses to execute it.
2. **The page must be cross-origin isolated** — `Cross-Origin-Opener-Policy: same-origin`
   and `Cross-Origin-Embedder-Policy: require-corp` — or OPFS is unavailable.
3. **The default `OpfsDb` path is a trap.** It relies on a *nested* async-proxy worker.
   Where that is blocked, sqlite-wasm logs `Ignoring inability to install OPFS
   sqlite3_vfs` and falls through to an in-memory VFS — and `new sqlite3.oo1.OpfsDb(...)`
   still succeeds. Writes appear to work, the UI updates through Room's `Flow`, and
   everything is lost on reload.

That last one was not theoretical; it happened during the spike and was only caught by
reloading the page and finding the data gone. `sqlite3_vfs_find("opfs")` returned false
while the registered VFS list showed only `unix` and `memdb`.

The fix is `installOpfsSAHPoolVfs()`, which uses sync access handles directly in the
worker and needs no nested worker. The vendored worker resolves a durable VFS once at
startup and **refuses to open a database** if none is available. For a local-first
product, failing loudly beats losing writes silently.

Verified end to end: recording a prayer creates `.wird-opfs-pool/` in OPFS with real
bytes, and the record survives a full page reload.

## The version squeeze

These four facts interlock, and together they decide the module layout:

1. Compose Multiplatform 1.12.0's Android artifacts require **compileSdk 37**.
2. compileSdk 37 requires **AGP 9** — AGP 8.x cannot target it.
3. AGP 8.x is **incompatible with Gradle 9.6+**.
4. AGP 9 **refuses `com.android.application` in the same module as the Kotlin
   Multiplatform plugin**.

So the single-module `composeApp` layout survives only via the documented bypass:
`android.builtInKotlin=false` and `android.newDsl=false`, plus a file-level deprecation
suppression in the module build script. When that bypass goes away, the migration is to
split into a KMP module on `com.android.kotlin.multiplatform.library` plus a thin Android
app module.

## Bundle size

Production wasmJs distribution, excluding the 1.42 MiB source map, which is not served:

| File | Raw (bytes) | Gzipped (bytes) |
| --- | ---: | ---: |
| Skiko renderer `.wasm` | 8,640,316 | 3,328,940 |
| App `.wasm` | 3,698,341 | 1,174,521 |
| `sqlite3.wasm` | 856,028 | 391,621 |
| `wird.js` | 529,955 | 99,589 |
| `sqlite3.mjs` | 483,051 | 84,067 |
| `sqlite3-opfs-async-proxy.js` | 21,742 | 5,810 |
| `sqlite-worker.js` | 7,288 | 2,119 |
| `index.html` + licence | 1,672 | 576 |
| **Total** | **14,238,393** | **5,087,243** |

That is **13.58 MiB raw, 4.85 MiB gzipped**.

The Skiko renderer alone is 65% of the gzipped payload and is a fixed cost of Compose on
the web — it does not grow with the app. SQLite adds roughly 480 KB gzipped. Serve
everything gzipped or brotli; the raw figure is not a realistic download.

Android debug APK: 19,758,202 bytes.

## Build times

Measured on this machine, clean tree, no build cache, warm dependency cache.

| Build | Time |
| --- | --- |
| `:composeApp:assembleDebug` (cold) | 27 s |
| `:composeApp:wasmJsBrowserDistribution` (cold) | 100 s |
| Both, no-op incremental | 3 s |

The web target is the slow one, roughly four times Android, mostly in the Kotlin/Wasm
compile and webpack steps. Incremental builds are fast enough not to matter.

## Skills inventory

Skills are installed at `C:\Users\Ahmad\.agents\skills\` — the shared agent-skills
location, not `~/.claude/skills`, so they are not auto-loaded and must be read from disk.

| Skill | What it does |
| --- | --- |
| `codex-delegate` | Brief → dispatch → poll → review → commit loop against the Codex CLI. Exposes no fixed model list; `--model` and `--effort` pass through. The relay never commits |
| `compose-multiplatform-patterns` | Compose state, navigation, slot APIs, recomposition performance, theming |
| `find-skills` | Discover and install skills from the open ecosystem |

**Not installed:** there is no Kotlin Multiplatform skill, no clean-code review skill and
no test discipline skill. Where CLAUDE.md says "run the clean-code and test skills," that
gate is currently unbacked and must be installed or done by hand.

One deviation worth recording: the Compose Multiplatform skill's theming example uses
`dynamicColorScheme` with `Build.VERSION` and `LocalContext`. That is Android-only and
cannot compile in `commonMain`, so `WirdTheme` uses fixed token schemes instead.
