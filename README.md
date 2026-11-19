# Facets

Data-driven character traits for NeoForge 1.21.1. Players pick their traits on a selection screen when they first join
(and optionally after dying). Traits are defined in data files, so any modpack can make its own without code.

## Where content goes

The easiest place is the `facets` folder in the game directory (`.minecraft/facets`, or next to the server jar). It is
created on first launch, and everything in it applies to every world:

```
facets/
  my_traits/              a folder pack
    data/mypack/facets/trait/strong.json
    assets/mypack/lang/en_us.json   (optional, for translated names)
  some_addon.zip          a zipped pack
  data/...                files placed directly in the folder work too
```

A `pack.mcmeta` is optional. These packs are always enabled and load above mod data, so they can override traits
shipped by other mods. Regular world datapacks and data inside mod jars work as well, with the same file layout.

On a server, only the server needs the data files; clients receive everything when they join. Clients only need
the `assets` part if you use lang files for names.

**Reloading:** `/reload` or `/facets reload` reloads all trait, category and occupation files, and picks up new pack
folders. `/facets reload` also reports how much was loaded and whether any file failed. Players who still have to
pick traits (for example because nothing was loaded when they joined) get the screen right after the reload.

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
| `cost`           | `0`              | Points spent in `POINTS` mode. Negative values give points back                      |
| `category`       | none             | Category the trait is listed under (see below)                                       |
| `selectable`     | `true`           | `false` hides it from the screen; it can only be given by occupations, commands or other mods |
| `exclusive_with` | `[]`             | Traits (`mypack:weak`) or trait tags (`#mypack:strength`) that can't be held together with this one (checked both ways) |
| `requires`       | `[]`             | Traits or trait tags the player needs before taking this one                         |
| `occupations`    | `[]`             | If set, only these occupations can pick the trait                                    |
| `show_effects`   | `true`           | `false` hides the generated effect lines ("+1 Attack Damage") from the tooltip       |
| `effects`        | `[]`             | What the trait does                                                                  |
| `gain_when`      | none             | A [condition](#conditions); the player gains the trait during play once it matches   |
| `lose_when`      | none             | A condition; the player loses the trait during play once it matches                  |
| `replaces`       | `[]`             | Traits or trait tags removed when this trait is gained through `gain_when`           |

### Gaining and losing traits during play

`gain_when` and `lose_when` are checked once a second, not while the player still has to pick traits. A trait gained
this way is kept until its `lose_when` matches or a command removes it, and players get a toast for it (can be turned
off with `notifications.toasts`). These traits are usually `"selectable": false`:

```json
{
  "name": "Seasoned Fighter",
  "selectable": false,
  "gain_when": { "type": "facets:stat", "stat_type": "minecraft:custom", "stat": "minecraft:mob_kills", "at_least": 100 },
  "replaces": ["mypack:weak"],
  "effects": [{ "type": "facets:attribute", "attribute": "minecraft:generic.attack_damage", "amount": 0.5 }]
}
```

`lose_when` only takes away traits the player holds themselves, never the free traits of their occupation.

Trait tags go in `data/<namespace>/tags/facets/trait/<name>.json`, like any other tag.

Trait, category and occupation files support `neoforge:conditions`, e.g. to load a trait only when another mod is
installed:

```json
"neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "pmmo" }]
```

### Categories

Categories give traits their own lists on the screen. They go in `data/<namespace>/facets/category/<name>.json`:

```json
{ "name": "Flaws", "color": "#FF5555", "order": 1, "min": 0, "max": 2, "sort": "cost_descending" }
```

| Field   | Default  | Description                                                                     |
|---------|----------|---------------------------------------------------------------------------------|
| `name`  | required | Label shown above the list                                                      |
| `color` | none     | Color of the label and the cost numbers (`"#RRGGBB"` or a color name)           |
| `order` | `0`      | Lists are shown from lowest to highest                                          |
| `min`   | `0`      | Minimum traits a player has to pick from this category                          |
| `max`   | none     | Maximum traits a player can pick from this category                             |
| `sort`  | by mode  | `cost_descending`, `cost_ascending` or `name`. Defaults to cost in `POINTS` mode, name otherwise |

Traits without a category are listed under "Other". When there are too many categories to fit on screen, the lists
become pages with arrows to switch between them.

Without any categories, `POINTS` mode shows "Positive Traits" and "Negative Traits" lists (split by the sign of the
cost, highest values at the outer edges), and the other modes show a single list sorted by name.

### Occupations

Occupations are picked on their own screen before traits. When at least one exists, every player has to pick one.
They go in `data/<namespace>/facets/occupation/<name>.json`:

```json
{
  "name": "Knight",
  "description": "Trained in arms and armor.",
  "icon": "minecraft:iron_chestplate",
  "points": -2,
  "traits": ["mypack:armor_training"],
  "blocked": ["mypack:coward"]
}
```

| Field         | Default           | Description                                                               |
|---------------|-------------------|---------------------------------------------------------------------------|
| `name`        | required          | Display name                                                              |
| `description` | empty             | Shown next to the list                                                    |
| `icon`        | `minecraft:paper` | Item shown next to the name                                               |
| `points`      | `0`               | Added to the starting points in `POINTS` mode, can be negative            |
| `order`       | `0`               | Occupations are listed from lowest to highest, then by name               |
| `traits`      | `[]`              | Traits given for free; they show as "Free" in the build and can't be removed |
| `blocked`     | `[]`              | Traits or trait tags that can't be picked with this occupation            |

Players can check their traits at any time on the "My Traits" screen: `J` by default, or the book button in the top
left corner of the inventory screen. With JEI or EMI installed, their bookmark/favorites panel stays clear of that
button.

### Effects

`facets:attribute` adds an attribute modifier while the trait is held.

| Field       | Default     | Description                                                     |
|-------------|-------------|-----------------------------------------------------------------|
| `attribute` | required    | Attribute id, e.g. `minecraft:generic.max_health`               |
| `amount`    | required    | Modifier amount                                                 |
| `operation` | `add_value` | `add_value`, `add_multiplied_base` or `add_multiplied_total`    |

`facets:mob_effect` keeps a mob effect on the player. Milk removes it for a moment; it comes back within half a second.

| Field       | Default  | Description                                  |
|-------------|----------|----------------------------------------------|
| `effect`    | required | Mob effect id, e.g. `minecraft:night_vision` |
| `amplifier` | `0`      | `0` is level I, `1` is level II...           |
| `show_icon` | `true`   | Show the effect icon                         |
| `particles` | `false`  | Show effect particles                        |

`facets:damage` multiplies damage the player takes or deals.

| Field          | Default  | Description                                                              |
|----------------|----------|--------------------------------------------------------------------------|
| `direction`    | `taken`  | `taken` or `dealt`                                                       |
| `damage_types` | all      | A damage type, a list, or a tag like `#minecraft:is_fire`                |
| `multiplier`   | required | `0.5` halves the damage, `2` doubles it                                  |

`facets:hunger` changes hunger.

| Field        | Default | Description                                                         |
|--------------|---------|---------------------------------------------------------------------|
| `exhaustion` | `1`     | Multiplier on hunger drain from sprinting, jumping, fighting...    |
| `food`       | `1`     | Multiplier on hunger and saturation restored by eating             |

`facets:experience` multiplies experience gained: `{ "type": "facets:experience", "multiplier": 1.25 }`

`facets:conditional` runs other effects only while a condition matches (checked every half second):

```json
{
  "type": "facets:conditional",
  "condition": { "type": "facets:health", "below": 0.3 },
  "effects": [
    { "type": "facets:damage", "direction": "dealt", "multiplier": 1.3 }
  ]
}
```

### Conditions

| Type               | Fields                                   | Matches when                                          |
|--------------------|------------------------------------------|-------------------------------------------------------|
| `facets:time`      | `period`: `day` or `night`               | Night is 13000-23000 ticks, like monster spawning; never in the Nether or End |
| `facets:dimension` | `dimension`                              | The player is in that dimension                       |
| `facets:biome`     | `biomes`: a biome, a list or a tag       | The player stands in one of those biomes              |
| `facets:health`    | `below` and/or `above` (0-1)             | Health as a fraction of max health is in range        |
| `facets:in_water`  |                                          | The player is in water                                |
| `facets:all_of`    | `conditions`: list                       | All of them match                                     |
| `facets:any_of`    | `conditions`: list                       | Any of them matches                                   |
| `facets:not`       | `condition`                              | It doesn't match                                      |
| `facets:advancement` | `advancement`                          | The player has earned the advancement                 |
| `facets:stat`      | `stat_type`, `stat`, `at_least`          | A statistic reached the value, e.g. `minecraft:custom` / `minecraft:mob_kills`, or `minecraft:killed` / `minecraft:zombie`. Distances are in cm, times in ticks |
| `facets:has_trait` | `trait`: a trait or `#tag`               | The player holds the trait                            |

Tooltips describe effects and conditions automatically. Tag and dimension names come from lang keys you can add in a
resource pack, e.g. `facets.damage_tag.<namespace>.<path>`, `facets.biome_tag.<namespace>.<path>` and
`facets.dimension.<namespace>.<path>`; without one, the id is shown.

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
| `notifications.toasts`      | `true`   | Show a toast when a trait is gained or lost during play (triggers, commands, other mods) |

### Client config

`config/facets-client.toml`, also editable in-game from the mod list's Config button. Each player sets their own.

| Option                    | Default                            | Description                                              |
|---------------------------|------------------------------------|----------------------------------------------------------|
| `inventory.button`        | `true`                             | Book button on the inventory screen that opens My Traits |
| `inventory.buttonX` / `buttonY` | `4` / `4`                    | Button position from the top left corner of the screen    |
| `sounds.confirm`          | `minecraft:block.note_block.pling` | Sound when confirming traits, any sound id, empty for none |
| `sounds.confirmPitch` / `confirmVolume` | `2.0` / `1.0`        |                                                          |

## Theming

Resource packs can restyle the screens by overriding `assets/facets/theme.json`. Every key is optional; the
mod's own copy lists all of them with their defaults. Colors are `#RRGGBB` or `#AARRGGBB`:

```json
{
  "colors": { "panel_background": "#C01A1410", "title": "#FFE0A050", "cost_positive": "#FF9ACD5A" },
  "sprites": { "panel": "mypack:facets/panel"}
}
```

| Color keys | |
|---|---|
| `panel_background`, `panel_border` | List and detail boxes |
| `row_hover`, `row_hover_disabled`, `row_selected`, `row_selected_border` | List rows |
| `text`, `text_disabled`, `text_empty`, `label`, `title`, `error` | Text |
| `cost_positive`, `cost_negative`, `points_left`, `points_over` | Costs and the points line (categories with a color use their own) |
| `scrollbar`, `scrollbar_track` | Scrollbars |

Sprites (`panel`, `row_hover`, `row_selected`) replace the matching flat colors. They go in
`assets/<namespace>/textures/gui/sprites/` and can use nine-slice scaling with a `.png.mcmeta`, like vanilla GUI
sprites. `examples/facets_example_theme` is a small resource pack with a rusty color scheme.

## Commands

All require permission level 2.

- `/facets reload`: reloads all data (same as `/reload`) and reports what was loaded
- `/facets list <player>`
- `/facets add <player> <trait>` (ignores selection rules)
- `/facets remove <player> <trait>`
- `/facets reset <player>`: removes all traits and the occupation, and opens the screen
- `/facets open <player>`: opens the screen with the current occupation and traits preselected
- `/facets occupation <player> <occupation>|clear`: sets the occupation (ignores selection rules)

## Integrations

These only exist when the other mod is installed (on both server and client). Without it, a trait using one of them
is skipped with an error in the log, so packs can use `neoforge:conditions` to load such traits only when the mod is
present.

### Project MMO (2.8+)

`facets:pmmo_xp` multiplies Project MMO experience:

```json
{ "type": "facets:pmmo_xp", "skills": ["mining", "excavation"], "multiplier": 1.25 }
```

`skills` is optional; without it every skill is affected.

`facets:pmmo_level` is a condition, e.g. for `gain_when`:

```json
"gain_when": { "type": "facets:pmmo_level", "skill": "mining", "at_least": 50 }
```

### NeoOrigins (2.2+)

`facets:origins_power` grants an Origins power while the trait is held, and takes it away when the trait is lost:

```json
{ "type": "facets:origins_power", "power": "neoorigins:abyssal_aquatic_speed" }
```

Each trait grants under its own source, so the player's origin, other mods or a second trait granting the same power
don't interfere with it.

`facets:origin` is a condition: `{ "type": "facets:origin", "origin": "origins:blazeborn", "layer": "origins:origin" }`
(`layer` optional).

### KubeJS (2101.7+)

Server script events:

```js
FacetsEvents.traitAdded(event => {
  event.player.tell(`You gained ${event.trait} (${event.cause})`)
})
FacetsEvents.traitRemoved(event => { /* event.player, event.trait, event.cause */ })
FacetsEvents.occupationChanged(event => { /* event.previous, event.current (ids or null) */ })
```

`cause` is `selection`, `command`, `reset`, `trigger` or `api`.

The `Facets` binding gives scripts the Java API: `Facets.addTrait(player, 'mypack:strong')`, `Facets.removeTrait`,
`Facets.hasTrait`, `Facets.getTraits`, `Facets.setOccupation`, `Facets.openSelection`...

`facets:script` effects are handled by your own script:

```json
{ "type": "facets:script", "id": "mypack:glow", "tooltip": "Glows in the dark" }
```

```js
FacetsEvents.scriptEffect(event => {
  if (event.id != 'mypack:glow') return
  if (event.applying) event.player.addTag('glowing_trait')
  else event.player.removeTag('glowing_trait')
  // ...then react to the tag elsewhere in your scripts
})
```

`event.applying` is true when the effect is applied and false when it's removed. Applying happens on join, respawn,
reload and whenever traits change, so make it safe to run more than once.

## For mod developers

`net.tkg.facets.api.FacetsApi` reads and changes traits on the server: `getTraits`, `hasTrait`, `addTrait`,
`removeTrait`, `getOccupation`, `setOccupation`, `openSelection`, `getTrait`.

Events on the NeoForge event bus, fired after the change:

- `TraitChangeEvent.Added` and `TraitChangeEvent.Removed`: one per trait, including occupation free traits. Listen to
  these two, not to `TraitChangeEvent` itself. `getCause()` says why (`SELECTION`, `COMMAND`, `RESET`, `TRIGGER`, `API`)
- `OccupationChangeEvent`

New effect and condition types are registered with a `DeferredRegister` on `FacetsRegistries.EFFECT_TYPE` or
`FacetsRegistries.CONDITION_TYPE`, with a `MapCodec` implementing `TraitEffect` or `EffectCondition`.

## Example

Copy one of these into the `facets` folder and run `/reload` to try them (one at a time, the second one adds
occupations that every player then has to pick):

- `examples/facets_example_pack`: positive/negative traits, a trait tag, and one trait per effect type (mob effect, damage, hunger, experience, conditional), and traits gained or lost during play
- `examples/facets_example_rpg`: categories, occupations, `requires` and occupation-only traits

## Troubleshooting

A file with an error is skipped and the rest still loads. Every problem is logged, and `/facets reload` tells you
how many files failed:

```
[Facets] Couldn't load trait mypack:strong: Unknown registry key in ResourceKey[minecraft:root / minecraft:attribute]: minecraft:generic.atack_damage; No key name in MapLike[...]
[Facets] Trait mypack:heavy_hitter requires unknown mypack:swordman
[Facets] Loaded 11 traits, 2 categories, 3 occupations and 0 trait tags (1 files failed, see above)
```

`No key name` means a required field (here `name`) is missing; `Unknown registry key` and `unknown` usually mean a
typo in an id.

If the selection screen doesn't open, `/facets open <player>` says why: either `selection.mode` is `NONE`, or no
traits or occupations are loaded.
