# RuneLite Plugin Development — Agent Guidelines

## RuneTongue: changing translations

Follow these steps whenever you add or fix a bundled translation. Background: `docs/CONTEXT.md` (3.3, 3.7) and `data/README.md`.

### Where the data is

- `data/<lang>/*.tsv` is the source of truth. The columns are `english, translation, category, sub_category, source`, in UTF-8, and every file in one language folder has the same header.
- `src/main/resources/com/runetongue/translations_<lang>.tsv.gz` is generated from it. Never edit the gz by hand, and never edit tsv without regenerating the gz.
- Files are read in name order, and a later line wins over an earlier one.
  - To fix a line, change it in place.
  - Put new game text in the file for its category: dialogue in `transcript_ko_dialogue.tsv`, names in `transcript_ko_name.tsv`, and so on.
  - Put UI text that the data never had in `zz_runetongue.tsv`.
- New text usually comes from the user's `~/.runelite/plugin-data/runetongue/missing/ko/missing.tsv`. Its `english` column is already normalized (`<Num0>`, `<colNum0>`, `[player name]`). Copy the line as it is, keep its category, sub_category and source, and fill in the translation. Never edit that file yourself; the plugin rewrites it.

### Rules each translation line must pass

The bundler drops a line that breaks these, and the game then shows English.

- Keep every number from the English as digits ("3 lots" becomes "3묶음", not "세 묶음"). When a number cannot be said naturally, write the digits anyway (`1백만`).
- Keep each `<colNumN>…</col>` pair, `<br>` where the English has it, `[player name]`, and template slots such as `<!PLAYER_NAME0>`.
- `[a/b]` choice groups need the same count and order in the translation, or a single translation without choices.
- One line is one dialogue box or widget text. Do not join or split lines. When the wiki split one sentence across two lines, keep each line's numbers in its own line.
- Style: follow `~/my_dev/runelingual-ko/translation/STYLE_GUIDE.md` and `glossary.tsv` when they are present.
  - Game names are written as `English(한국어)`.
  - Short UI (menu options, buttons, list screens) is Korean only.
  - Player lines use 해요체.
  - NPCs keep their own voice.

### After editing

1. Run `./gradlew bundleTranslations`.
2. Check `build/reports/translations/ko-skipped.tsv`. No new `PLACEHOLDER_MISMATCH` or `CHOICE_MISMATCH` lines may appear. The five `CHOICE_MISMATCH` lines already there are known.
3. Run `./gradlew test`. `BundledTranslationsTest` fails if the gz does not match `data/`.
4. Look up a changed line to confirm it, e.g. with a small program on `TextLookup.translate`. Tell the user which `::rh find <english>` to type in game to check it.

### Shipping

- Commit the tsv changes and the regenerated gz together, on a branch, never straight to `main`.
- Open a pull request on `AngryCatKR96/runetongue` with the `translation` label, for example `gh pr create --label translation`. When the user asks for a translation fix, committing, pushing the branch and opening this pull request are part of the task, so do not stop to ask. Never merge it yourself: the user merges, and merging is what updates the Plugin Hub.
- When that pull request is merged, `.github/workflows/plugin-hub.yml` tests the merged commit and points the Plugin Hub entry at it. It updates the open `runelite/plugin-hub` pull request, or opens one if none is open.
  - Never open or edit a `runelite/plugin-hub` pull request by hand, and never open a second one.
  - If a change reached `main` without the label, run `gh workflow run plugin-hub.yml --repo AngryCatKR96/runetongue`.
- Plugin Hub still reviews every update, so a fix reaches players only after that review. Say so when you report a fix.

## Logging

- Use `log.debug()` for developer/diagnostic logging.
- Do not use `log.info` for per-frame or per-event logging - RuneLite runs at INFO level in production, so high-frequency info logs will pollute user logs. `log.info()` is fine for one-time startup/shutdown messages or infrequent events.

## Threading & Concurrency

- Never use `Thread.sleep()`.
- Never block on `shutDown()` or `startUp()` — don't call `executor.awaitTermination()` in shutdown, just use `shutdownNow()`.
- Never do blocking network IO or disk IO on the client thread. The OkHttp thread pool can be used for blocking network requests.
  If you need to call back into `client` from the okhttp threadpool, such as from the response queued with `enqueue()`, use `clientThread.invoke()`
- Explicitly cancel scheduled tasks (e.g. `ScheduledFuture`) on shutdown, in addition to shutting down the executor.
- For batching async work, use `CompletableFuture.allOf()` — not `CountDownLatch`.
- If you must use `Process.waitFor()`, always pass a reasonable timeout.

## Performance

- Don't scan the entire scene every tick or frame. Use events such as object and npc (de)spawn to track what you care about and maintain your own collection.
- Keep the computations in Overlays, which are run each frame, to a minimum.

## API Usage

- Use `net.runelite.api.gameval` package constants — `ItemID`, `InterfaceID`, `ObjectID`, etc. Never hardcode magic numbers when gameval constants can be used instead.
- Use `LinkBrowser` to open URLs, not `java.awt.Desktop`
- When looking up Widgets, pass the component ID from gamevals (eg `client.getWidget(InterfaceID.DomEndLevelUi.LOOT_VALUE)`) - do not manually combine interface + component child IDs.
- Use of Java reflection is forbidden.

## HTTP & JSON

- Use OkHttp for all HTTP requests. `@Inject OkHttpClient` to get the HTTP client. Do not use `HttpURLConnection`, `java.net.http.HttpClient`, or Apache HttpClient.
- Use `@Inject Gson` to get a Gson instead, never create your own from scratch. You can use `.newBuilder()` to create one derived from the base `Gson.`
- Do not add transitive dependencies from `runelite-client` directly to `build.gradle`, such as gson, guice, or okhttp.
- Never execute okhttp calls on the client thread. Prefer using `enqueue()` which places the request on the okhttp threadpool.

