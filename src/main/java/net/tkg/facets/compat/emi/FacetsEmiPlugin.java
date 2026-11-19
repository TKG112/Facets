package net.tkg.facets.compat.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.widget.Bounds;
import net.minecraft.client.renderer.Rect2i;
import net.tkg.facets.client.TraitsButton;

/** Found by EMI through its annotation, so it's only loaded when EMI is installed. */
@EmiEntrypoint
public class FacetsEmiPlugin implements EmiPlugin {
    /** Keeps EMI's favorites panel from covering the My Traits button. */
    @Override
    public void register(EmiRegistry registry) {
        registry.addGenericExclusionArea((screen, bounds) -> {
            Rect2i area = TraitsButton.area(screen);
            if (area != null) {
                bounds.accept(new Bounds(area.getX(), area.getY(), area.getWidth(), area.getHeight()));
            }
        });
    }
}
