# Kuznec

`Kuznec` is a Paper 1.16.5 plugin that adds a blacksmith GUI with upgrade menus, Vault-based purchases, configurable decorative layouts, and upgrade effects bound to the exact item a player improves.

The repository is written and documented in English, while the plugin itself is configured in Russian by default.

## Features

- Main `/kuznec` menu with 54 slots
- Slot `20` uses the item from the player's hand, except armor
- Separate upgrade menus for weapons and armor pieces
- Vault economy support for buying upgrades
- Upgrade effects stored directly on the improved item
- Purchased upgrades appended to item lore
- Separate config files for GUI, messages, and effects
- Decorative filler items configurable independently for main and upgrade menus

## Requirements

- Java 8
- Maven 3.8+
- Paper 1.16.5
- Vault
- Any Vault-compatible economy plugin

## Build

```bash
mvn clean package
```

The compiled jar will be available in:

```text
target/kuznec-1.0.0.jar
```

## Installation

1. Build the plugin or use the packaged jar.
2. Put the jar into your server `plugins` folder.
3. Install `Vault` and an economy plugin.
4. Start the server once to generate plugin files.
5. Edit the configs if needed.
6. Restart the server.

## Commands

- `/kuznec` - opens the blacksmith menu

## Configuration

The plugin uses three separate config files:

- `gui.yml` - menu titles, slots, filler items, button items, lore formatting
- `messages.yml` - player messages and sounds
- `effects.yml` - passive refresh timing and all upgrade definitions

## Project Structure

```text
src/main/java/dev/mark/kuznec
├── command
├── config
├── gui
├── upgrade
├── util
└── vault
```

## Notes

- Upgrade names, prices, descriptions, and effects are fully configurable.
- The plugin prevents taking decorative GUI items out of menus.
- Unsupported items in the hand are shown in the menu, but they do not open an upgrade menu.
- The plugin is configured for Russian in-game text out of the box.
