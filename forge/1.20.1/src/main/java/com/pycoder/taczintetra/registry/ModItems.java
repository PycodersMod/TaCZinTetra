package com.pycoder.taczintetra.registry;

import com.pycoder.taczintetra.TaCZinTetra;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import com.pycoder.taczintetra.item.ModularGunItem;
import com.pycoder.taczintetra.config.ConfiguredPartIds;
import com.pycoder.taczintetra.config.ModuleConfig;

import java.util.LinkedHashMap;
import java.util.Map;

/** 占位实体部件；对应的 Tetra 模块映射由数据驱动。 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TaCZinTetra.MOD_ID);

    public static final RegistryObject<Item> STARTER_PISTOL = ITEMS.register(
            "starter_pistol", () -> new ModularGunItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PISTOL_BODY = register("pistol_body");
    public static final RegistryObject<Item> PISTOL_BARREL = register("pistol_barrel");
    public static final RegistryObject<Item> PISTOL_MAGAZINE = register("pistol_magazine");
    public static final RegistryObject<Item> PISTOL_STOCK = register("pistol_stock");
    public static final RegistryObject<Item> PISTOL_OPTIC = register("pistol_optic");
    public static final RegistryObject<Item> PISTOL_GRIP = register("pistol_grip");
    public static final RegistryObject<Item> SPECIAL_INLAY = register("special_inlay");
    public static final RegistryObject<Item> REPAIR_AGENT = register("repair_agent");
    public static final RegistryObject<Item> WOOD_REPAIR = register("wood_repair");
    public static final RegistryObject<Item> STONE_REPAIR = register("stone_repair");
    public static final RegistryObject<Item> IRON_REPAIR = register("iron_repair");
    public static final RegistryObject<Item> GOLD_REPAIR = register("gold_repair");
    public static final RegistryObject<Item> NETHERITE_REPAIR = register("netherite_repair");
    private static final Map<String, RegistryObject<Item>> CONFIGURED_PARTS = new LinkedHashMap<>();

    private ModItems() {
    }

    /** 仅注册用户可编辑目录中声明的成品部件。 */
    public static void registerConfiguredParts(ModuleConfig config) {
        for (String path : ConfiguredPartIds.localPaths(config)) {
            CONFIGURED_PARTS.computeIfAbsent(path, ModItems::register);
        }
    }

    public static RegistryObject<Item> configuredPart(String path) {
        return CONFIGURED_PARTS.get(path);
    }

    private static RegistryObject<Item> register(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties()));
    }
}
