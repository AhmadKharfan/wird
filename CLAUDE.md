# CLAUDE.md — Wird

Operating contract for this project. Read before writing any code.

Wird is a Compose Multiplatform app targeting **Android (minSdk 26)** and **wasmJs
(browser)**. Package `dev.ahmad.wird`.

---

## 1. Roles and delegation

**Claude is the lead architect and the sole reviewer.** Codex is an implementer that
receives bounded, fully-specified tasks and returns a diff. Codex never has the last word,
and never commits.

### NEVER DELEGATED

- Architecture and layering decisions.
- **Contract files** — `UiState`, `Effect`, `InteractionListener` signatures, repository
  interfaces. They define the shape everything else is written against.
- Extracting design tokens from Claude Design.
- The final review of any returned code.

### DELEGATED TO GPT VIA `codex-delegate`

- Mechanical implementation against a contract already written.
- Stateless composables whose spec is fully given.
- DAOs and mappers.
- Test bodies for a signature and cases already specified.
- Second-opinion analysis on a decision, where Claude weighs the answer and decides.

### EVERY DELEGATED TASK MUST CARRY

1. The exact file path.
2. The exact signatures it must satisfy.
3. The token names it may use.
4. The acceptance criteria.
5. The layering rules it must not violate.

**Never delegate "implement the Today screen."** If a brief cannot name the files and the
signatures, the task is not ready to delegate — it is still an architecture task.

### REVIEW

Review **every** returned diff against: the clean-code skill, the test skill, the layering
rules, and the design board. Re-run the gates yourself; never accept a self-report that
"gates passed." Reject with **specific** feedback and re-delegate to the same thread
(`--session <threadId>`).

**If the same task fails review twice, implement it yourself and note why in the commit
message.**

### DISAGREEMENT

When Claude and the delegate disagree, **Claude decides**, and states both positions in
one sentence each before proceeding.

### Invocation

```bash
node "/c/Users/Ahmad/.agents/skills/codex-delegate/scripts/relay.mjs" --brief brief.txt --cd /c/Users/Ahmad/AndroidStudioProjects/wird --timeout 2h
```

- `--read-only` — second opinions and review; the sandbox enforces no edits.
- `--session <id>` — rework in the same thread.
- `--model` / `--effort` — pass through to Codex; the skill fixes no model list.

Run it backgrounded, then read `<out-dir>/result.json` (`status`, `finalMessage`,
`touchedFiles`, `threadId`). Brief shape: `<task>` (including what to leave untouched),
`<verification_loop>` (the real gate commands below), `<action_safety>` (no unrelated
refactors; do NOT git add/commit), `<structured_output_contract>`. One task per brief;
queues run sequentially, one commit each. **The relay never commits — Claude does.**

---

## 2. Git and pull request rules

These are absolute.

### Commit messages

- **Conventional commits, entirely lowercase — including the subject after the colon.**
  Format: `type(scope): subject`, e.g. `feat(domain): add habit and entry models`.
- Types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `build`, `ci`.
- Imperative mood. No trailing period. Subject under 72 characters.
- **Never add a `Co-Authored-By` trailer. Never add "Generated with Claude Code", a
  session URL, or any other attribution line.** The body, when present, is plain prose
  explaining *why* — nothing else.

### Commit size

- One commit covers **one to three files that change together for one reason**.
- If a commit would touch more than three files, ask whether it should be split. The only
  exception is a generated set that is meaningless apart, such as the Gradle wrapper.
- **Every commit must leave the build in a working state** wherever that is possible.

### Pull requests

- Branch off `main`, one concern per branch.
- A PR should stay around **10 files or fewer**. If it would exceed that, or mix
  unrelated concerns, split it and say why.
- Fill in `.github/pull_request_template.md` properly — tick the boxes that genuinely
  pass, and explain the ones that do not. Never submit an empty template.
- Verify the branch builds **before** merging.
- Merge with `--rebase --delete-branch`. Do not open the next PR until the previous one
  is merged and `main` is green.

---

## 3. Layering — mechanically enforced

```
composeApp/src/commonMain/kotlin/dev/ahmad/wird/
  domain/
    model/       pure data classes
    repository/  repository INTERFACES only
    usecase/     one class per use case, single operator fun invoke
    util/        pure helpers
    -- imports allowed: kotlin stdlib, kotlinx-datetime, kotlinx-coroutines ONLY.
       NO Compose, NO Room, NO Koin, NO Supabase, NO Android.
  data/
    local/       Room entities, DAOs, database
    remote/      empty for now - Supabase lands in a later pack
    mapper/      entity <-> domain model
    repository/  repository IMPLEMENTATIONS of the domain interfaces
    -- domain models never carry Room annotations; entities never leave data/.
  ui/
    theme/       tokens
    components/  reusable stateless composables
    base/        BaseViewModel, BaseInteractionListener, EffectHandler
    navigation/  routes and the nav graph
    feature/     one package per feature
  di/            Koin modules, one per layer
```

`:composeApp:checkDomainPurity` scans every `.kt` under `domain/` and **fails the build**
on a forbidden import or a forbidden fully-qualified reference. It is wired to every
Kotlin compilation on both targets, so `assembleDebug` and `wasmJsBrowserDistribution`
both fail on a violation — it is not a matter of discipline. The allowed import prefixes
are declared in `composeApp/build.gradle.kts`; widening that list is an architecture
decision.

