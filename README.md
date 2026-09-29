# LoanDesk

LoanDesk is a JavaFX desktop application for managing equipment lending across
three roles: borrower, lab supervisor, and equipment custodian.

## Development

Prerequisites:

- JDK 25
- Internet access on the first Gradle build so dependencies can be downloaded

Run the test suite from a Windows terminal:

```powershell
.\gradlew.bat test
```

Run the desktop application from a Windows terminal:

```powershell
.\gradlew.bat run
```

On macOS/Linux, use `./gradlew test` and `./gradlew run` respectively.

Build a runnable JAR for the current operating system:

```powershell
.\gradlew.bat fatJar
```

The output is `build/libs/loandesk-0.1.0-all.jar`. It includes the application,
H2, and the platform-specific JavaFX runtime selected by Gradle. Run it with
`java -jar build/libs/loandesk-0.1.0-all.jar`. Build it on each target OS; JavaFX
native libraries are OS-specific. On macOS/Linux, use `./gradlew fatJar`.

Pushing a version tag such as `v1.0.0` runs the three-platform CI matrix and
publishes the resulting Linux, Windows, and Apple-silicon macOS JARs to a
GitHub Release: `loandesk-linux.jar`, `loandesk-windows.jar`, and
`loandesk-macos-arm64.jar`. Test each release asset on its matching OS before
announcing compatibility.

The current `main` branch is the shared integration baseline. Create feature
branches from it and use pull requests for changes.

See [docs/DeveloperGuide.md](docs/DeveloperGuide.md) for the project layout
and contribution workflow. User-facing setup instructions will be maintained
in [docs/UserGuide.md](docs/UserGuide.md). The complete progress tracker is
in [docs/ProjectChecklist.md](docs/ProjectChecklist.md).

Borrower review skills and optional per-checkout Git hooks are documented in
[docs/AgenticSE.md](docs/AgenticSE.md) and the
[developer setup guide](docs/DeveloperGuide.md#borrower-skills-and-personal-hooks).
