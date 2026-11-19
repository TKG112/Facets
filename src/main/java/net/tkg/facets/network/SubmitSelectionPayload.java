package net.tkg.facets.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;

import java.util.List;
import java.util.Optional;

/** Client to server: the occupation and traits picked on the screen. Always validated on the server. */
public record SubmitSelectionPayload(Optional<ResourceLocation> occupation, List<ResourceLocation> traits)
        implements CustomPacketPayload {
    public static final Type<SubmitSelectionPayload> TYPE = new Type<>(Facets.id("submit_selection"));

    public static final StreamCodec<ByteBuf, SubmitSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SubmitSelectionPayload::occupation,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list(256)), SubmitSelectionPayload::traits,
            SubmitSelectionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
