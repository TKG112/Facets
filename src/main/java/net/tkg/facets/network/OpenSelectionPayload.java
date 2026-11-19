package net.tkg.facets.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;
import net.tkg.facets.trait.SelectionRules;

import java.util.List;
import java.util.Optional;

/**
 * Server to client: open the selection screen.
 *
 * @param current    selectable traits to preselect
 * @param occupation occupation to preselect
 * @param granted    traits the player holds outside the selection (commands, other mods)
 */
public record OpenSelectionPayload(SelectionRules rules, List<ResourceLocation> current, Optional<ResourceLocation> occupation,
                                   List<ResourceLocation> granted) implements CustomPacketPayload {
    public static final Type<OpenSelectionPayload> TYPE = new Type<>(Facets.id("open_selection"));

    public static final StreamCodec<ByteBuf, OpenSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            SelectionRules.STREAM_CODEC, OpenSelectionPayload::rules,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenSelectionPayload::current,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), OpenSelectionPayload::occupation,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenSelectionPayload::granted,
            OpenSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
