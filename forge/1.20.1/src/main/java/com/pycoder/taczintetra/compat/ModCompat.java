package com.pycoder.taczintetra.compat;

import net.minecraftforge.fml.ModList;

/** 集中检查依赖是否存在；功能适配器必须在此防护可选路径。 */
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
