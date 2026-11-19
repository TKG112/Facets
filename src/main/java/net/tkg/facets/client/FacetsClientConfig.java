package net.tkg.facets.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Client config (config/facets-client.toml), editable from the mod list. Each player picks their own. */
public final class FacetsClientConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue INVENTORY_BUTTON;
    public static final ModConfigSpec.IntValue INVENTORY_BUTTON_X;
    public static final ModConfigSpec.IntValue INVENTORY_BUTTON_Y;

    public static final ModConfigSpec.ConfigValue<String> CONFIRM_SOUND;
    public static final ModConfigSpec.DoubleValue CONFIRM_SOUND_PITCH;
    public static final ModConfigSpec.DoubleValue CONFIRM_SOUND_VOLUME;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("inventory");
        INVENTORY_BUTTON = builder
                .comment("Show a button in the inventory that opens My Traits.",
                        "JEI and EMI keep their bookmark/favorites panel clear of it.")
                .define("button", true);
        INVENTORY_BUTTON_X = builder
                .comment("Button position from the top left corner of the screen, in pixels.")
                .defineInRange("buttonX", 4, 0, 10000);
        INVENTORY_BUTTON_Y = builder
                .defineInRange("buttonY", 4, 0, 10000);
        builder.pop();

        builder.push("sounds");
        CONFIRM_SOUND = builder
                .comment("Sound played when you confirm your traits. Any sound id works, empty for none.")
                .define("confirm", "minecraft:block.note_block.pling",
                        value -> value instanceof String s && (s.isEmpty() || ResourceLocation.tryParse(s) != null));
        CONFIRM_SOUND_PITCH = builder
                .defineInRange("confirmPitch", 2.0, 0.5, 2.0);
        CONFIRM_SOUND_VOLUME = builder
                .defineInRange("confirmVolume", 1.0, 0.0, 1.0);
        builder.pop();

        SPEC = builder.build();
    }

    private FacetsClientConfig() {
    }
}
