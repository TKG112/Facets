package net.tkg.facets.effect;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formatting shared by effect tooltips. */
final class EffectTooltips {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private EffectTooltips() {
    }

    /** A multiplier as a signed percentage: 1.5 becomes "+50%", 0.75 becomes "-25%". */
    static String percent(double multiplier) {
        double change = (multiplier - 1) * 100;
        return (change >= 0 ? "+" : "") + FORMAT.format(change) + "%";
    }

    /** Blue when the change helps the player, red when it hurts, like attribute tooltips. */
    static MutableComponent colored(MutableComponent line, boolean good) {
        return line.withStyle(good ? ChatFormatting.BLUE : ChatFormatting.RED);
    }

    static Component percentLine(String key, double multiplier, boolean higherIsBetter, Object... extraArgs) {
        Object[] args = new Object[extraArgs.length + 1];
        args[0] = percent(multiplier);
        System.arraycopy(extraArgs, 0, args, 1, extraArgs.length);
        return colored(Component.translatable(key, args), (multiplier >= 1) == higherIsBetter);
    }
}
