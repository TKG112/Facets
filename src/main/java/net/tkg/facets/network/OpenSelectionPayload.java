package net.tkg.facets.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;
import net.tkg.facets.trait.SelectionRules;

import java.util.List;

/** Server to client: open the selection screen with the given rules and preselected traits. */
public record OpenSelectionPayload(SelectionRules rules, List<ResourceLocation> current) implements CustomPacketPayload {
    public static final Type<OpenSelectionPayload> TYPE = new Type<>(Facets.id("open_selection"));

    public static final StreamCodec<ByteBuf, OpenSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            SelectionRules.STREAM_CODEC, OpenSelectionPayload::rules,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenSelectionPayload::current,
            OpenSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
