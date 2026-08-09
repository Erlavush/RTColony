package com.erlavush.rtcolony.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RtsWorldActivationStateTest {
    @Test
    void activatesOnlyOnceWhenAWorldBecomesReady() {
        RtsWorldActivationState state = new RtsWorldActivationState();
        Object world = new Object();

        assertTrue(state.observeWorld(world));
        assertFalse(state.consumeWhenReady(false));
        assertTrue(state.consumeWhenReady(true));
        assertFalse(state.consumeWhenReady(true));
        assertFalse(state.observeWorld(world));
        assertFalse(state.consumeWhenReady(true));
    }

    @Test
    void manualSuppressionLastsForTheCurrentWorldOnly() {
        RtsWorldActivationState state = new RtsWorldActivationState();
        Object firstWorld = new Object();
        Object secondWorld = new Object();

        state.observeWorld(firstWorld);
        state.suppressForCurrentWorld();
        assertFalse(state.consumeWhenReady(true));

        assertTrue(state.observeWorld(secondWorld));
        assertTrue(state.consumeWhenReady(true));
    }

    @Test
    void disconnectRearmsEvenWhenTheSameWorldTokenIsObservedAgain() {
        RtsWorldActivationState state = new RtsWorldActivationState();
        Object world = new Object();

        state.observeWorld(world);
        assertTrue(state.consumeWhenReady(true));
        assertTrue(state.observeWorld(null));
        assertFalse(state.consumeWhenReady(true));
        assertTrue(state.observeWorld(world));
        assertTrue(state.consumeWhenReady(true));
    }
}
