# RuneTongue

A RuneLite plugin that shows Old School RuneScape in Korean (한국어), and lets anyone play with their own hand-made translation in another language by dropping a tsv file into a folder.

All translations are made ahead of time. The Korean set (about 125,000 lines) ships inside the plugin, and the plugin never connects to a translation service or any other server.

## What gets translated

- NPC dialogue, your character's lines and dialogue options, plus message, item and level-up boxes.
- Right-click menus and the text at the top left of the screen. Other players' names are left alone. Hovering shows the English in a tooltip; you can turn this off with "Show English on hover".
- Examine text, game messages and NPC overhead text. Chat typed by players is never touched.
- Interfaces: side tabs, hover descriptions (prayers, spells, settings, emotes), skill guides, the quest list, settings and more.

When a text has no translation, it stays exactly as the game shows it.

## Translating into your own language

You do not need a custom build, hosting, hash files or anyone's approval.

1. In the plugin settings, pick your language (Korean, Spanish, Portuguese, French or German for now). Korean has bundled translations; the others start empty.
2. Open the RuneTongue side panel (the "한" button) and click **Open folder**. The panel and the plugin's chat messages are in English for every language but Korean. The folder is `.runelite/plugin-data/runetongue/translations/<language code>/`, for example `translations/es/`.
3. Put one or more `.tsv` files there, or use **Add files** to copy them in.
4. Click **Reload** or type `::rh reload` in the chat box. No restart is needed. The panel lists, for each file, the lines that were loaded and the lines that were skipped, with the reason.

### File format

Tab-separated text, saved as UTF-8 (with or without BOM) or as Excel's "Unicode Text" (UTF-16).

```
english	translation	category	sub_category	source
Talk-to	Hablar
You have 5 coins.	Tienes 5 monedas.
Hello there!	¡Hola!	dialogue	Hans	Hans
```

- Only `english` and `translation` are required. `category`, `sub_category` and `source` narrow where a line applies; a line without a category applies everywhere.
- A header line is optional. Without one, the columns are in the order shown above.
- Numbers match any number: `You have 5 coins.` also translates "You have 1,234 coins." Keep every number from the English in your translation.
- Your files win over the bundled translations. Files are read in name order, and a later file wins over an earlier one.
- A line with an empty translation counts as untranslated, not as an error.

See [docs/examples/example_ko.tsv](docs/examples/example_ko.tsv) for a small example.

### Collecting the text you still need

While you play, any text the plugin could not translate is written to `.runelite/plugin-data/runetongue/missing/<language code>/missing.tsv`. The file uses the same format with the translation column left empty. Fill in the translations, move the file into the `translations` folder and reload. Lines that become translated are removed from `missing.tsv` on the next reload, and the panel shows how many are left. **Open untranslated-text folder** opens it.

The file stays on your computer; nothing is sent anywhere. Chat and other players' names are never collected. If you would like your translations included in a release, share the file on GitHub or Discord.

## Commands

- `::rh reload`: reload the translation files.
- `::rh find <english>`: show the active translation of a text.

## Building

`./gradlew build` builds the plugin and bundles `data/<language>/*.tsv` into it. `./gradlew run` starts a RuneLite development client with the plugin. Both run on JDK 11 or newer. [docs/CONTEXT.md](docs/CONTEXT.md) has the design notes and history (in Korean).

## Credits and licenses

- Plugin code: BSD 2-Clause, see [LICENSE](LICENSE).
- Parts of the code come from [RuneLingual](https://github.com/YS-jack/RuneLingual-Plugin) (BSD 2-Clause, Copyright (c) 2024, Y. S. Jack). `Widget2ModDict.java` is a copy with small changes; `InterfaceRules.java` is ported from its `Ids` and `WidgetCapture`. The original notice is kept in those files.
- English source text is game text © Jagex. It comes from the [RuneLingual transcripts](https://github.com/YS-jack/Runelingual-Transcripts). NPC dialogue in that set was collected from the [Old School RuneScape Wiki](https://oldschool.runescape.wiki/) (CC BY-NC-SA 3.0). See [data/README.md](data/README.md).
- The Korean translations were made for this project.
- Korean font: [Galmuri](https://github.com/quiple/galmuri) by Lee Minseo, SIL Open Font License 1.1 (`src/main/resources/com/runetongue/Galmuri11-OFL.txt`).

This plugin is not affiliated with Jagex or RuneLite.
