# Flash Card data format and integrity contract

## Scope

This reference defines the internal Room representation and the clipboard JSON format. These contracts are shared by `CardRepositoryDatabase`, `CardDealerImpl`, set management, TTS settings, the daily counter, and `raw-vocab-to-meta-tool.py`.

Changing identifiers, link direction, zero-card fields, or JSON fields requires coordinated code changes and a compatibility/migration plan.

## Domain card

The business and interchange model is `Card`:

| Field | Type | Meaning |
| --- | --- | --- |
| `id` | string | Logical ID. Empty for the zero card; normally eight lowercase hexadecimal characters for a regular card. |
| `title` | string | Prompt-side text. Empty only on the zero card. |
| `body` | string | Answer-side text, or metadata on the zero card. |
| `level` | integer | Scheduling level, clamped to 0 through 10 by rating logic. |
| `previous` | string | Logical ID of the previous node in the circular queue. |
| `state` | boolean | `true` for remembered and `false` for forgotten. |

Regular IDs created in the app start as the first eight hexadecimal characters of the title's MD5 digest. On collision, the complete stored candidate is hashed repeatedly until a database-wide unused stored ID is found. IDs should be treated as opaque; consumers must not recompute them from titles.

## Room entity

Room stores `CardEntity` rows in table `card`:

| Column | Type | Internal representation |
| --- | --- | --- |
| `id` | text, primary key | `<logical-id>@<collection>`; zero card is `@<collection>`. |
| `title` | text | Card title; empty for the zero card. |
| `body` | text | Card body; zero card uses this for metadata. |
| `level` | integer | Scheduling level. |
| `previous` | text | Previous stored ID including `@<collection>`. |
| `state` | integer | `1` for true/remembered, `0` for false/forgotten. |
| `coll` | text | Collection name, matching the ID suffix. |

The database filename is `card-database`, schema version is 1, and no migration is registered.

### Collection-name restrictions implied by storage

The UI currently does not validate names beyond duplicate detection. Nevertheless, the encoding assumes a collection name is non-empty and does not contain `@`:

- `@` is the separator stripped from stored IDs with `substringBefore("@")`.
- An empty name produces `@`, which collides with the sentinel encoding and `(none)` selection semantics.

Until validation is added, import producers and maintainers should use non-empty names without `@`. Renaming a collection is not implemented.

## Circular-list invariant

For one collection `demo` with two cards `a1` and `b2`, a valid logical structure is:

```text
zero.previous = b2
a1.previous   = zero
b2.previous   = a1
```

The stored rows are:

```text
id       previous  coll
@demo    b2@demo   demo
a1@demo  @demo     demo
b2@demo  a1@demo   demo
```

The queue is traversed forward by finding the unique row whose `previous` equals the current ID:

```text
@demo -> a1@demo -> b2@demo -> @demo
```

A structurally valid collection must satisfy all of the following:

1. It contains at least one row.
2. It contains exactly one effective sentinel whose ID begins with `@`.
3. All regular rows have non-empty titles.
4. Every row has one unique predecessor link and one unique successor.
5. Following successors visits every row once and returns to the sentinel; disconnected cycles are invalid.
6. IDs and `previous` pointers use the same collection suffix, and `coll` matches that collection.

`CardRepositoryDatabase.isStructureIntactForList` checks the core single-cycle/zero/title conditions. Suffix and `coll` consistency are constructed by normal app writes but are not exhaustively validated on every path; external import producers must honor them.

### Empty collection

An app-level empty collection still contains one database row:

```json
{"id":"","title":"","body":"","level":0,"previous":"","state":true}
```

After persistence, both `id` and `previous` become `@<collection>`. `getTop()` therefore returns the zero card, which the dealer translates to `CollectionEmpty`.

## Zero-card metadata

The zero card is not shown or edited as a normal flash card. Its `body` is split on commas into positional fields:

