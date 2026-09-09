# Storage Central

![Storage Central demo](storage-central.gif)

A Minecraft 1.21.1 **NeoForge** mod that adds a **Storage Terminal** — a block that scans nearby chunks for any storage container and lets you access, search, and manage all of your items from a single, fast interface.

> Currently in development. Tested in single-player and on LAN/dedicated servers.

## Features

- **Storage Terminal block** — place it down and open a searchable interface that aggregates the contents of every inventory-bearing block in range (chests, barrels, furnaces, hoppers, and more).
- **Click-to-extract & deposit** — click an item in the terminal to pull it into your inventory (shift-click for a full stack), or shift-click items in your inventory to push them into the network.
- **Live search** — type to filter items by name or ID.
- **Same feel as a double chest** — the terminal renders with the familiar double-chest window and works like a normal container.
- **Range upgrades** — six tiers that expand the scan radius:
  - Tier 0 → 1×1 chunks (base) - no upgrade needed
  - Tier 1 → 3×3 chunks
  - Tier 2 → 5×5 chunks
  - Tier 3 → 7×7 chunks
  - Tier 4 → 9×9 chunks
  - Tier 5 → 11×11 chunks (max)
- **Server-aware range limits** — tiers are automatically capped so the scan radius never exceeds the server's `view-distance`/`simulation-distance` from `server.properties`, and can be further limited in a config file.
- **Fast scanning** — the terminal only scans actual inventory-bearing block entities, so it stays responsive even with many containers nearby.
- **Server-authoritative** — works in single-player, LAN, and dedicated servers; no client-side hacks.
- **Aggregate reads** — the terminal shows the total count of each item across the whole network.

## Requirements

- **Minecraft**: 1.21.1
- **NeoForge**: 21.1.235 or later

## Installation

1. Install [NeoForge](https://neoforged.net/) for Minecraft 1.21.1.
2. Place the `storage_central-1.26.9.9.jar` from the [Releases](https://github.com/RetiredRoca/storage-central/releases) page into your `mods/` folder.
3. Launch the game.

## Building from source

Requires **Java 21** (auto-provisioned by the Gradle toolchain).

```bash
./gradlew build
```

The built mod JAR will be at `build/libs/storage_central-1.26.9.9.jar`.

> Note: run `build` only. Do **not** run `runClient`/`runServer` during development if you prefer to test via a launcher (e.g. Prism Launcher) pointing at an existing installation.

## Usage

1. Craft the **Storage Terminal** and place it somewhere with your storage nearby.
2. **Right-click** the terminal to open the interface.
3. Browse/search the aggregated contents.
4. **Click** an item to take one, **shift-click** to take a full stack.
5. **Shift-click** items from your own inventory to deposit them into the network.
6. Craft and apply a **Range Upgrade** (right-click it on the terminal) to increase scan range.

## Recipes

Recipes for the terminal and the five range upgrades are included in `src/main/resources/data/storage_central/recipe/`.

## Configuration

On servers, the max tier can be limited via `config/storage_central-server.toml` (generated on first run):

- `maxTier` — hard cap on the highest tier that may be applied (default `5`).
- The effective tier is also capped automatically by the smaller of the server's `view-distance` and `simulation-distance` from `server.properties`, so upgrades never scan further than chunks are actually generated/loaded.

## Configuration / Credits

- **Mod ID**: `storage_central`
- **Package**: `com.retiredroca.storagecentral`
- **Server config**: `config/storage_central-server.toml`

*Built with the [NeoForge MDK](https://github.com/neoforged/MDK). Minecraft and NeoForge are property of their respective owners.*
