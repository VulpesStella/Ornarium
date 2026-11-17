# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added

- Show the slot name and number in equipped accessory tooltips.
- List compatible slot types in unequipped item tooltips.
- Add Trinkets icons to the built-in amulet, ring, and belt slots.
- Add namespaced slot registration, server-configurable counts, and Ore Dictionary matching.
- Add paginated accessory inventory screens and cross-page quick equip.
- Add a client API for rendering equipped accessories on player models in third person.
- Add an amulet slot, two ring slots, and a belt slot for Forge 1.6.4.
- Add a configurable key to open the accessory inventory, with shift-click transfers.
- Save equipment with player data and respect the world's keepInventory rule.
- Expose an accessory API with equip, unequip, and worn-tick hooks.

### Changed

- Replace enum-based accessory matching with a collection of slot type IDs.
- Save equipment by slot type and index, migrate classic four-slot saves, and relocate equipment when a slot becomes unavailable.
