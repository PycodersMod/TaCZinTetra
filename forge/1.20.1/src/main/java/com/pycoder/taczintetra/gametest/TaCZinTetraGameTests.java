package com.pycoder.taczintetra.gametest;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.registry.ModItems;
import com.pycoder.taczintetra.config.ConfiguredPartIds;
import com.pycoder.taczintetra.config.ConfiguredAmmoRecipeResolver;
import com.pycoder.taczintetra.config.TetraItemStackProfileResolver;
import com.pycoder.taczintetra.config.ModuleConfig;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.tacz.guns.api.item.IGun;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import se.mickelus.tetra.items.modular.IModularItem;

/** 需要真实注册表与 TaCZ 运行时环境的 Forge 端检查。 */
@GameTestHolder(TaCZinTetra.MOD_ID)
public final class TaCZinTetraGameTests {
    private TaCZinTetraGameTests() { }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void starterGunIsRecognizedByTacz(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        helper.assertTrue(IGun.getIGunOrNull(stack) == ModItems.STARTER_PISTOL.get(),
                "starter_pistol must be visible through TaCZ IGun.getIGunOrNull");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void realTetraModuleNbtResolvesConfiguredRecipe(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY, "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL, "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE, "taczintetra/magazine/standard", "taczintetra/iron/");
        TetraItemStackProfileResolver.SelectedModules modules = TetraItemStackProfileResolver.selectedModules(
                (IModularItem) stack.getItem(), stack, GunModuleSlots.BODY, GunModuleSlots.BARREL,
                GunModuleSlots.MAGAZINE);
        helper.assertTrue(modules != null, "real Tetra module NBT must resolve all three modules");
        helper.assertTrue(ConfiguredAmmoRecipeResolver.select(TaCZinTetra.MODULE_CONFIG,
                (IModularItem) stack.getItem(), stack) != null,
                "resolved modules must select a configured ammo recipe");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void configuredPhysicalPartsAreRegistered(GameTestHelper helper) {
        for (String path : ConfiguredPartIds.localPaths(TaCZinTetra.MODULE_CONFIG)) {
            helper.assertTrue(ModItems.configuredPart(path) != null,
                    "configured physical part must have a registry entry: " + path);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void everyConfiguredMajorModuleCombinationResolves(GameTestHelper helper) {
        for (ModuleConfig.Body body : TaCZinTetra.MODULE_CONFIG.bodies()) {
            for (ModuleConfig.Barrel barrel : TaCZinTetra.MODULE_CONFIG.barrels()) {
                ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
                IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY,
                        "taczintetra/body/" + body.id(), "taczintetra/iron/");
                IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL,
                        "taczintetra/barrel/" + barrel.id(), "taczintetra/iron/");
                IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE,
                        "taczintetra/magazine/standard", "taczintetra/iron/");
                var profile = TetraItemStackProfileResolver.tryResolve((IModularItem) stack.getItem(), stack,
                        GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
                helper.assertTrue(profile != null && profile.feed().loadedCapacity() > 0,
                        "configured Tetra combination must resolve: " + body.id() + "/" + barrel.id());
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void installedMinorModulesParticipateInRuntimeProfile(GameTestHelper helper) {
        ItemStack base = new ItemStack(ModItems.STARTER_PISTOL.get());
        installMinimumMajorModules(base);
        ItemStack attached = base.copy();
        IModularItem.putModuleInSlot(attached, GunModuleSlots.STOCK, "taczintetra/stock", "taczintetra/");
        IModularItem.putModuleInSlot(attached, GunModuleSlots.GRIP, "taczintetra/grip", "taczintetra/");
        IModularItem.putModuleInSlot(attached, GunModuleSlots.OPTIC, "taczintetra/optic", "taczintetra/");
        var baseProfile = TetraItemStackProfileResolver.tryResolve((IModularItem) base.getItem(), base,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        var attachedProfile = TetraItemStackProfileResolver.tryResolve((IModularItem) attached.getItem(), attached,
                GunModuleSlots.BODY, GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        helper.assertTrue(baseProfile != null && attachedProfile != null,
                "major and minor module profiles must resolve");
        helper.assertTrue(attachedProfile.handling().recoilAdd() < baseProfile.handling().recoilAdd(),
                "installed stock must affect recoil on the runtime profile");
        helper.succeed();
    }

    private static void installMinimumMajorModules(ItemStack stack) {
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY,
                "taczintetra/body/pistol", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL,
                "taczintetra/barrel/9mm", "taczintetra/iron/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE,
                "taczintetra/magazine/standard", "taczintetra/iron/");
    }
}
