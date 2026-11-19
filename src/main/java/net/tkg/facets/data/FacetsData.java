package net.tkg.facets.data;

import java.util.function.Supplier;

/** The data currently loaded on the server. Replaced on every datapack reload. */
public final class FacetsData {
    private static volatile TraitData current = TraitData.EMPTY;
    private static volatile int lastErrors;
    private static volatile Supplier<TraitData> display = FacetsData::get;

    private FacetsData() {
    }

    public static TraitData get() {
        return current;
    }

    /**
     * Data for building tooltips and descriptions: the client's synced copy on the client, the
     * server's data otherwise.
     */
    public static TraitData forDisplay() {
        return display.get();
    }

    public static void setDisplaySource(Supplier<TraitData> source) {
        display = source;
    }

    /** Number of files that failed to load in the last reload. */
    public static int lastErrors() {
        return lastErrors;
    }

    static void set(TraitData data, int errors) {
        current = data;
        lastErrors = errors;
    }
}
