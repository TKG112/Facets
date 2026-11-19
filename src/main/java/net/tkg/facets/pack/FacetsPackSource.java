package net.tkg.facets.pack;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.tkg.facets.Facets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Loads content packs from the "facets" folder in the game directory (.minecraft/facets, or next to
 * the server jar), for every world:
 * <pre>
 * facets/
 *   my_traits/data/...       a folder pack
 *   some_addon.zip           a zipped pack
 *   data/...                 data placed directly in the folder also works
 * </pre>
 * Packs are always enabled and sit above mod data, so they can override traits shipped by mods. A
 * pack.mcmeta is optional. assets/ in the same packs load as resource packs, e.g. for lang files.
 */
@EventBusSubscriber(modid = Facets.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FacetsPackSource implements RepositorySource {
    public static final String FOLDER = "facets";
    private static final PackSource SOURCE = PackSource.create(
            name -> Component.translatable("pack.nameAndSource", name, Component.translatable("facets.pack.source"))
                    .withStyle(ChatFormatting.GRAY), true);
    private static final PackSelectionConfig SELECTION = new PackSelectionConfig(true, Pack.Position.TOP, false);

    private final PackType type;

    private FacetsPackSource(PackType type) {
        this.type = type;
    }

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        event.addRepositorySource(new FacetsPackSource(event.getPackType()));
    }

    public static Path folder() {
        return FMLPaths.GAMEDIR.get().resolve(FOLDER);
    }

    @Override
    public void loadPacks(Consumer<Pack> output) {
        Path folder = folder();
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            Facets.LOGGER.error("Couldn't create the {} folder", folder, e);
            return;
        }

        String content = type == PackType.SERVER_DATA ? "data" : "assets";
        if (Files.isDirectory(folder.resolve(content))) {
            addPack(output, "facets", "facets", new PathPackResources.PathResourcesSupplier(folder));
        }

        try (Stream<Path> children = Files.list(folder)) {
            for (Path child : children.sorted().toList()) {
                String name = child.getFileName().toString();
                if (Files.isDirectory(child)) {
                    if (name.equals("data") || name.equals("assets")) {
                        continue;
                    }
                    if (Files.isDirectory(child.resolve(content)) || Files.isRegularFile(child.resolve("pack.mcmeta"))) {
                        addPack(output, "facets/" + name, name, new PathPackResources.PathResourcesSupplier(child));
                    }
                } else if (name.endsWith(".zip")) {
                    addPack(output, "facets/" + name, name.substring(0, name.length() - 4), new FilePackResources.FileResourcesSupplier(child));
                }
            }
        } catch (IOException e) {
            Facets.LOGGER.error("Couldn't list the {} folder", folder, e);
        }
    }

    private void addPack(Consumer<Pack> output, String id, String name, Pack.ResourcesSupplier resources) {
        PackLocationInfo location = new PackLocationInfo(id, Component.literal(name), SOURCE, Optional.empty());
        Pack.Metadata metadata = readMetadata(location, resources);
        output.accept(new Pack(location, resources, metadata, SELECTION));
    }

    /** Uses the pack's own pack.mcmeta when it has one, otherwise metadata for the current version. */
    private Pack.Metadata readMetadata(PackLocationInfo location, Pack.ResourcesSupplier resources) {
        int version = SharedConstants.getCurrentVersion().getPackVersion(type);
        boolean hasMetadata = false;
        try (PackResources pack = resources.openPrimary(location)) {
            hasMetadata = pack.getMetadataSection(PackMetadataSection.TYPE) != null;
        } catch (Exception ignored) {
            // Missing or broken pack.mcmeta, use the default below
        }
        if (hasMetadata) {
            Pack.Metadata metadata = Pack.readPackMetadata(location, resources, version);
            if (metadata != null) {
                return metadata;
            }
        }
        return new Pack.Metadata(Component.translatable("facets.pack.description"), PackCompatibility.COMPATIBLE,
                FeatureFlagSet.of(), List.of());
    }
}
