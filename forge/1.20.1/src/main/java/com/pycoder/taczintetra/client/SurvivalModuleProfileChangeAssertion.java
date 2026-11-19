package com.pycoder.taczintetra.client;

import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.item.ModularGunItem;
import net.minecraft.world.item.ItemStack;

/** Runtime evidence that a real Tetra body upgrade changed the resolved gun profile. */
public final class SurvivalModuleProfileChangeAssertion {
    private SurvivalModuleProfileChangeAssertion() { }

    public static Snapshot snapshot(ModularGunItem item, ItemStack stack) {
        if (item == null || stack == null || stack.isEmpty()) return null;
        var profile = TetraItemStackProfileResolver.tryResolve(item, stack,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (profile == null) return null;
        return new Snapshot(profile.fireControl().roundsPerMinute(), profile.projectile().damage(),
                profile.handling().recoilAdd(), TetraItemStackProfileResolver.isTwoHanded(item, stack));
    }

    public static Result evaluate(Snapshot before, Snapshot after) {
        if (!valid(before) || !valid(after)) return new Result(false, "profile_snapshot_missing");
        if (after.roundsPerMinute() <= before.roundsPerMinute()
                || after.damage() <= before.damage()
                || after.recoil() <= before.recoil()
                || before.twoHanded() || !after.twoHanded()) {
            return new Result(false, "runtime_profile_did_not_reflect_rifle");
        }
        return new Result(true, "runtime_profile_updated");
    }

    private static boolean valid(Snapshot value) {
        return value != null && value.roundsPerMinute() > 0
                && Double.isFinite(value.damage()) && value.damage() > 0
                && Double.isFinite(value.recoil()) && value.recoil() >= 0;
    }

    public record Snapshot(int roundsPerMinute, double damage, double recoil, boolean twoHanded) { }
    public record Result(boolean passed, String reason) { }
}
