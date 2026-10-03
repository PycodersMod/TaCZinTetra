package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.logic.GunProfileResolver;

/** 独立于 Tetra Item 实现的运行时边界。 */
public interface GunRuntimeBridge {
    GunProfileResolver.Result profile(PerHandWeaponContext context);
    boolean canShoot(PerHandWeaponContext context);
    void shoot(PerHandWeaponContext context);
    void beginReload(PerHandWeaponContext context, boolean fillMode);
    void tick(PerHandWeaponContext context, long gameTime);
}
