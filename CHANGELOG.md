# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0-1.21.1] - 2026-09-16

### Added
- **Arcane Transformer Unit** (`mana_conversion_unit`): Converts MekaSuit Forge Energy into Iron's Spellbooks player mana pool across four speed presets (Low, Normal, High, Maximum) and four unit capacity tiers (1,000 to 10,000 max mana).
- **Rapid Casting Unit** (`cooldown_reduction_unit`): Accelerates spell cooldowns using MekaSuit FE, reducing active cooldowns tick-by-tick with energy costs scaled per saved tick.
- **Casting Stabilization Unit** (`casting_stabilization_unit`): Eliminates casting movement penalties, accelerates spell casting time, and grants cast concentration (uninterruptible casting matching the Concentration Amulet).
- **Arcane Energy Amplification Unit** (`amplification_unit`): Boosts overall spell power (+100% per installed unit, up to +400% total) powered by MekaSuit FE.
- **Attunable Lens Matrix Unit** (`focus_unit`): Meka-Tool module providing school-specific spell power and attunement (+100% per unit, up to +400% total).
- **Summon & Black Hole Concurrency Limits** (`[limits]`): Configurable caps to prevent server/client degradation (default max 3 Black Holes, max 20 general summons) with FIFO eviction of oldest instances and projectile exclusion.
- **Automated Server Configuration Migration & Backup**: Server configuration version tracking (`config_version = 1`), automatic `.bak` backups before migration, and remapping for legacy config keys.
- **Configurable Recipe Toggle** (`enable_builtin_recipes`): Server config option and `neoforge:conditions` rule to disable built-in recipes for modpacks providing custom KubeJS/CraftTweaker recipes.
- **Endgame Crafting Recipes**: Thematic recipes using Mithril Ingots, Mithril Weave, Atomic Alloys, Ultimate Control Circuits, and Iron's Spells reagents (Orbs, Concentration Amulet, Arcane Salvage, Affinity Ring).

### Changed
- Aligned module naming, tooltips, and balance caps to Mekanism conventions and Iron's Spellbooks progression.

[Unreleased]: https://github.com/codecraft3r/mekasuit-arcana/compare/v0.1.0-1.21.1...HEAD
[0.1.0-1.21.1]: https://github.com/codecraft3r/mekasuit-arcana/releases/tag/v0.1.0-1.21.1
