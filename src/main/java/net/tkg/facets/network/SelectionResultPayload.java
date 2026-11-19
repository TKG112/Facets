package net.tkg.facets.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.tkg.facets.Facets;

/** Server to client: whether a submitted selection was accepted, with the reason if it wasn't. */
public record SelectionResultPayload(boolean accepted, Component message) implements CustomPacketPayload {
    public static final Type<SelectionResultPayload> TYPE = new Type<>(Facets.id("selection_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectionResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SelectionResultPayload::accepted,
            ComponentSerialization.TRUSTED_STREAM_CODEC, SelectionResultPayload::message,
            SelectionResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
