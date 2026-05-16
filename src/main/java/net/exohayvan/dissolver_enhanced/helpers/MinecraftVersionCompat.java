package net.exohayvan.dissolver_enhanced.helpers;

import net.minecraft.SharedConstants;

public final class MinecraftVersionCompat {
    private MinecraftVersionCompat() {
    }

    public static boolean isMinecraft(String version) {
        return minecraftVersion().equals(version);
    }

    public static boolean isLegacyRendererVersion() {
        String version = minecraftVersion();
        return version.equals("1.21") || version.equals("1.21.1");
    }

    private static String minecraftVersion() {
        return SharedConstants.getCurrentVersion().getName();
    }
}
