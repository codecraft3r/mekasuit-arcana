# MekaSuit Arcana

<img src="src/main/resources/icon.png" alt="MekaSuit Arcana Icon" width="96" align="right" />

[![CI](https://github.com/codecraft3r/mekasuit-arcana/actions/workflows/ci.yml/badge.svg)](https://github.com/codecraft3r/mekasuit-arcana/actions/workflows/ci.yml)

**Early preview.** 43 unit tests and 16 isolated server assertions have passed.
Connected-client GUI and full-modpack playtesting remain unverified.

Minecraft 1.21.1 / NeoForge 21.1.248 integration for Mekanism 10.7.19.85 and
Iron's Spells 'n Spellbooks 3.16.3. Java 21 required.

Five Mekanism modules add powered magic support to MekaSuit Bodyarmor.
Arcane Energy Amplification and Attunable Lens Matrix Units also work on a held
Meka-Tool using staff-style attribute bonuses. Use your existing spellbook and
Iron's normal casting controls.

## Modules Overview

| Unit | Supported Carriers | Max Units | Effect per Unit | Max Contribution (4 Units) | Utility & Notes |
| --- | --- | ---: | --- | --- | --- |
| **Arcane Transformer Unit** | Bodyarmor | 4 | Capacity tier + FE-to-mana conversion | 10,000 Max Mana, 50k FE/t conversion | Configurable speed presets |
| **Arcane Energy Amplification Unit** | Bodyarmor, Meka-Tool | 4 | +100% Spell Power (all schools) | **+400% Spell Power** | Stacks additively across carriers |
| **Attunable Lens Matrix Unit** | Bodyarmor, Meka-Tool | 4 | +100% Spell Power (selected school) | **+400% School Spell Power** | Configurable spell school |
| **Rapid Casting Unit** | Bodyarmor | 4 | -25% Spell Cooldown | **-100% Spell Cooldown (0s cooldown)** | Linear duration-based FE cost |
| **Casting Stabilization Unit** | Bodyarmor | 4 | -25% Cast Duration | **-100% Cast Duration (Instant cast)** | **Uninterruptible concentration** & **no movement penalty** (1+ units) |

### Key Module Mechanics

- **Rapid Casting**: Linearly reduces spell cooldown by 25% per installed unit. At 4 units (100% reduction), spell cooldowns are completely eliminated. Energy is consumed upon cast proportional to the cooldown duration saved, plus a baseline slot fee.
- **Casting Stabilization**: Linearly shortens long-cast spell duration by 25% per installed unit. At 4 units (100% reduction), spells cast instantly. Energy is consumed dynamically based on cast time saved plus a casting maintenance fee.
  - **Concentration**: Having 1 or more powered units grants uninterruptible casting concentration (identical to the Amulet of Concentration), preventing spell cancellation when taking damage.
  - **Full Movement**: Having 1 or more powered units completely eliminates the movement slowdown penalty while casting or channeling spells. Continuous (channeled) spells maintain normal duration and pulse intervals while allowing full walking and sprint speed.
- **Arcane Energy Amplification & Attunable Lens Matrix**: Provide massive spell power scaling (+100% per unit, up to +400% at 4 units). When installed on both a MekaSuit Bodyarmor and a Meka-Tool, their contributions stack additively (up to +800% combined spell power). Billed per cast event (or per channel pulse).

## Using the Modules

1. **Crafting**: Craft the module units in a crafting table using Mekanism circuits/alloys and Iron's Spells arcane materials (Scrolls, Tomes, Arcane Cloth, Amulets, etc.).
2. **Installation**: Install units into MekaSuit Bodyarmor (or Meka-Tool for Arcane Energy Amplification and Attunable Lens Matrix) using Mekanism's Modification Station. Helmets, leggings, and boots cannot accept arcana modules.
3. **Configuration (Module Tweaker)**: Use Mekanism's Module Tweaker to configure modules in-game:
   - **Arcane Transformer**: Set conversion rate preset (`Low`, `Normal`, `High`, `Maximum`).
   - **Attunable Lens Matrix**: Select the targeted spell school (`Fire`, `Ice`, `Lightning`, `Holy`, `Ender`, `Blood`, `Evocation`, `Nature`, `Eldritch`) and output level.
   - **Output Steps**: Arcane Energy Amplification, Attunable Lens Matrix, Rapid Casting, and Casting Stabilization can each be tuned via discrete output steps (`Off`, `1/4`, `2/4`, `3/4`, `Full`) to conserve energy.
   - *Note*: Casting Stabilization's concentration and movement perks remain active at 1+ units even if the cast reduction output step is set to zero or turned down.

## Energy Accounting & Failsafes

- Every restored mana point costs Forge Energy (FE).
- Running out of energy gracefully suspends module enhancements:
  - If energy is depleted during a cast, cast acceleration stops and the spell finishes at native speed without breaking.
  - If energy is depleted, mana regeneration reverts to Iron's default background rate and cooldowns revert to standard length.
- Bodyarmor and Meka-Tool maintain independent unit caps, output configurations, and energy storage pools.

## Server Configuration

Server configuration is automatically generated at `world/serverconfig/mekasuitarcana-server.toml`. Configuration versions are tracked automatically via `config_version`; outdated or missing configuration files are automatically backed up (`.v<old>.bak`) and cleanly migrated forward with existing operator settings preserved (including legacy key renames).

Server operators can customize:
- `config_version`: Configuration schema version integer (managed automatically).
- `enable_builtin_recipes`: Enable the mod's built-in crafting recipes (default `true`). Set to `false` for modpacks providing custom recipes (e.g. via KubeJS or CraftTweaker).
- `fe_per_mana`: FE cost per unit of mana converted.
- Max unit limits (0–4) and enabled/disabled status for each individual module type.
- Base energy costs and per-saved-tick scaling rates for cooldown reduction and cast stabilization.
- Mana capacity curve tiers (defaults: 1,000 / 4,000 / 7,000 / 10,000) and preset speed limits.
- Spell power percentage multipliers per unit (defaults: 100.0% per unit).
- **Concurrency Limits (`[limits]`)**:
  - `max_black_holes`: Maximum concurrent active Black Holes per player (default `3`; set to `0` or negative to disable).
  - `max_summons`: Maximum concurrent active summons (mobs, summoned weapons, etc.) per player (default `20`; set to `0` or negative to disable). Projectiles (arrows, missiles, firebolts, etc.) are explicitly excluded. When the cap is exceeded, the oldest active instance is cleanly dismissed so new casts succeed without interruption.

## Building

Requires Java 21 and Python 3.11 or newer. Gradle resolves compile-only Mekanism and Iron's Spellbooks dependencies from Modrinth Maven:

```bash
./gradlew build runtimeTestJar --console=plain
python tools/verify-release.py
```

On Windows, replace `./gradlew` with `.\gradlew.bat`.

**Output**: `build/libs/mekasuit-arcana-0.1.0.jar`. Install this jar on both server and client alongside Mekanism, Iron's Spellbooks, and their standard dependencies.

## Verification

- **Unit Tests**: 46 JUnit tests cover arithmetic, rate conversions, config boundaries, summon limiter FIFO tracking, config migration and backups, and tuning.
- **Runtime Verification**: A separate runtime test fixture exercises mod interactions against real pinned binaries on an isolated server thread. See [docs/RUNTIME_PROOF.md](docs/RUNTIME_PROOF.md) for details.
- **Damage Routing**: Uses standard Mekanism damage absorption pipelines for incoming magic damage.

## Project

- [Design Documentation](docs/DESIGN.md)
- [Verification & Proof](docs/RUNTIME_PROOF.md)
- [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Changelog](CHANGELOG.md)

**All Rights Reserved.** This is a public source-available repository, not an open-source license grant. See [LICENSE](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md).
