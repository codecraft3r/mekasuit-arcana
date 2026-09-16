# MekaSuit Arcana

<img src="src/main/resources/icon.png" alt="MekaSuit Arcana Icon" width="96" align="right" />

[![CI](https://github.com/H-H-E/mekasuit-arcana/actions/workflows/ci.yml/badge.svg)](https://github.com/H-H-E/mekasuit-arcana/actions/workflows/ci.yml)

**Early preview.** 36 unit tests and 16 isolated server assertions have passed.
Connected-client GUI and full-modpack playtesting remain unverified.

Minecraft 1.21.1 / NeoForge 21.1.248 integration for Mekanism 10.7.19.85 and
Iron's Spells 'n Spellbooks 3.16.3. Java 21 required.

Five Mekanism modules add powered magic support to MekaSuit Bodyarmor.
Amplification and Focus Units also work on a held Meka-Tool using staff-style
attribute bonuses. Use your existing spellbook and Iron's normal casting controls.

| Unit | Install cap | Maximum contribution per carrier |
| --- | ---: | --- |
| Mana Conversion | 4 | Mana capacity curve 1000 / 4000 / 7000 / 10000 and FE-to-mana conversion |
| Amplification | 4 | +400% global spell-power rating (+100% per unit) |
| Focus | 4 | +400% spell-power rating in one selected school (+100% per unit) |
| Cooldown Reduction | 4 | 25% spell cooldown reduction per unit (100% / no cooldown at 4 units) |
| Casting Stabilization | 4 | 25% cast time reduction per unit (100% / instant cast at 4 units); uninterruptible casting (concentration) and full casting movement at 1+ units |

Cooldown Reduction provides direct linear spell cooldown reduction (25% per installed unit,
reaching complete cooldown elimination at 4 units). Casting Stabilization linearly reduces spell
cast duration (25% per installed unit, reaching instant cast at 4 units), removes the casting
movement penalty at 1+ units, and grants uninterruptible casting concentration (identical to the
Amulet of Concentration) when 1 or more units are installed and powered.

## Using the modules

Craft the modules, install them through Mekanism's Modification Station, then use
the Module Tweaker to enable them, select a Focus school, adjust output, or limit
mana-conversion power. Helmets, leggings and boots cannot accept these modules.
The Meka-Tool accepts only Amplification and Focus.

Bodyarmor and tool retain independent unit caps, output settings, Focus selections,
and energy accounts. Their eligible attribute contributions add together.

Every restored mana point costs FE. Amplification charges once per Iron's cast
event; Focus charges only on events from its selected school. Iron's channeled
spells emit repeated cast events, so their pulses are billed separately.
Cooldown Reduction drains energy proportional to cooldown time saved when spells
are cast, plus a base slot fee. Casting Stabilization drains energy proportional
to cast duration saved, plus a fixed FE load while casting. Lower output reduces
reduction percentages and their associated energy costs. Stabilization's concentration
and movement benefits remain active at 1+ units even at zero output step.

Acceleration is purchased as the native timers advance, so running out of power
stops further acceleration. Casting itself continues. Continuous spells retain
Iron's normal channel duration and pulse spacing; the module improves movement
during those channels.

Mana conversion uses Iron's existing mana pool and ordinary regeneration remains
active. Each installed converter raises the conversion throughput available at a
chosen preset. The mana capacity curve describes an otherwise unmodified player's
capacity; other equipment can still contribute its own modifiers.

## Building

Requires Java 21 and Python 3.11 or newer. Gradle resolves the pinned
compile-only Mekanism and Iron's Spellbooks artifacts from Modrinth Maven;
neither is bundled or committed:

    ./gradlew build runtimeTestJar --console=plain
    python tools/verify-release.py

On Windows replace `./gradlew` with `.\gradlew.bat`.

Output: build/libs/mekasuit-arcana-0.1.0.jar. Install this jar on both server and
client alongside Mekanism, Iron's Spellbooks and their normal dependencies.

Server balance configuration is generated under the world's serverconfig directory
as mekasuitarcana-server.toml. Module output selections are stored on the carrier
through Mekanism's own module config system.

## Verification

Plain JUnit tests cover balance and accounting arithmetic. The separate runtime
harness exercises the production code against the real pinned mods in an isolated
dedicated server. See docs/RUNTIME_PROOF.md for the actual results and remaining
client-side checks. A runtime-harness jar is for development only.

Damage routing uses the mods' existing behavior. No extra ward module is included.

## Project

- [Design and balance](docs/DESIGN.md)
- [Verification and remaining checks](docs/RUNTIME_PROOF.md)
- [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [Changelog](CHANGELOG.md)

**All Rights Reserved.** This is a public source-available repository, not an
open-source license grant. See [LICENSE](LICENSE) and [third-party notices](THIRD_PARTY_NOTICES.md).
