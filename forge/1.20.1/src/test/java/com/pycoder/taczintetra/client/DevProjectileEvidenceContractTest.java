package com.pycoder.taczintetra.client;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevProjectileEvidenceContractTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java");

    @Test
    void damageEvidenceRequiresTheActualKineticBulletAsDirectDamageSource() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        int attack = source.indexOf("public static void onLivingAttack");
        int damage = source.indexOf("public static void onLivingDamage");
        int attackLog = source.indexOf("dev projectile hit target", attack);
        int damageLog = source.indexOf("dev projectile damage event", damage);

        assertTrue(attack >= 0 && damage > attack);
        assertTrue(source.substring(attack, attackLog).contains(
                "event.getSource().getDirectEntity() instanceof EntityKineticBullet"),
                "attack evidence must reject generic kill and other non-projectile damage sources");
        assertTrue(source.substring(damage, damageLog).contains(
                "event.getSource().getDirectEntity() instanceof EntityKineticBullet"),
                "damage evidence must be tied to a TaCZ kinetic bullet");
    }

    @Test
    void multiplayerTargetHitRequiresMatchingOwnerTargetAndAppliedDamage() {
        UUID shooter = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID target = UUID.fromString("22222222-2222-2222-2222-222222222222");

        assertTrue(DevProjectileHitAssertion.passesCrossPlayerHit(
                true, "Guest", "Dev", shooter, shooter, target, target, true, 2.0F));
        assertFalse(DevProjectileHitAssertion.passesCrossPlayerHit(
                true, "Dev", "Dev", shooter, shooter, target, target, true, 2.0F),
                "the Integrated LAN host must never satisfy the guest hit assertion with its own shot");
        assertFalse(DevProjectileHitAssertion.passesCrossPlayerHit(
                false, "Guest", "Dev", shooter, shooter, target, target, true, 2.0F),
                "a single-player session must not report a cross-player assertion");
        assertFalse(DevProjectileHitAssertion.passesCrossPlayerHit(
                true, "Guest", "Dev", shooter, UUID.fromString("33333333-3333-3333-3333-333333333333"), target, target, true, 2.0F),
                "a different projectile owner must not satisfy this player's assertion");
        assertFalse(DevProjectileHitAssertion.passesCrossPlayerHit(
                true, "Guest", "Dev", shooter, shooter, target, UUID.fromString("44444444-4444-4444-4444-444444444444"), true, 2.0F),
                "damage to a different entity must not satisfy the assigned-target assertion");
        assertFalse(DevProjectileHitAssertion.passesCrossPlayerHit(
                true, "Guest", "Dev", shooter, shooter, target, target, false, 2.0F),
                "an ordinary entity without the probe tag must not satisfy the assertion");
        assertFalse(DevProjectileHitAssertion.passesCrossPlayerHit(
                true, "Guest", "Dev", shooter, shooter, target, target, true, 0.0F),
                "a collision that applies no damage must not satisfy the assertion");
    }

    @Test
    void setupCleanupPreservesTheCurrentTaggedDamageTarget() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevClientAutomation.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains(
                "kill @e[type=!minecraft:player,tag=!taczintetra_dev_damage_target]"),
                "generic entity cleanup must not destroy the target used by the firing probe");
    }

    @Test
    void workbenchProbeDoesNotOccupyThePlayersForwardFiringLane() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("player.getDirection().getClockWise()"),
                "place the development workbench beside the player, away from the firing lane");
        assertTrue(source.contains("workbenchProbePosition(player)"),
                "all workbench probe stages must use the same safe position");
    }

    @Test
    void damageTargetRemainsEligibleForEntityCollision() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("target.noPhysics = false;"),
                "the target must use normal entity collision so real bullets can hit it");
    }

    @Test
    void damageTargetBoundingBoxCenterIsPlacedOnTheAimRay() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("var targetAim = serverPlayer.getEyePosition().add(look.scale(2.0));"));
        assertTrue(source.contains("target.setPos(targetAim.x, targetAim.y - target.getBbHeight() / 2.0, targetAim.z);"),
                "the test target's collision box must straddle the observed shot path");
    }

    @Test
    void probeClearsAndLogsOnlyTheShortFiringLaneInTheIsolatedWorld() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        int helper = source.indexOf("private static void clearDamageProbeFiringLane");
        int target = source.indexOf("private static void ensureDamageProbeTarget");
        assertTrue(helper >= 0 && target > helper,
                "the firing lane must be prepared by a dedicated bounded helper");
        String lane = source.substring(helper, target);
        assertTrue(lane.contains("start.distanceTo(end)"), "the helper must iterate a bounded ray segment");
        assertTrue(lane.contains("length > 3.0"), "the helper must refuse to alter an unexpectedly long segment");
        assertTrue(lane.contains("BuiltInRegistries.BLOCK.getKey(state.getBlock())"),
                "every removed block must be recorded by registry ID");
        assertTrue(lane.contains("level.getBlockEntity(position) != null"),
                "the probe must preserve block entities such as Tetra workbenches");
        assertTrue(lane.contains("Blocks.AIR.defaultBlockState()"),
                "only the isolated probe lane should be cleared to air");
        assertTrue(source.contains("clearDamageProbeFiringLane(serverPlayer, targetAim);"),
                "clear the short lane before the target is spawned");
    }
}