---

## 4. Feature file pattern

One package per feature under `ui/feature/<name>/`, exactly these files:

| File | Contains |
| --- | --- |
| `<Name>UiState.kt` | `@Immutable data class <Name>UiState`, any row/item state, and `sealed interface <Name>Effect` |
| `<Name>InteractionListener.kt` | `interface <Name>InteractionListener : BaseInteractionListener` — the screen's whole interaction surface |
| `<Name>ViewModel.kt` | `class <Name>ViewModel(...) : BaseViewModel<<Name>UiState, <Name>Effect>(...), <Name>InteractionListener` |
| `<Name>Screen.kt` | stateful `<Name>Screen()` (koinViewModel + collectAsStateWithLifecycle + EffectHandler) and a **private stateless** `<Name>Content(state, listener, ...)` |
| `<Name>Labels.kt` | Arabic display strings and formatting; the domain carries no display text |

Rules:

- The state object is one `@Immutable` data class, so the content composable stays skippable.
- Effects are one-shot and never part of state.
- The screen takes a **single listener**, not a drift of loose lambdas.
- The stateless `Content` never touches a ViewModel — it is previewable and testable alone.
- `NavController` stays in `ui/navigation/`; screens receive lambdas.
- ViewModels use `MutableStateFlow`, never `mutableStateOf`.

---

## 5. Product and code rules

- **Arabic RTL product.** `LayoutDirection.Rtl` is forced app-wide in `WirdTheme`, not
  inherited from the host locale. **No English in the UI.**
- **Every write is local-first.** No UI interaction ever awaits the network. Repository
  writes complete against local storage and return.
- **Never hardcode a color, size, radius or spacing in a feature** — use a token from
  `ui/theme/` (`Spacing`, `Radius`, `Sizes`, `WirdLightColors` / `WirdDarkColors`).
- **Minimum touch target 48dp** (`Sizes.minTouchTarget`).
- **Both targets build after every phase.**
- All versions live in `gradle/libs.versions.toml`. **No version literals in build files.**
- Run the clean-code and test skills before every commit.

### Gate commands

```bash
./gradlew :composeApp:checkDomainPurity
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:wasmJsBrowserDistribution
```

---

## 6. Toolchain constraints discovered by the spike

These are load-bearing. Do not "clean them up" without re-verifying both targets.

- **AGP 9 constraint.** AGP 9 refuses `com.android.application` in the same module as the
  Kotlin Multiplatform plugin. `gradle.properties` sets `android.builtInKotlin=false` and
  `android.newDsl=false` — the documented bypass — to keep the single-module `composeApp`
  layout. `composeApp/build.gradle.kts` carries a file-level `DEPRECATION` suppression for
  the same reason. **Migration trigger:** when AGP removes the bypass, split into a KMP
  module on `com.android.kotlin.multiplatform.library` plus a thin `:androidApp` on
  `com.android.application`.
- **compileSdk 37 is forced** by Compose Multiplatform 1.12.0's Android artifacts, which in
  turn forces AGP 9 — AGP 8.x cannot target 37 and is incompatible with Gradle 9.6+.
- **CMP's `compose.runtime`-style DSL accessors are deprecated-to-error** in 1.12.0. Every
  Compose artifact is declared explicitly in the version catalog.
- **CMP material3 is on its own version stream** — there is no stable 1.12.0; the catalog
  pins `1.12.0-alpha03`.
- **Navigation and lifecycle come from the JetBrains forks** (`org.jetbrains.androidx.*`).
  `androidx.navigation:navigation-compose` publishes **no** wasm-js artifact.
- **Room on wasmJs needs assets androidx does not ship.** `androidx.sqlite:sqlite-web`
  provides only the Kotlin driver — no worker, no SQLite WASM binary.
  `composeApp/src/wasmJsMain/resources/` carries androidx's reference `sqlite-worker.js`
  plus the official `@sqlite.org/sqlite-wasm` build in `sqlite3/`. The worker is an **ES
  module** and must be constructed with `type: "module"`.
- **The worker uses the OPFS SAH Pool VFS**, installed via `installOpfsSAHPoolVfs()`. The
  default `OpfsDb` path needs a *nested* async-proxy worker, which some embedded browsers
  block; when that fails, `OpfsDb` silently opens on a **non-persistent** VFS and every
  write is lost on reload. The worker therefore resolves a durable VFS once at init and
  **refuses to open a database** rather than degrade silently. Never restore the
  unconditional `new sqlite3.oo1.OpfsDb(...)`.
- **The web target must be served with COOP/COEP**
  (`Cross-Origin-Opener-Policy: same-origin`, `Cross-Origin-Embedder-Policy: require-corp`)
  or OPFS is unavailable. `.mjs` must be served with a JavaScript MIME type.

---

## 7. Skills — what is actually installed

At `C:\Users\Ahmad\.agents\skills\` (the shared agent-skills location, **not**
`~/.claude/skills`, so Claude Code does not auto-load them — read them from disk):

| Skill | Applies to |
| --- | --- |
| `codex-delegate` | All delegation |
| `compose-multiplatform-patterns` | The `ui/` layer |
| `find-skills` | Filling the gaps below |

**Not installed:** no Kotlin Multiplatform skill, no clean-code review skill, no test
discipline skill. Where section 4 says "run the clean-code and test skills," that gate is
currently **unbacked** and must be installed or done by hand.
