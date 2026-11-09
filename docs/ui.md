# UI guide

The UI is a JavaFX desktop app in `src/ui/`. Run it from the repo root (see the README).

## Pages and navigation

| Sidebar label | Class | Stylesheet |
|---|---|---|
| Home | `HomePage` | `styles.css` |
| Import Files | `ImportPage` | `import.css` |
| Generate | `GeneratePage` | `generate.css` |
| Batch Generate | `BatchPage` | `batch.css` |
| Auto-complete | `AutoCompletePage` | `autocomplete.css` |
| Word Explorer | `WordExplorerPage` | `explorer.css` |
| Reports | `ReportsPage` | `reports.css` |

- `App` owns a `Map<String, Supplier<Parent>>` from page name to page builder. A page is built the first time it is opened, then reused (so its state survives switching pages).
- `Sidebar` holds the page-name constants. Clicking a button calls the `onNavigate` callback with the name; `App.show` swaps the center pane and calls `Sidebar.setActive`.
- `HomePage` takes the same callback so its buttons and links can navigate.

### Adding a page
1. Create `XxxPage` with a `getView()` that returns `Widgets.page(Widgets.header(...), ...)`.
2. Add `src/ui/css/xxx.css` and attach it in the constructor: `view.getStylesheets().add(new File("src/ui/css/xxx.css").toURI().toString())`.
3. In `Sidebar`, add a name constant, an SVG icon, and an `addNavButton` call.
4. In `App.start`, add `pages.put(Sidebar.XXX, () -> new XxxPage().getView())`.
5. Add the page to `PagesSmokeTest` and write tests for any logic.

## Styling
- `styles.css` is loaded by `App` and holds everything shared; each page's own CSS holds only what is specific to it.
- Build common pieces with `Widgets` (`header`, `page`, `caption`, `pill`, `muted`) so pages look the same.
- Shared classes: `card` (`flush` for no padding), `card-header`, `section-title`, `field-label`, `muted`, `mono`, `primary` (main button), `link`, `pill` + `blue`/`green`/`orange`.
- State classes are toggled in code: `active` (sidebar button), `selected` (algorithm choice), `duplicate` and `unlikely`.
- CSS files are loaded by relative path (`src/ui/css/...`), so the app and the tests must run from the repo root.

## Backend integration
Every page currently shows sample data, and its buttons do nothing. Each spot is marked `// TODO` in the code. What the backend needs to provide:

| Page | Needed |
|---|---|
| Sidebar | Real database connection status (currently the fixed text "MySQL · not connected") |
| Home | Stat tiles (unique words, total words read, word pairs, files imported), recent imports, recently generated sentences |
| Import | Choose files and start the import with the three options (strip Gutenberg header/footer, lower-case, keep contractions); live progress and counts; cancel; import history |
| Generate | Generate from a starting word, algorithm (weighted bigram / most frequent / random follower) and max words, returning the sentence plus the chance of each chosen word; copy; add sentence to corpus; session history |
| Batch | Generate N sentences from random start words; flag sentences already in the database; add ticked sentences; save to history only; discard |
| Auto-complete | Suggestions for the next word; accept a suggestion; save the sentence; clear |
| Word Explorer | Search, sort and filter words; load a word and its followers; paginate; save edited counts or reload originals |
| Reports | Filter by text, algorithm and duplicates; word statistics; export to CSV |

When wiring these up, keep the database calls off the JavaFX thread (use a `Task`) and update controls back on it.

## Tests
Run `./run-tests.sh` from the repo root. Tests live in `test/ui/` and use JUnit 5 on a real JavaFX toolkit, with `FxTestSupport` to build pages on the FX thread and find nodes. See the README for setup.
