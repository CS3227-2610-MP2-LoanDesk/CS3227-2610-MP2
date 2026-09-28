# Release candidate A — ShelfDesk 1.0.0

Everything below is the complete supplied evidence for this candidate.

## Release notes (draft, not yet published)

> ShelfDesk 1.0.0 is our first production release. The packaged application
> runs on Windows, macOS and Linux — download the zip, unpack it and run the
> launcher script for your platform. Java 25 is required.
>
> Known limitation: shelf maintenance reporting is not implemented in this
> release. The user guide marks that section as unavailable.

## `gradlew assemble` output, contents of `build/distributions/shelfdesk-1.0.0.zip`

```text
shelfdesk-1.0.0/bin/shelfdesk
shelfdesk-1.0.0/bin/shelfdesk.bat
shelfdesk-1.0.0/lib/shelfdesk-1.0.0.jar
shelfdesk-1.0.0/lib/h2-2.5.250.jar
shelfdesk-1.0.0/lib/javafx-base-21.0.6-win.jar
shelfdesk-1.0.0/lib/javafx-controls-21.0.6-win.jar
shelfdesk-1.0.0/lib/javafx-graphics-21.0.6-win.jar
```

## Version declarations

- `build.gradle`: `version = '1.0.0'`
- Planned git tag: `v1.0.0`
- Release notes heading: `ShelfDesk 1.0.0`

## Documentation state

- `docs/UserGuide.md` describes browsing, reserving and collecting a shelf
  item. Its maintenance section reads "not available in this release".
- `docs/DeveloperGuide.md` describes the current architecture and has sections
  for structure, storage, workflow rules and testing. It has no
  acknowledgement section.
- The product website at `https://example-org.github.io/shelfdesk/` returns a
  page describing version 1.0.0.

## Build verification performed

- `gradlew build` run once on Windows 11. Passed.
