# Changelog

All notable changes to WinJavaDriver are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-07-29

MSFlexGrid cell access is rewritten to address cells by index rather than by screen position. The
previous implementation could read and write the wrong cell without reporting an error.

### ⚠️ Breaking

- **Reading or writing a grid column other than the editable column now fails** with an explicit
  error instead of returning (or overwriting) the editable column's value. Code that appeared to
  work while silently operating on the wrong column will now raise an error. Pass the new
  `editableColumn` parameter if your grid's editor serves a column other than `1`.
- **Grid cell operations on unreachable cells no longer "succeed" quietly.** Any call that cannot
  be satisfied reports a failure rather than returning a plausible but incorrect value.
- `GET /session/{id}/winjavadriver/grid/{eid}/info` response fields changed: added `rowCountHint`
  and `columnCount`. Previous releases documented `rows`/`cols` but never returned them.

### Fixed

- **MSFlexGrid cells were addressed by computed screen coordinates** derived from hardcoded row
  height, column width and header height, with no scroll offset. Cells outside the visible area
  were unreachable, and because the driver is per-monitor DPI aware while VB6 hosts are DPI
  unaware, the same code selected different cells at different display scaling. On a 150% display
  every row read incorrectly, not only off-screen ones — requesting row 32 returned row 17, and
  requesting row 3 returned row 1. Cells are now selected by logical row and column steps, so
  results no longer depend on grid geometry, screen resolution or display scaling, and the grid
  scrolls off-screen rows into view itself.
- **Non-editable columns silently returned the wrong column's value.** Cell text is read through
  the grid's editor, which only ever holds the editable column, so a request for a label column
  returned the value column's text instead. Such requests are now rejected explicitly.
- **A grid edit could be committed to the wrong cell.** The commit keystroke was sent
  asynchronously while navigation was synchronous, so it could be processed after the selection had
  moved — writing whatever the editor held at that moment into whatever cell was selected by then.
- **`clear()` on a grid cell had no effect** while reporting success: the editor was emptied but the
  edit was never committed, so the cell kept its old value and a following `sendKeys` concatenated
  onto it.
- **Reading an empty control returned a stale value.** A zero-length reply was treated as "no
  answer" and fell back to an API that does not read live text across process boundaries, returning
  a previously cached string. This affected every element's `getText()`, not only grid cells.
- **`win_grid_edit` defaulted to column 0**, which is not the editable column in a typical grid, so
  batch edits were applied to the wrong column. It now defaults to the editable column.
- **Virtual grid cell elements reported fabricated bounds.** `getRect()` returned a rectangle
  computed from assumed cell sizes. It now reports the containing grid's real bounds, because
  per-cell rectangles are not discoverable.

### Added

- `editableColumn` parameter on the grid endpoints, the Java client
  (`getGridCellValue`, `setGridCellValue`, `getGridCell` overloads) and the MCP grid tools, for
  grids whose editor serves a column other than `1`.
- `rowCountHint` on grid info, derived from the grid's scroll range, which a grid expresses in rows
  and is therefore valid at any DPI. `null` when the grid has no scrollbar.
- `WinJavaDriver.DEFAULT_GRID_EDITABLE_COLUMN` and `WinJavaDriver.DEFAULT_GRID_EDIT_FIELD_ID`
  constants.

### Documentation

- `docs/troubleshooting.md` no longer recommends computing cell positions and clicking them, and
  documents the editable-column limitation.
- README documents index-based cell addressing and the `editableColumn` parameter.

### Known limitations

- Reading a non-editable grid column is still not possible; it now fails clearly instead of
  returning wrong data. Tracked in
  [#11](https://github.com/glaciousm/winjavadriver/issues/11).
- `sendKeys` inserts text at the start of a control's existing contents rather than appending at
  the end. Use `clear()` before `sendKeys()`. Tracked in
  [#16](https://github.com/glaciousm/winjavadriver/issues/16).
- Element clicks may land off-centre on scaled displays when the target application is DPI
  unaware. Unconfirmed; tracked in
  [#15](https://github.com/glaciousm/winjavadriver/issues/15).
- The integration test suite launches `notepad.exe` and will attach to an already-running Notepad,
  closing it on `quit()`. Tracked in
  [#14](https://github.com/glaciousm/winjavadriver/issues/14).

## [1.1.0]

### Added

- Convenience API: wait helpers, element indexing, child enumeration, position-based lookup, retry
  and global `sendKeys()`
- `switchBack()` for window chaining
- System window discovery: `listAllWindows()`, `switchToWindowByTitle()`
- Full desktop screenshot capture

## [1.0.4]

### Added

- Desktop screenshot support

## [1.0.0]

Initial public release: W3C WebDriver server for Windows desktop applications, Java client
extending Selenium's `RemoteWebDriver`, MCP server, and Inspector GUI with record and replay.

[1.2.0]: https://github.com/glaciousm/winjavadriver/releases/tag/v1.2.0
[1.1.0]: https://github.com/glaciousm/winjavadriver/releases/tag/v1.1.0
[1.0.4]: https://github.com/glaciousm/winjavadriver/releases/tag/v1.0.4
[1.0.0]: https://github.com/glaciousm/winjavadriver/releases/tag/v1.0.0