| Index | Example | Meaning | Default/fallback |
| ---: | --- | --- | --- |
| 0 | `2026-09-02` | Effective local study date in ISO format. | Empty string. |
| 1 | `17` | Count of cards rated remembered that study day. | `0`. |
| 2 | `en-US-` | TTS locale for the title side. | Empty; device locale at playback. |
| 3 | `es-MX-` | TTS locale for the body side. | Empty; device locale at playback. |
| 4 | `bijective` | Enables two-way prompting only when exactly this value is present. | Any other/absent value means one-way. |
| 5 | `26` | Title-side text size in sp. | `0`/invalid maps to app default 26sp. |
| 6 | `18` | Body-side text size in sp. | `0`/invalid maps to app default 18sp. |

A full example is:

```text
2026-09-02,17,en-US-,es-MX-,bijective,26,18
```

The format is unescaped and unversioned. Metadata values must not contain commas. Writers must retain unknown/trailing positions when updating one field so that newer metadata is not accidentally discarded. The current setters preserve later fields when the expected prefix already exists, but create only the minimum prefix when metadata is absent.

The daily rollover date is computed from local time minus three hours. Thus the logical study day changes at 03:00 local time.

## Clipboard JSON envelope

Export produces a two-element JSON array:

```json
[
  "demo",
  [
    {
      "id": "",
      "title": "",
      "body": "2026-09-02,0,en-US-,es-MX-,bijective,26,18",
      "level": 0,
      "previous": "b2",
      "state": true
    },
    {
      "id": "a1",
      "title": "hello",
      "body": "hola",
      "level": 0,
      "previous": "",
      "state": true
    },
    {
      "id": "b2",
      "title": "goodbye",
      "body": "adiós",
      "level": 0,
      "previous": "a1",
      "state": true
    }
  ]
]
```

Important properties:

- The first element is the destination collection name, not merely a display label.
- The second element must be a non-empty array of complete `Card` objects.
- Logical `id` and `previous` values do not include `@<collection>`.
- Exactly one zero card with an empty ID is required.
- The list order itself is not the queue order; `previous` links define order.
- Unknown JSON fields are ignored by Gson, but required fields and their types should be supplied.
- The format has no explicit version field.

### Import behavior

`ImportExportViewModel` parses the envelope, deserializes the card list, and calls `importCollection(cards, collectionName)`. The repository:

1. Rejects an empty list or one without an empty logical ID.
2. Adds the destination suffix to IDs and links and converts boolean state to 1/0.
3. Opens a Room transaction and deletes the existing destination rows.
4. Inserts the replacement rows and validates that they form one intact circular structure.
5. Commits only on success; an exception or structural failure rolls the transaction back to the previous destination contents.
6. Changes the current collection only after a successful result.

Import targets the collection named in the payload. It does not wipe unrelated sets. Importing a name that already exists replaces that set; importing a new name creates it through the included zero card.

Add, delete, empty, and review/bury operations also execute their multi-row link changes in Room transactions. Review writes the top card's scheduling fields and moves it in the cycle atomically.

## Vocabulary converter

`raw-vocab-to-meta-tool.py` creates an import payload from UTF-8 tab-separated text:

```text
hello<TAB>hola
goodbye<TAB>adiós
```

Run it from the repository root:

```powershell
python .\raw-vocab-to-meta-tool.py .\demo.txt --output .\demo.json
```

Behavior:

- Uses the input filename without its extension as the collection name.
- Ignores blank lines and lines without a tab.
- Splits each valid line at the first tab.
- Removes duplicate `(title, body)` pairs.
- Generates random, unique eight-character lowercase hexadecimal IDs.
- Produces every regular card at level 0 with state true.
- Produces a zero card and closes the circular `previous` chain.

The script's introductory text mentions appending to an existing payload with `--meta`, but the current argument parser implements only creation with optional `--output`.

## Compatibility checklist

Before accepting a storage or interchange change, verify:

- Empty, single-card, and multi-card collections remain a single cycle.
- Add, delete, edit, bury, empty, export, import, and delete-set behaviors preserve the invariant.
- Existing database rows can be read or have an explicit Room migration.
- Existing exported payloads still import, or the envelope has explicit version handling.
- Metadata setters preserve fields they do not own.
- Current and previous selections cannot point to missing collections.
- Logical IDs remain suffix-free outside the Room implementation.
- Malformed imports fail without losing the previous destination collection.
