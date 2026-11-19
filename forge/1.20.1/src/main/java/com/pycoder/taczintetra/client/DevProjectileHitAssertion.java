package com.pycoder.taczintetra.client;

import java.util.UUID;

/** Pure rule used to verify that a player's real projectile damaged that player's assigned dev target. */
final class DevProjectileHitAssertion {
    private DevProjectileHitAssertion() {
    }

    static boolean passesCrossPlayerHit(boolean integratedLanHost, String shooterName, String hostPlayerName,
                                        UUID shooterId, UUID projectileOwnerId, UUID expectedTargetId,
                                        UUID damagedTargetId, boolean taggedDevelopmentTarget,
                                        float appliedDamage) {
        return integratedLanHost
                && shooterName != null
                && !shooterName.isBlank()
                && hostPlayerName != null
                && !hostPlayerName.isBlank()
                && !shooterName.equals(hostPlayerName)
                && shooterId != null
                && shooterId.equals(projectileOwnerId)
                && expectedTargetId != null
                && expectedTargetId.equals(damagedTargetId)
                && taggedDevelopmentTarget
                && Float.isFinite(appliedDamage)
                && appliedDamage > 0.0F;
    }
}
