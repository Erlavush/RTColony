package com.erlavush.rtcolony.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RtsCameraModeTest {
    @Test
    void cyclesThroughAllRtsCameraModes() {
        assertEquals(RtsCameraMode.FIXED_ANGLE, RtsCameraMode.PERSPECTIVE.next());
        assertEquals(RtsCameraMode.TRUE_ISOMETRIC, RtsCameraMode.FIXED_ANGLE.next());
        assertEquals(RtsCameraMode.FREECAM, RtsCameraMode.TRUE_ISOMETRIC.next());
        assertEquals(RtsCameraMode.PERSPECTIVE, RtsCameraMode.FREECAM.next());
    }
}