## File I/O

- All file i/o must go through the Filepath utility. Use of Filepath requires setting PluginDescriptor `internalName` to the internal name of the plugin.
- Use `Filepath.Chooser` instead of `JFileChooser`
- To migrate a legacy plugin data folder from .runelite automatically, set PluginDescriptor `legacyDataDirectory` to the name of the legacy folder.

## Config

- Config group names must be specific — e.g. `"deadman-prices"`, not `"deadman"`.
- Never rename a config key or config group without providing a migration. Renaming silently resets users' saved settings.
- If you add a `@ConfigItem` that toggles a feature involving a third-party server, it must:
  - Be **disabled by default** (opt-in)
  - Have a `warning` field set to: `"This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers"`

## Plugin Setup & Packaging

- Rename everything from the template. Do not leave `com.example`, `ExamplePlugin`, `ExampleConfig`, or `example` as the config group. Rename the package path, class names, config group, `build.gradle` group, `settings.gradle` project name, and `runelite-plugin.properties`.
- Do not include a `META-INF/services/net.runelite.client.plugins.Plugin` file.
- Do not commit build artifacts — no `.class` files, `out/` directories, or `.tmp` directories.
- `build.gradle` must target Java 11** and match the structure of the example-plugin template.
- Retain a permissive license, such as BSD-2.

## Resources & Assets

- Optimize icon PNGs. Java loads images at full resolution in memory (`width × height × 4` bytes), so a seemingly small file can use significant memory.
- Ensure PNGs are actually PNGs — do not rename JPEGs or ICOs to `.png`.

## Cleanup

- Remove unused config classes, fields, and imports.
- Clean up subscriptions, listeners, and overlays in `shutDown()`.
- Do not mix code reformatting with feature changes in the same commit — it makes diffs unreadable for reviewers.

## Testing

You cannot verify plugin behavior yourself. Even if you have screen-capture or computer-use tools available, **do not use them to interact with RuneScape** — automating game input violates Jagex's third-party client guidelines and will get the user's account banned. Only the user can confirm a plugin works in-game.

After completing a task, do not declare it done. Instead:

1. Offer to launch RuneLite for the user by running `./gradlew run` from the plugin's root directory.
2. Instruct the user to follow the "Using Jagex Accounts" instructions found at https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts to login to the development client.
3. Tell the user *what to test* — the specific behavior you changed, the golden path, and any edge cases worth exercising.
4. Wait for the user to confirm the feature works in-game before considering the task complete. A clean JVM start is not a passing test.

## Submitting

When submitting a pull request to runelite/plugin-hub, append an attribution footer to the PR description structured like:

Generated-by: Claude Code

---

# Plugin Rules & Restrictions

Features that are **forbidden or restricted** in RuneLite hub plugins.
Sourced from [Jagex's Third-Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1) and RuneLite's [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).

**If your plugin does any of the things listed below, it will be rejected.**

## Forbidden Language Features

- All code must be Java 11 compatible
- No use of reflection
- No use of JNI or JNA
- No direct access to native memory access via Unsafe or LWJGL
- No executing external processes, including with Process or ProcessBuilder
- No downloading or dynamic loading of code, including classloading
- No runtime generation of code
- No use of Java (de)serialization

## Boss & Combat Restrictions

Applies to all bosses, Raids sub-bosses, Slayer bosses, Demi-bosses, and wave-based minigames (Fight Caves, Inferno, etc.):

- No next-attack prediction (timing or attack style)
- No projectile target/landing indicators
- No prayer switching indicators
- No attack counters
- No automatic indicators showing where to stand or not stand (manual tile marking is allowed)
- No additional visual or audio indicators of a boss mechanic, unless it is a manually triggered external helper
- No advance warning of future hazards (highlighting currently active hazards is OK)
- No "flinch" timing helpers
- No combat prayer recommendations
- No NPC focus identification (which player the NPC is targeting)
- No content simulation (e.g. boss fight simulators)

New high-end PvM boss plugins are not accepted as a blanket policy.

## PvP Restrictions

- No removing or deprioritising attack/cast options in PvP
- No opponent freeze duration indicators
- No PvP clan opponent identification
- No PvP loot drop previews
- No identifying an opponent's opponent
- No PvP target scouting information
- No player group summaries (attackable counts, prayer usage, etc.)
- No level-based PvP player indicators (highlighting attackable players or those within level range)
- No spell targeting simplification (removing menu options to make targeting easier)

## Menu Restrictions

- No adding new menu entries that cause actions to be sent to the server
- No conditional menu entry removal based on NPC type, friend status, etc. (can be overpowered)

## Interface Restrictions

- No unhiding hidden interface components (special attack bar, minimap)
- No moving or resizing click zones for 3D components
- No moving or resizing click zones for combat options, inventory, equipment, or spellbook
- No resizing prayer book click zones
- No resizing spellbook components
- No removing inventory pane background or making it click-through
- No detached camera world interaction (interacting with the game world from a camera position that isn't the player's)

## Input Restrictions

- No injecting input events, including mouse and keyboard events
- No autotyping — plugins must not programmatically insert text into the chatbox input (includes pasting, shorthand expansion)
- No modifying outgoing chat messages after the user sends them

## Data & Privacy Restrictions

- No exposing player information over HTTP
- No crowdsourcing data about other players (locations, gear, names, etc.)
- No credential manager plugins that stores account credentials

## Content Restrictions

- No adult or overtly sexual content
- No plugins that use player-provided IDs for their entire functionality (causes moderation issues)
