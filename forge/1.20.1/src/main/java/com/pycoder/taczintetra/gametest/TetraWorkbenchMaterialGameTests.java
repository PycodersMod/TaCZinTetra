package com.pycoder.taczintetra.gametest;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.item.GunModuleSlots;
import com.pycoder.taczintetra.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.SchematicRegistry;
import se.mickelus.tetra.module.schematic.UpgradeSchematic;

/** Runtime checks for Tetra material acceptance using the actual loaded schematics. */
@GameTestHolder(TaCZinTetra.MOD_ID)
public final class TetraWorkbenchMaterialGameTests {
    private TetraWorkbenchMaterialGameTests() { }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void configuredIronBodyAppliesRifleUpgradeAndIsConsumed(GameTestHelper helper) {
        UpgradeSchematic schematic = SchematicRegistry.getSchematic("taczintetra/body_rifle");
        helper.assertTrue(schematic != null, "body_rifle schematic must be loaded by Tetra");
        if (schematic == null) return;

        ItemStack target = starterPistol();
        ItemStack[] materials = {new ItemStack(ModItems.configuredPart("iron_body").get())};
        var player = helper.makeMockPlayer();

        helper.assertTrue(schematic.acceptsMaterial(target, GunModuleSlots.BODY, 0, materials[0]),
                "configured iron body must be accepted as Tetra iron material");
        helper.assertTrue(schematic.canApplyUpgrade(player, target, materials,
                        GunModuleSlots.BODY, java.util.Map.of()),
                "configured iron body must allow the rifle-body upgrade");

        ItemStack result = schematic.applyUpgrade(target, materials, true, GunModuleSlots.BODY, player);
        var body = ((IModularItem) result.getItem()).getModuleFromSlot(result, GunModuleSlots.BODY);
        helper.assertTrue(body != null && "taczintetra/body/rifle".equals(body.getKey()),
                "Tetra upgrade must install the rifle body module");
        helper.assertTrue(materials[0].isEmpty(), "the accepted iron body must be consumed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void rawIronIngotCannotBypassConfiguredPhysicalPartRestriction(GameTestHelper helper) {
        UpgradeSchematic schematic = SchematicRegistry.getSchematic("taczintetra/body_rifle");
        helper.assertTrue(schematic != null, "body_rifle schematic must be loaded by Tetra");
        if (schematic == null) return;

        ItemStack target = starterPistol();
        ItemStack before = target.copy();
        ItemStack[] materials = {new ItemStack(Items.IRON_INGOT)};
        int materialCountBefore = materials[0].getCount();
        var player = helper.makeMockPlayer();

        helper.assertTrue(!schematic.acceptsMaterial(target, GunModuleSlots.BODY, 0, materials[0]),
                "raw iron ingot must not be accepted in place of a configured physical part");
        helper.assertTrue(!schematic.canApplyUpgrade(player, target, materials,
                        GunModuleSlots.BODY, java.util.Map.of()),
                "raw iron ingot must not enable the rifle-body upgrade");
        helper.assertTrue(ItemStack.isSameItemSameTags(before, target),
                "rejected material must leave the gun unchanged");
        helper.assertTrue(materials[0].getCount() == materialCountBefore,
                "rejected material must not be consumed");
        helper.succeed();
    }

    private static ItemStack starterPistol() {
        ItemStack stack = new ItemStack(ModItems.STARTER_PISTOL.get());
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BODY,
                "taczintetra/body/pistol", "taczintetra/wood/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.BARREL,
                "taczintetra/barrel/9mm", "taczintetra/wood/");
        IModularItem.putModuleInSlot(stack, GunModuleSlots.MAGAZINE,
                "taczintetra/magazine/standard", "taczintetra/wood/");
        return stack;
    }
}
