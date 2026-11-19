package net.tkg.facets.trait;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public enum SelectionMode {
    /** Players start with a point budget; traits spend or refund points. */
    POINTS,
    /** Players pick between a minimum and maximum number of traits; costs are ignored. */
    COUNT,
    /** Players pick anything they want; only exclusivity applies. */
    FREE,
    /** No selection screen. Traits only come from commands or other mods. */
    NONE;

    public static final StreamCodec<ByteBuf, SelectionMode> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Enum::ordinal);
}
