package net.tkg.facets.client.theme;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Colors and optional sprites for the Facets screens, loaded from assets/facets/theme.json.
 * Resource packs override that file to restyle everything; keys they leave out keep the default.
 * <pre>
 * { "colors": { "panel_background": "#A0000000", "cost_positive": "#55FF55" },
 *   "sprites": { "panel": "mypack:facets/panel" } }
 * </pre>
 * Sprites go in assets/&lt;namespace&gt;/textures/gui/sprites/ and can use nine-slice scaling through
 * a .png.mcmeta file, like vanilla GUI sprites. A sprite replaces the matching flat color.
 */
public record FacetsTheme(Map<String, Integer> colors, Map<String, ResourceLocation> sprites) {
    public static final String PANEL_BACKGROUND = "panel_background";
    public static final String PANEL_BORDER = "panel_border";
    public static final String ROW_HOVER = "row_hover";
    public static final String ROW_HOVER_DISABLED = "row_hover_disabled";
    public static final String ROW_SELECTED = "row_selected";
    public static final String ROW_SELECTED_BORDER = "row_selected_border";
    public static final String TEXT = "text";
    public static final String TEXT_DISABLED = "text_disabled";
    public static final String TEXT_EMPTY = "text_empty";
    public static final String LABEL = "label";
    public static final String TITLE = "title";
    public static final String ERROR = "error";
    public static final String COST_POSITIVE = "cost_positive";
    public static final String COST_NEGATIVE = "cost_negative";
    public static final String POINTS_LEFT = "points_left";
    public static final String POINTS_OVER = "points_over";
    public static final String SCROLLBAR_TRACK = "scrollbar_track";
    public static final String SCROLLBAR = "scrollbar";

    /** Sprite keys */
    public static final String SPRITE_PANEL = "panel";
    public static final String SPRITE_ROW_HOVER = "row_hover";
    public static final String SPRITE_ROW_SELECTED = "row_selected";

    private static final Map<String, Integer> DEFAULT_COLORS = Map.ofEntries(
            Map.entry(PANEL_BACKGROUND, 0xA0000000),
            Map.entry(PANEL_BORDER, 0xFF555555),
            Map.entry(ROW_HOVER, 0x30FFFFFF),
            Map.entry(ROW_HOVER_DISABLED, 0x18FFFFFF),
            Map.entry(ROW_SELECTED, 0x50FFFFFF),
            Map.entry(ROW_SELECTED_BORDER, 0xFFFFFFFF),
            Map.entry(TEXT, 0xFFFFFFFF),
            Map.entry(TEXT_DISABLED, 0xFF707070),
            Map.entry(TEXT_EMPTY, 0xFF808080),
            Map.entry(LABEL, 0xFFA0A0A0),
            Map.entry(TITLE, 0xFFFFFFFF),
            Map.entry(ERROR, 0xFFFF5555),
            Map.entry(COST_POSITIVE, 0xFF55FF55),
            Map.entry(COST_NEGATIVE, 0xFFFF5555),
            Map.entry(POINTS_LEFT, 0xFF55FF55),
            Map.entry(POINTS_OVER, 0xFFFF5555),
            Map.entry(SCROLLBAR_TRACK, 0xFF202020),
            Map.entry(SCROLLBAR, 0xFF909090));

    public static final FacetsTheme DEFAULT = new FacetsTheme(DEFAULT_COLORS, Map.of());

    /** "#RRGGBB" (opaque) or "#AARRGGBB". */
    public static final Codec<Integer> COLOR = Codec.STRING.comapFlatMap(FacetsTheme::parseColor, FacetsTheme::formatColor);

    public static final Codec<FacetsTheme> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, COLOR).optionalFieldOf("colors", Map.of()).forGetter(FacetsTheme::colors),
            Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC).optionalFieldOf("sprites", Map.of()).forGetter(FacetsTheme::sprites)
    ).apply(instance, FacetsTheme::new));

    private static volatile FacetsTheme current = DEFAULT;

    public static FacetsTheme current() {
        return current;
    }

    static void set(FacetsTheme theme) {
        current = theme;
    }

    /** This theme with the default colors filled in for any key it leaves out. */
    FacetsTheme withDefaults() {
        Map<String, Integer> merged = new HashMap<>(DEFAULT_COLORS);
        merged.putAll(colors);
        return new FacetsTheme(Map.copyOf(merged), Map.copyOf(sprites));
    }

    static boolean isKnownColor(String key) {
        return DEFAULT_COLORS.containsKey(key);
    }

    public int color(String key) {
        Integer value = colors.get(key);
        return value != null ? value : DEFAULT_COLORS.getOrDefault(key, 0xFFFFFFFF);
    }

    @Nullable
    public ResourceLocation sprite(String key) {
        return sprites.get(key);
    }

    /** Fills a box with the sprite if the theme sets one, otherwise with the color. */
    public void fill(GuiGraphics graphics, String spriteKey, String colorKey, int x, int y, int width, int height) {
        ResourceLocation sprite = sprite(spriteKey);
        if (sprite != null) {
            graphics.blitSprite(sprite, x, y, width, height);
        } else {
            int color = color(colorKey);
            if ((color >>> 24) != 0) {
                graphics.fill(x, y, x + width, y + height, color);
            }
        }
    }

    private static DataResult<Integer> parseColor(String value) {
        String hex = value.startsWith("#") ? value.substring(1) : value;
        try {
            if (hex.length() == 6) {
                return DataResult.success(0xFF000000 | Integer.parseUnsignedInt(hex, 16));
            }
            if (hex.length() == 8) {
                return DataResult.success((int) Long.parseLong(hex, 16));
            }
        } catch (NumberFormatException ignored) {
            // Falls through to the error below
        }
        return DataResult.error(() -> "Invalid color " + value + ", expected #RRGGBB or #AARRGGBB");
    }

    private static String formatColor(int color) {
        return String.format("#%08X", color);
    }
}
