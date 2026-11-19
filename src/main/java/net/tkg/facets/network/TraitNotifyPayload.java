package net.tkg.facets.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;

/** Server to client: show a toast for a trait the player gained or lost. */
public record TraitNotifyPayload(ResourceLocation trait, boolean gained) implements CustomPacketPayload {
    public static final Type<TraitNotifyPayload> TYPE = new Type<>(Facets.id("trait_notify"));

    public static final StreamCodec<ByteBuf, TraitNotifyPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, TraitNotifyPayload::trait,
            ByteBufCodecs.BOOL, TraitNotifyPayload::gained,
            TraitNotifyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
