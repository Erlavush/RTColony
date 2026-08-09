package com.erlavush.rtcolony.client;

/** Tracks one automatic RTS activation opportunity for each client world. */
final class RtsWorldActivationState {
    private Object currentWorld;
    private boolean autoEnablePending;

    /**
     * Observes the active world by identity and arms auto-enable for a newly
     * joined world. Returning {@code true} lets callers reset world-bound UI.
     */
    boolean observeWorld(Object world) {
        if (world == this.currentWorld) {
            return false;
        }

        this.currentWorld = world;
        this.autoEnablePending = world != null;
        return true;
    }

    void suppressForCurrentWorld() {
        this.autoEnablePending = false;
    }

    boolean consumeWhenReady(boolean ready) {
        if (!this.autoEnablePending || this.currentWorld == null || !ready) {
            return false;
        }

        this.autoEnablePending = false;
        return true;
    }
}
