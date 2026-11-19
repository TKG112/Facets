package net.tkg.facets.config;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.tkg.facets.trait.SelectionMode;

/**
 * Server config (world/serverconfig/facets-server.toml). Modpacks can ship defaults in
 * defaultconfigs/facets-server.toml.
 */
public final class FacetsConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.EnumValue<SelectionMode> SELECTION_MODE;
    public static final ModConfigSpec.IntValue STARTING_POINTS;
    public static final ModConfigSpec.IntValue MIN_TRAITS;
    public static final ModConfigSpec.IntValue MAX_TRAITS;

    public static final ModConfigSpec.BooleanValue OPEN_ON_FIRST_JOIN;
    public static final ModConfigSpec.EnumValue<DeathBehavior> ON_DEATH;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("selection");
        SELECTION_MODE = builder
                .comment("How trait selection is limited.",
                        " POINTS - players get a point budget, traits spend or give back points (\"cost\" in the trait file)",
                        " COUNT  - players pick between minTraits and maxTraits traits, costs are ignored",
                        " FREE   - players pick any traits they want, only exclusivity is checked",
                        " NONE   - no selection screen, traits only come from commands or other mods")
                .defineEnum("mode", SelectionMode.POINTS);
        STARTING_POINTS = builder
                .comment("Points every player starts with. Only used in POINTS mode.")
                .defineInRange("startingPoints", 0, -10000, 10000);
        MIN_TRAITS = builder
                .comment("Minimum number of traits a player has to pick. Only used in COUNT mode.")
                .defineInRange("minTraits", 0, 0, 1000);
        MAX_TRAITS = builder
                .comment("Maximum number of traits a player can pick. Only used in COUNT mode.")
                .defineInRange("maxTraits", 3, 0, 1000);
        builder.pop();

        builder.push("triggers");
        OPEN_ON_FIRST_JOIN = builder
                .comment("Open the selection screen the first time a player joins the world.")
                .define("openOnFirstJoin", true);
        ON_DEATH = builder
                .comment("What happens to a player's traits when they die.",
                        " KEEP   - traits are kept, no screen",
                        " RESET  - traits are removed and the player picks again from scratch",
                        " REPICK - the screen opens with the previous traits already selected")
                .defineEnum("onDeath", DeathBehavior.KEEP);
        builder.pop();

        SPEC = builder.build();
    }

    private FacetsConfig() {
    }
}
