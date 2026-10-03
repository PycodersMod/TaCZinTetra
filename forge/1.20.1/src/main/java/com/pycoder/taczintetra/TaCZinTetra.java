package com.pycoder.taczintetra;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import com.pycoder.taczintetra.config.ModuleConfig;
import com.pycoder.taczintetra.config.ModuleConfigManager;
import com.pycoder.taczintetra.config.TaczInTetraForgeConfig;
import com.pycoder.taczintetra.registry.ModItems;
import com.pycoder.taczintetra.registry.ModRecipeSerializers;
import com.pycoder.taczintetra.network.ModNetwork;
import com.pycoder.taczintetra.data.DefinitionReloadListener;
import com.pycoder.taczintetra.compat.TaCZGunpackExporter;
import com.pycoder.taczintetra.compat.TaczRepairSchematic;
import com.pycoder.taczintetra.item.ModularGunItem;
import se.mickelus.tetra.module.SchematicRegistry;

@Mod(TaCZinTetra.MOD_ID)
public class TaCZinTetra {
    public static final String MOD_ID = "taczintetra";
    public static final DefinitionReloadListener DEFINITIONS = new DefinitionReloadListener();
    public static volatile ModuleConfig MODULE_CONFIG = ModuleConfig.empty();
    private static final Logger LOGGER = LogUtils.getLogger();

    public TaCZinTetra() {
        TaczInTetraForgeConfig.register();
        TaCZGunpackExporter.export();
        MODULE_CONFIG = ModuleConfigManager.loadOrCreate(FMLPaths.CONFIGDIR.get());
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.registerConfiguredParts(MODULE_CONFIG);
        ModItems.ITEMS.register(modBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modBus);
        ModNetwork.registerMessages();
        modBus.addListener(TaCZinTetra::registerRepairSchematic);
        MinecraftForge.EVENT_BUS.addListener(TaCZinTetra::addReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(TaCZinTetra::onConfigLoaded);
        MinecraftForge.EVENT_BUS.addListener(TaCZinTetra::onPlayerLoggedIn);
        LOGGER.info("{} loaded", MOD_ID);
    }

    private static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new com.pycoder.taczintetra.network.ConfigDigestMessage(
                            com.pycoder.taczintetra.network.ConfigDigest.of(MODULE_CONFIG)));
        }
    }

    private static void registerRepairSchematic(FMLCommonSetupEvent event) {
        if (SchematicRegistry.instance != null && ModItems.STARTER_PISTOL.isPresent()) {
            SchematicRegistry.instance.registerSchematic(
                    new TaczRepairSchematic((ModularGunItem) ModItems.STARTER_PISTOL.get()));
        }
    }

    private static void onConfigLoaded(ModConfigEvent.Loading event) {
        if (!MOD_ID.equals(event.getConfig().getModId()) || !TaczInTetraForgeConfig.DEBUG_LOGGING.get()) {
            return;
        }
        {
            LOGGER.info("TaCZinTetra debug logging enabled: nativeItems={}, nativeRecipes={}, nativeWorkbenches={}, jeiMode={}, onlySemiFinishedParts={}, onlyRepairAgents={}",
                    TaczInTetraForgeConfig.DISABLE_NATIVE_ITEMS.get(),
                    TaczInTetraForgeConfig.DISABLE_NATIVE_RECIPES.get(),
                    TaczInTetraForgeConfig.DISABLE_NATIVE_WORKBENCHES.get(),
                    TaczInTetraForgeConfig.JEI_MODE.get(),
                    TaczInTetraForgeConfig.ONLY_SEMI_FINISHED_PARTS.get(),
                    TaczInTetraForgeConfig.ONLY_REPAIR_AGENTS.get());
        }
    }

    private static void addReloadListeners(AddReloadListenerEvent event) {
        if (SchematicRegistry.instance != null) {
            SchematicRegistry.instance.registerSchematic(
                    new TaczRepairSchematic((ModularGunItem) ModItems.STARTER_PISTOL.get()));
        }
        event.addListener(DEFINITIONS);
    }
}
