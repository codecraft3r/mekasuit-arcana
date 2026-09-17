# Contributing

This project is publicly readable but **All Rights Reserved**, not open source.
Please discuss substantial changes in an issue before preparing a contribution;
no broader license or copyright assignment is implied by this guide.

## Development

Use Java 21 and Python 3. Gradle resolves the pinned compile-only dependencies
from Modrinth Maven:

```sh
./gradlew build runtimeTestJar
python tools/verify-release.py
```

On Windows use `gradlew.bat`. The normal jar must not contain dependency or
runtime-harness classes. Keep changes scoped and include regression tests.

## Design Constraints

- Bodyarmor only; mainhand Meka-Tool accepts Amplification and Focus only.
- Keep per-carrier caps, configuration, and FE accounts independent.
- Use Iron's existing mana, casting, and timers; no custom casting or spell storage.
- Insufficient FE removes bonuses without blocking ordinary casting.
- Do not commit dependency jars, game worlds, logs, tokens, or personal paths.

## Versioning & Tagging Rules (For Agents & Humans)

This repository strictly enforces [Semantic Versioning (SemVer 2.0.0)](https://semver.org/spec/v2.0.0.html) and standardized Git tag formats.

### 1. Tag Format
- **Rule**: Git tags **MUST ALWAYS** be formatted as `vx.x.x` (e.g. `v0.1.0`, `v0.2.0`, `v1.0.0`).
- Do not include Minecraft versions, suffixes, or build metadata directly in the Git tag (use `vx.x.x` exclusively).
- Version components:
  - `MAJOR` (`x.0.0`): Incompatible changes, breaking API/configuration overhauls, or fundamental mechanic redesigns.
  - `MINOR` (`0.x.0`): New features, new modules, or backwards-compatible server/operator enhancements.
  - `PATCH` (`0.0.x`): Backwards-compatible bug fixes and minor balancing adjustments.
  - `0.x.x`: Initial development and pre-release phase. Releases with major version `0` are automatically published as **Pre-releases** on GitHub.

### 2. Gradle Version Alignment
- Keep `mod_version` in `gradle.properties` aligned with the project's current active version (e.g. `mod_version=0.1.0`).
- The Gradle build supports dynamic overrides via `-Pversion=<version>` or `-PmodVersion=<version>`.

## Changelog Rules (Keep a Changelog 1.1.0)

All notable changes must be documented in `CHANGELOG.md` adhering strictly to [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/):

1. **Unreleased Changes**:
   - All in-progress changes must be recorded under the `## [Unreleased]` section at the top of `CHANGELOG.md`.
2. **Cutting a Release**:
   - Move the unreleased items into a new version block directly below `## [Unreleased]`.
   - Format: `## [x.x.x] - YYYY-MM-DD` (e.g. `## [0.1.0] - 2026-09-16`).
   - Group entries into standard sub-headers:
     - `### Added`: New modules, configs, commands, or behaviors.
     - `### Changed`: Changes in existing features, recipes, or balance values.
     - `### Deprecated`: Features scheduled for removal in future versions.
     - `### Removed`: Previously deprecated features that are now removed.
     - `### Fixed`: Bug fixes, exploit resolutions, or crash prevention.
     - `### Security`: Security or vulnerability patches.
3. **Reference Links**:
   - Maintain the reference links at the bottom of `CHANGELOG.md`:
     ```markdown
     [Unreleased]: https://github.com/codecraft3r/mekasuit-arcana/compare/vx.x.x...HEAD
     [x.x.x]: https://github.com/codecraft3r/mekasuit-arcana/releases/tag/vx.x.x
     ```

## Automated Release Pipeline

When a tag matching `v*` is pushed to GitHub:
1. `.github/workflows/release.yml` triggers automatically.
2. `tools/extract-changelog.py` extracts the corresponding `[x.x.x]` section from `CHANGELOG.md`.
3. Gradle compiles the production jar and dev harness.
4. `tools/verify-release.py` enforces production packaging guarantees.
5. GitHub CLI (`gh release create`) publishes the release (with `--prerelease` automatically applied for `0.x.x`).

## Evidence

`gradlew test` checks loader-free logic. It does not boot Minecraft. See
[the runtime report](docs/RUNTIME_PROOF.md) for server integration coverage and
remaining client/full-pack checks. Never describe a compile or boot as gameplay
verification. Include exact mod versions when reporting a compatibility issue.
