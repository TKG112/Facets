package net.tkg.facets.player;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.tkg.facets.Facets;

import java.util.function.Supplier;

public final class FacetsAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Facets.MODID);

    public static final Supplier<AttachmentType<PlayerFacets>> PLAYER_FACETS = ATTACHMENT_TYPES.register(
            "player_facets",
            () -> AttachmentType.builder(() -> PlayerFacets.DEFAULT)
                    .serialize(PlayerFacets.CODEC)
                    .copyOnDeath()
                    .build());

    private FacetsAttachments() {
    }
}
