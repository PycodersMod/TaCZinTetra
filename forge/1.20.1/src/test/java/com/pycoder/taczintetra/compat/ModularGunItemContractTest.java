package com.pycoder.taczintetra.compat;

import com.pycoder.taczintetra.item.ModularGunItem;
import com.tacz.guns.api.item.IGun;
import com.pycoder.taczintetra.logic.AmmoStatePolicy;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularGunItemContractTest {
    @Test
    void modularGunIsRecognizedByTaczGunLookup() {
        assertTrue(IGun.class.isAssignableFrom(ModularGunItem.class));
    }

    @Test
    void modularGunUsesTaczNativeSwingSuppression() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        int method = source.indexOf("boolean onEntitySwing(ItemStack stack, LivingEntity entity)");
        assertTrue(method >= 0);
        int bodyEnd = source.indexOf("}", method);
        assertTrue(bodyEnd > method && source.substring(method, bodyEnd).contains("return true;"));
    }

    @Test
    void modularGunExposesRequiredNbtBackedGunStateMethods() throws Exception {
        for (String method : new String[]{"getGunId", "getCurrentAmmoCount", "setCurrentAmmoCount", "getFireMode", "hasBulletInBarrel", "getRPM", "getRepairMaterialCount"}) {
            assertTrue(hasItemStackMethod(method), method);
        }
    }

    @Test
    void modularGunDelegatesEnchantmentAllowListToPolicy() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("acceptsEnchantment"));
        assertTrue(source.contains("EnchantmentPolicy.isAllowed"));
    }

    @Test
    void modularGunHasInGameNbtInspectionPath() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/ModularGunTooltipHandler.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ItemTooltipEvent"));
        assertTrue(source.contains("GunCurrentAmmoCount"));
        assertTrue(source.contains("DummyAmmo"));
        assertTrue(source.contains("resources"));
    }

    @Test
    void durabilityAndIntegrityDefaultsComeFromTheEditableCatalog() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        String defaults = Files.readString(Path.of(
                "src/main/resources/defaultconfigs/taczintetra.json"), StandardCharsets.UTF_8);
        assertTrue(source.contains("configuredInt(\"base_durability\""));
        assertTrue(source.contains("configuredInt(\"base_integrity\""));
        assertTrue(defaults.contains("\"base_durability\":100"));
        assertTrue(defaults.contains("\"base_integrity\":1"));
        assertTrue(source.contains("refreshConfiguredDurability"));
    }

    @Test
    void maxAmmoSetterIsReadBackAndAmmoAdditionSaturates() {
        assertEquals(7, AmmoStatePolicy.sanitizeMax(7, 15));
        assertEquals(Integer.MAX_VALUE, AmmoStatePolicy.addSaturated(Integer.MAX_VALUE, 1));
        assertEquals(0, AmmoStatePolicy.addSaturated(0, -1));
    }

    @Test
    void modularGunExposesBatchAmmoConsumption() throws Exception {
        Method method = ModularGunItem.class.getMethod("reduceCurrentAmmoCount", ItemStack.class, int.class);
        assertTrue(method != null);
    }

    @Test
    void currentAmmoEntryPointRevalidatesPersistedState() throws Exception {
        assertTrue(hasItemStackMethod("revalidateRuntimeState"));
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("revalidateRuntimeState(stack)"));
        assertTrue(source.contains("GunStateRevalidator.revalidate"));
        assertTrue(source.contains("ResourceNbtAdapter.write"));
    }

    @Test
    void ammoWriteTraceIsOptInAndRecordsTheChangingStackAndCaller() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("taczintetra.dev_trace_ammo_writes"));
        assertTrue(source.contains("NATIVE_AMMO_WRITE_TRACE"));
        assertTrue(source.contains("before={}"));
        assertTrue(source.contains("after={}"));
        assertTrue(source.contains("StackWalker"));
        assertTrue(source.contains("stackUuid={}"));
    }

    @Test
    void ammoWriteTraceCoversUnmarkedAndRevalidationWrites() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        assertTrue(!source.contains("NATIVE_AMMO_WRITE_TRACE && tag.getBoolean(\"taczintetra_dev_modules_ready\")"),
                "native ammo writes must be traced even before the probe readiness marker appears");
        assertTrue(source.contains("NATIVE_AMMO_REVALIDATE_WRITE_TRACE"),
                "the direct TaCZ accessor write in revalidateRuntimeState must have its own trace");
    }

    @Test
    void repairFallbackHonorsOnlyRepairAgentsRule() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/item/ModularGunItem.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("ONLY_REPAIR_AGENTS.get()"));
        assertTrue(source.contains("return 0;"));
    }

    private static boolean hasItemStackMethod(String name) {
        for (Method method : ModularGunItem.class.getMethods()) {
            if (method.getName().equals(name)) return true;
        }
        return false;
    }
}
