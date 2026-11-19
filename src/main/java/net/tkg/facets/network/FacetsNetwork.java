package net.tkg.facets.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.tkg.facets.client.FacetsClient;
import net.tkg.facets.trait.TraitManager;

public final class FacetsNetwork {
    private static final String VERSION = "1";

    private FacetsNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);

        // Client handlers go through lambdas so FacetsClient is only loaded on the client
        registrar.playToClient(OpenSelectionPayload.TYPE, OpenSelectionPayload.STREAM_CODEC,
                (payload, context) -> FacetsClient.openSelection(payload));
        registrar.playToClient(SelectionResultPayload.TYPE, SelectionResultPayload.STREAM_CODEC,
                (payload, context) -> FacetsClient.handleResult(payload));
        registrar.playToClient(TraitNotifyPayload.TYPE, TraitNotifyPayload.STREAM_CODEC,
                (payload, context) -> FacetsClient.notifyTrait(payload));
        registrar.playToClient(SyncDataPayload.TYPE, SyncDataPayload.STREAM_CODEC,
                (payload, context) -> FacetsClient.syncData(payload));
        registrar.playToClient(SyncTraitsPayload.TYPE, SyncTraitsPayload.STREAM_CODEC,
                (payload, context) -> FacetsClient.syncTraits(payload));

        registrar.playToServer(SubmitSelectionPayload.TYPE, SubmitSelectionPayload.STREAM_CODEC,
                (payload, context) -> {
                    if (context.player() instanceof ServerPlayer player) {
                        TraitManager.submit(player, payload.occupation().orElse(null), payload.traits());
                    }
                });
    }
}
