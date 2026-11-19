# Facets

Data-driven character traits for NeoForge 1.21.1. Players pick their traits on a selection screen when they first join
(and optionally after dying). Traits are defined in datapacks, so any modpack can make its own without code.

## Traits

Traits go in `data/<namespace>/facets/trait/<name>.json`:

```json
{
  "name": { "translate": "trait.mypack.strong", "fallback": "Strong" },
  "description": "Hits harder with every weapon.",
  "icon": "minecraft:iron_sword",
  "cost": 4,
  "selectable": true,
  "exclusive_with": ["mypack:weak"],
  "effects": [
    { "type": "facets:attribute", "attribute": "minecraft:generic.attack_damage", "amount": 1.0, "operation": "add_value" }
  ]
}
```

| Field            | Default          | Description                                                                          |
|------------------|------------------|--------------------------------------------------------------------------------------|
| `name`           | required         | Text component (a plain string or `{"translate": ...}`)                              |
| `description`    | empty            | Shown in the tooltip                                                                 |
| `icon`           | `minecraft:paper`| Item shown next to the name                                                          |
| `cost`           | `0`              | Points spent in `POINTS` mode. Negative values give points back. Traits with a negative cost go in the "Negative Traits" list on the screen, the rest in "Positive Traits". If no trait has a negative cost, a single list is shown |
| `selectable`     | `true`           | `false` hides it from the screen; it can only be given with commands or by other mods |
| `exclusive_with` | `[]`             | Traits that can't be held together with this one (checked both ways)                 |
| `effects`        | `[]`             | What the trait does                                                                  |

### Effects

`facets:attribute` adds an attribute modifier while the trait is held.

| Field       | Default     | Description                                                     |
|-------------|-------------|-----------------------------------------------------------------|
| `attribute` | required    | Attribute id, e.g. `minecraft:generic.max_health`               |
| `amount`    | required    | Modifier amount                                                 |
| `operation` | `add_value` | `add_value`, `add_multiplied_base` or `add_multiplied_total`    |

## Config

`world/serverconfig/facets-server.toml` (ship defaults in `defaultconfigs/facets-server.toml`):

| Option                      | Default  | Description                                                    |
|-----------------------------|----------|----------------------------------------------------------------|
| `selection.mode`            | `POINTS` | `POINTS`, `COUNT`, `FREE` or `NONE`                            |
| `selection.startingPoints`  | `0`      | Point budget in `POINTS` mode                                  |
| `selection.minTraits`       | `0`      | Minimum traits in `COUNT` mode                                 |
| `selection.maxTraits`       | `3`      | Maximum traits in `COUNT` mode                                 |
| `triggers.openOnFirstJoin`  | `true`   | Open the screen on a player's first join                       |
| `triggers.onDeath`          | `KEEP`   | `KEEP`, `RESET` (pick again from scratch) or `REPICK` (pick again, previous traits preselected) |

## Commands

All require permission level 2.

- `/facets list <player>`
- `/facets add <player> <trait>` (ignores selection rules)
- `/facets remove <player> <trait>`
- `/facets reset <player>`: removes all traits and opens the screen
- `/facets open <player>`: opens the screen with the current traits preselected

## Example

`examples/facets_example_pack` has a few sample traits. Copy it into a world's `datapacks` folder to try them.
