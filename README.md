# LoanDesk

LoanDesk is a JavaFX desktop application for managing equipment lending across
three roles: borrower, lab supervisor, and equipment custodian.

## Development

Prerequisites:

- JDK 25
- Internet access on the first Gradle build so dependencies can be downloaded

Run the test suite:

```powershell
./gradlew test
```

Run the desktop application:

```powershell
./gradlew run
```

The current `main` branch is the shared integration baseline. Create feature
branches from it and use pull requests for changes.

See [docs/DeveloperGuide.md](docs/DeveloperGuide.md) for the project layout
and contribution workflow. User-facing setup instructions will be maintained
in [docs/UserGuide.md](docs/UserGuide.md). The complete progress tracker is
in [docs/ProjectChecklist.md](docs/ProjectChecklist.md).