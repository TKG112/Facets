package net.tkg.facets.trait;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** How traits are ordered inside a category's list. */
public enum SortOrder implements StringRepresentable {
    COST_DESCENDING("cost_descending"),
    COST_ASCENDING("cost_ascending"),
    NAME("name");

    public static final Codec<SortOrder> CODEC = StringRepresentable.fromEnum(SortOrder::values);

    private final String name;

    SortOrder(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
