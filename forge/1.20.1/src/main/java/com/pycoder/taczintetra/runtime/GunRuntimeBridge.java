package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.logic.GunProfileResolver;

/** Runtime boundary kept outside the Tetra Item implementation. */
public interface GunRuntimeBridge {
    GunProfileResolver.Result profile(PerHandWeaponContext context);
    boolean canShoot(PerHandWeaponContext context);
    void shoot(PerHandWeaponContext context);
    void beginReload(PerHandWeaponContext context, boolean fillMode);
    void tick(PerHandWeaponContext context, long gameTime);
}
