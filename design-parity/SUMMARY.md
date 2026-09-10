# Design parity — summary

**No screen has a parity verdict yet, so nothing here is green.** A parity report compares a
built screen with its design board. The boards could not be reached: the design-system tool
needs `/design-login`, which cannot run in a non-interactive session, and every board link
received so far ended in a literal `<... filename>` placeholder. With no board there is
nothing to compare a screen against, and a verdict written without one would be invented.

## Screens

| Screen | Built | Board | Verdict |
| --- | --- | --- | --- |
| Today | provisional placeholder on the real data layer | not reachable | not assessed |
| History | no — empty destination; the aggregations are built and tested in `domain/` | not reachable | not assessed |
| Settings | no — empty destination; habit management, privacy, theme, numerals and export are built in `domain/` | not reachable | not assessed |
| Onboarding | no — the use cases (routine choice, onboarding needed) are built in `domain/` | not reachable | not assessed |
| Circles | no — the leaderboard and a seeded fake repository are built | not reachable | not assessed |
| Badges | no — calculated in `domain/` from local history | not reachable | not assessed |

## What was checked without the board

These are the design rules that do not need a board to judge. They were checked on the only
UI that exists — the app shell, the bottom bar, the list row and the provisional Today —
on an Android emulator (API 34, 1080×2400) and in the wasm build in a browser.

| Check | Result | Evidence |
| --- | --- | --- |
| Colour, radius, spacing or type size written outside `ui/theme` | none | every `.dp`, `.sp`, `Color(` and hex literal under `ui/` sits in `ui/theme` |
| Theme roles left at library defaults | **found and fixed** (#54) | the bottom bar painted Material's baseline lavender — container `#F3EDF7`, indicator `#E8DEF8`, selected label `#625B71`, sampled from screen pixels — on a teal and sand palette. It now paints project tokens only; the snackbar's inverse roles are mapped to existing tokens too |
| Raw colour values inside the theme | **fixed** (#54) | the dark surface and white were written inline in the schemes; both are named tokens now |
| A component drawn differently in two features | not applicable | only one feature exists |
| RTL: chevrons, back arrows, progress direction, chart axes | clean | direction is forced RTL in `WirdTheme`; names sit at the start and values at the end; "0 / 15" reads right to left as "0 of 15". No chevrons, back arrows, progress bars or charts exist yet |
| English strings, emoji as icons, lorem text | none | every UI-reachable literal is Arabic |
| Touch targets under 48dp | none | list rows measure 48dp on the emulator (enforced by `WirdListRow`); bottom-bar items are Material's 80dp |
| Window insets | **found and fixed** (#55) | with edge-to-edge on — which Android 15 enforces at target SDK 37 — the nested Scaffolds padded the status bar twice: the score's text started at y=183 instead of y=121, one status-bar height (62px) too low. After the fix it starts at y=121 |
| List-row press feedback | **found and fixed** (#55) | mid long-press, the pixel outside the row's rounded corner turned from the background's `#f6f1e7` to `#e8e4dc` — the ripple was drawn square. After the fix it stays `#f6f1e7` |

### Text contrast, both themes

Measured with the WCAG formula on the token values, and on sampled screen pixels for the
bottom bar. Body text needs 4.5:1; every pair passes it, so large text (3:1) passes too.

| Pair | Light | Dark |
| --- | --- | --- |
| text on background | 14.62 | 15.58 |
| text on surface | 16.46 | 13.87 |
| primary (completed value, retry) on surface | 6.04 | 8.86 |
| primary on background | 5.37 | 9.96 |
| on-primary on primary | 6.04 | 9.96 |
| error on surface | 6.53 | 9.38 |

## Known, not yet fixable

- **The web build's Arabic font** arrives well after first paint; for the first half-minute or
  so after a reload, Arabic text draws as empty boxes. Bundling Readex Pro and Noto Sans Arabic
  is part of the theme phase, which waits on the board.
- **Every colour, spacing and radius token is a placeholder** until the board's values are
  extracted. The checks above prove the rules are held, not that the values are right.

## To turn this green

1. Run `/design-login` once from an interactive `claude` session on this machine.
2. Send each board's link with its real file name.
3. Each screen then gets `design-parity/<screen>.md`, and this table its verdicts.
