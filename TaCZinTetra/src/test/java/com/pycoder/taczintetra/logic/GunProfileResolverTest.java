package com.pycoder.taczintetra.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GunProfileResolverTest {
    @Test
    void fixedAdditionsThenMultipliersAreAppliedInOrder() {
        GunProfileResolver.PartStats body = new GunProfileResolver.PartStats(10, 100, 1, 1, 0, 0, 1, 1);
        GunProfileResolver.PartStats barrel = new GunProfileResolver.PartStats(2, 0, 2, 1, 0, 0, 1, 1);
        GunProfileResolver.PartStats magazine = new GunProfileResolver.PartStats(0, 0, 1, 2, 0, 0, 1, 1);
        GunProfileResolver.Result result = GunProfileResolver.resolve(body, barrel, magazine);

        assertEquals(12.0, result.damage());
        assertEquals(100, result.roundsPerMinute());
        assertEquals(2.0, result.velocity());
        assertEquals(2, result.loadedCapacity());
    }

    @Test
    void damageAndRpmMultipliersAreMultipliedIndependently() {
        GunProfileResolver.Result result = GunProfileResolver.resolve(
                new GunProfileResolver.PartStats(10, 100, 1, 1, 0, 0, 1.5, 1.2),
                new GunProfileResolver.PartStats(2, 0, 1, 1, 0, 0, 0.5, 0.5),
                null);

        assertEquals(9.0, result.damage());
        assertEquals(60, result.roundsPerMinute());
    }

    @Test
    void invalidValuesAreClampedToSafeDefaults() {
        GunProfileResolver.PartStats invalid = new GunProfileResolver.PartStats(-4, -1, -2, 0, 0, 0, -1, -3);
        GunProfileResolver.Result result = GunProfileResolver.resolve(invalid, invalid, invalid);
        assertEquals(0.01, result.damage());
        assertEquals(1, result.roundsPerMinute());
        assertEquals(1.0, result.velocity());
        assertEquals(1, result.loadedCapacity());
    }

    @Test
    void profilesExposeSeparatedDomains() {
        GunProfileResolver.ResolvedGunProfile profile = GunProfileResolver.resolveProfiles(
                new GunProfileResolver.PartStats(2, 10, 1, 1, 0.1, 0.2, 1, 1), null, null);
        assertEquals(1, profile.feed().loadedCapacity());
        assertEquals(2.0, profile.projectile().damage());
        assertEquals(0.1, profile.handling().accuracyAdd());
        assertFalse(profile.thermal().overheatLocked());
    }

    @Test
    void projectileProfileCarriesConfiguredBallisticProperties() {
        GunProfileResolver.ResolvedGunProfile profile = GunProfileResolver.resolveProfiles(
                new GunProfileResolver.PartStats(0, 0, 1, 1, 0, 0, 1, 1),
                new GunProfileResolver.PartStats(0, 0, 2, 1, 0, 0, 1, 1,
                        1.5, 0.25, 2, 1.2), null);

        assertEquals(2.0, profile.projectile().velocity());
        assertEquals(1.5, profile.projectile().range());
        assertEquals(0.25, profile.projectile().armorIgnore());
        assertEquals(2, profile.projectile().pierce());
        assertEquals(1.2, profile.projectile().knockback());
    }

    @Test
    void weightAndReloadTimeUseIndependentMultiplicativeFactors() {
        GunProfileResolver.ResolvedGunProfile profile = GunProfileResolver.resolveProfiles(
                new GunProfileResolver.PartStats(0, 0, 1, 1, 0, 0, 1, 1,
                        1, 0, 0, 1, 1.5, 1.2),
                new GunProfileResolver.PartStats(0, 0, 1, 1, 0, 0, 1, 1,
                        1, 0, 0, 1, 2.0, 0.5),
                null);

        assertEquals(3.0, profile.handling().weightMultiplier());
        assertEquals(0.6, profile.handling().reloadTimeMultiplier());
    }
}
