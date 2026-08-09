package com.erlavush.rtcolony.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RtsCameraModeTest {
    @Test
    void cyclesBetweenPerspectiveAndTrueIsometric() {
        assertEquals(RtsCameraMode.TRUE_ISOMETRIC, RtsCameraMode.PERSPECTIVE.next());
        assertEquals(RtsCameraMode.PERSPECTIVE, RtsCameraMode.TRUE_ISOMETRIC.next());
    }
}
