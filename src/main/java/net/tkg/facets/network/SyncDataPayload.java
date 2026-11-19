package net.tkg.facets.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tkg.facets.Facets;
import net.tkg.facets.data.TraitData;

/** Server to client: all loaded traits, categories, occupations and tags. Sent on join and after every reload. */
public record SyncDataPayload(TraitData data) implements CustomPacketPayload {
    public static final Type<SyncDataPayload> TYPE = new Type<>(Facets.id("sync_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncDataPayload> STREAM_CODEC =
            TraitData.STREAM_CODEC.map(SyncDataPayload::new, SyncDataPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
