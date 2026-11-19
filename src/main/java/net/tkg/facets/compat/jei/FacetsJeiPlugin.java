package net.tkg.facets.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import net.tkg.facets.Facets;
import net.tkg.facets.client.TraitsButton;

import java.util.Collection;
import java.util.List;

/** Found by JEI through its annotation, so it's only loaded when JEI is installed. */
@JeiPlugin
public class FacetsJeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return Facets.id("jei_plugin");
    }

    /** Keeps JEI's bookmark list from covering the My Traits button. */
    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                Rect2i area = TraitsButton.area(Minecraft.getInstance().screen);
                return area == null ? List.of() : List.of(area);
            }
        });
    }
}
