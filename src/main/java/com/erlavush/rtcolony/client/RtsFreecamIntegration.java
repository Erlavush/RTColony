package com.erlavush.rtcolony.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import net.xolt.freecam.Freecam;
import net.xolt.freecam.util.FreeCamera;
import net.xolt.freecam.util.FreecamPosition;

final class RtsFreecamIntegration {
    private RtsFreecamIntegration() {
    }

    static boolean enableAt(Vec3 cameraPosition, float yaw, float pitch) {
        if (!Freecam.isEnabled()) {
            Freecam.toggle();
        }

        FreeCamera freeCamera = Freecam.getFreeCamera();
        if (!Freecam.isEnabled() || freeCamera == null) {
            return false;
        }

        FreecamPosition position = new FreecamPosition(freeCamera);
        position.x = cameraPosition.x;
        position.y = cameraPosition.y - freeCamera.getEyeHeight();
        position.z = cameraPosition.z;
        position.setRotation(yaw, pitch);
        Freecam.moveToPosition(position);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen == null && minecraft.isWindowActive()) {
            minecraft.mouseHandler.grabMouse();
        }
        return true;
    }

    static VisualPose captureVisualPose() {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        return new VisualPose(camera.getPosition(), camera.getYRot(), camera.getXRot());
    }

    static boolean isEnabled() {
        return Freecam.isEnabled();
    }

    static void disable() {
        if (Freecam.isEnabled()) {
            Freecam.toggle();
        }
    }

    record VisualPose(Vec3 position, float yaw, float pitch) {
    }
}
