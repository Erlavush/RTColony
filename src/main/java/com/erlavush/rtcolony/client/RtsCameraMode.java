package com.erlavush.rtcolony.client;

public enum RtsCameraMode {
    PERSPECTIVE,
    FIXED_ANGLE,
    TRUE_ISOMETRIC,
    FREECAM;

    public RtsCameraMode next() {
        return switch (this) {
            case PERSPECTIVE -> FIXED_ANGLE;
            case FIXED_ANGLE -> TRUE_ISOMETRIC;
            case TRUE_ISOMETRIC -> FREECAM;
            case FREECAM -> PERSPECTIVE;
        };
    }
}
