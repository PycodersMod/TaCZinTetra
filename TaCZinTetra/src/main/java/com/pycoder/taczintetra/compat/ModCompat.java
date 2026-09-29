package com.pycoder.taczintetra.compat;

import net.minecraftforge.fml.ModList;

/** Centralized dependency presence checks; feature adapters must guard optional paths here. */
public final class ModCompat {
    private ModCompat() {
    }

    public static boolean isTaczLoaded() {
        return ModList.get().isLoaded("tacz");
    }

    public static boolean isTetraLoaded() {
        return ModList.get().isLoaded("tetra");
    }

    public static boolean areCoreModsLoaded() {
        return isTaczLoaded() && isTetraLoaded();
    }
}
