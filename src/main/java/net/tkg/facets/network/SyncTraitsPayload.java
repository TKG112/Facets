package net.tkg.facets.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;

import java.util.List;
import java.util.Optional;

/**
 * Server to client: the player's occupation and every trait they hold, including the occupation's
 * free traits. Sent whenever traits are (re)applied.
 */
public record SyncTraitsPayload(Optional<ResourceLocation> occupation, List<ResourceLocation> traits)
        implements CustomPacketPayload {
    public static final Type<SyncTraitsPayload> TYPE = new Type<>(Facets.id("sync_traits"));

    public static final StreamCodec<ByteBuf, SyncTraitsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), SyncTraitsPayload::occupation,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncTraitsPayload::traits,
            SyncTraitsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
