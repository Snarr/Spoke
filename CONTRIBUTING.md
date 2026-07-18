# Contributing to Spoke

Thank you for your interest in contributing to Spoke! We welcome contributions of all kinds, including bug reports, feature requests, documentation improvements, and code contributions.

## Development Workflow

### Setting Up Your Environment

Follow the setup instructions in [README.md](README.md) to get your development environment ready.

### Code Style

We maintain consistent code style using Spotless and Detekt.

**Before committing**, run:
```bash
./gradlew spotlessApply   # Automatically format code
./gradlew detekt          # Check for style violations
```

### Commit Messages

All commits **must** follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification. This keeps history readable and drives our versioning (see [Versioning](#versioning)).

## Versioning

Spoke uses a simple two-part version scheme (`MAJOR.MINOR`):

- **MAJOR** — significant releases: major new features, redesigns, or milestones
- **MINOR** — everything else: smaller features and bug fixes

For example: `1.0` → `1.1` → `1.2` → `2.0`. There is no separate patch segment. All notable changes are recorded in [CHANGELOG.md](CHANGELOG.md), which follows the [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) format.

## Running Tests Locally

### Unit Tests
```bash
./gradlew test
```

### Instrumentation Tests
```bash
# Connect a device or start an emulator first
./gradlew connectedAndroidTest
```

## Branch Protection Rules

The main branch has the following protections:
- Pull requests must be approved before merging
- All CI/CD checks must pass

## License

By contributing, you agree that your contributions will be licensed under the MIT License (see [LICENSE](LICENSE)).

## Community Guidelines

We are committed to providing a welcoming and inclusive community. Please review our expectations:

- **Be respectful**: Treat all community members with respect
- **Be constructive**: Provide helpful feedback and suggestions
- **Be inclusive**: Welcome contributors of all backgrounds and experience levels
- **Report misconduct**: If you witness inappropriate behavior, please report it

## Questions?

- Open a [Discussion](https://github.com/Snarr/Spoke/discussions)
- Review [existing Issues](https://github.com/Snarr/Spoke/issues)

Thank you for contributing to Spoke! 🚴
