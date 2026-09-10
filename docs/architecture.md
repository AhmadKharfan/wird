# Architecture

Wird is a single Compose Multiplatform module, `:composeApp`, targeting Android and
wasmJs. Everything shared lives in `commonMain`; each target contributes only its
platform entry point and the pieces that genuinely cannot be shared.

## The three layers

```
composeApp/src/commonMain/kotlin/dev/ahmad/wird/
  domain/    model/ repository/ usecase/ util/
  data/      local/ remote/ mapper/ repository/
  ui/        theme/ components/ base/ navigation/ feature/
  di/        one Koin module per layer
```

The dependency rule points inward:

```
ui ──┐
     ├──> domain <── data
di ──┘
```

`ui` and `data` may depend on `domain`. `domain` depends on nothing but the Kotlin
standard library, kotlinx-datetime and kotlinx-coroutines. `ui` never touches `data`
directly — it goes through a use case, which goes through a repository interface that
`data` implements.

### domain

Pure Kotlin. Data classes with no annotations, repository **interfaces** only, one class
per use case exposing a single `operator fun invoke`, and pure helpers.

No Compose, no Room, no Koin, no Supabase, no Android. This is what makes the layer
portable, trivially testable, and stable while everything around it changes.

### data

Room entities, DAOs and the database; mappers; and the repository **implementations** of
the domain interfaces. `remote/` is reserved for Supabase and is currently empty.

Two rules hold the boundary: domain models never carry Room annotations, and entities
never leave `data/` — a mapper converts at the edge. Without this, a storage change
becomes a change to every layer.

Every write is local-first: a repository write completes against local storage and
returns. No UI interaction ever awaits the network.

### ui

`theme/` holds the design tokens — colours, spacing, radii, sizes. Features consume
tokens and never hardcode a colour, size, radius or spacing.

`base/` holds the shared ViewModel machinery. `components/` holds reusable stateless
composables. `navigation/` owns the `NavController`; screens receive lambdas rather than
the controller itself. `feature/` holds one package per screen.

## Enforcement

The dependency rule is checked mechanically, not by review.

`:composeApp:checkDomainPurity` scans every `.kt` file under `domain/` and fails the
build on:

- an import that does not sit under an allowed prefix, and
- a forbidden fully-qualified reference written inline, which carries no import line.

The rule is an **allowlist**. A blocklist would silently pass every dependency nobody
thought to forbid.

The detector lives in `buildSrc` (`dev.ahmad.wird.gradle.DomainPurity`) so it is unit
tested like any other code — see `buildSrc/src/test/`. The Gradle task is a thin wrapper
that feeds it files and formats the failure.

The task is wired to every Kotlin compilation, so it fails `assembleDebug` and
`wasmJsBrowserDistribution` as well as `check`. Widening the allowed prefix list is an
architecture decision, not a convenience.

```bash
./gradlew :composeApp:checkDomainPurity
```

## Presentation pattern

One state object per screen, exposed as `StateFlow`; one-shot events exposed as a
Channel-backed `Flow` so they are delivered exactly once and never replayed on
recreation. A feature declares one interaction listener interface, and its ViewModel
implements it, so a screen takes a single listener rather than a drift of lambdas.

Each screen splits in two: a stateful composable that resolves the ViewModel and collects
state, and a private stateless `Content` composable that takes state plus the listener.
The stateless half is previewable and testable on its own.

See CLAUDE.md for the exact file-per-feature layout.
