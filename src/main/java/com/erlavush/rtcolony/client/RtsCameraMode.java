package com.erlavush.rtcolony.client;

public enum RtsCameraMode {
    PERSPECTIVE,
    TRUE_ISOMETRIC;

    public RtsCameraMode next() {
        return this == PERSPECTIVE ? TRUE_ISOMETRIC : PERSPECTIVE;
    }
}
