package net.tkg.facets.api;

/** Why a player's traits or occupation changed. */
public enum ChangeCause {
    /** The player confirmed the selection screen. */
    SELECTION,
    /** A /facets command (add, remove, occupation). */
    COMMAND,
    /** All traits were cleared: /facets reset, or dying with onDeath = RESET. */
    RESET,
    /** A trait's gain_when or lose_when condition matched. */
    TRIGGER,
    /** Another mod called {@link FacetsApi}. */
    API;

    /** Whether the player gets a toast for the change. Not for screen picks or full resets. */
    public boolean notifiesPlayer() {
        return this == COMMAND || this == TRIGGER || this == API;
    }
}
