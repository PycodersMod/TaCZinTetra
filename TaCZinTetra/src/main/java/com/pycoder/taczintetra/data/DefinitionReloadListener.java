package com.pycoder.taczintetra.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pycoder.taczintetra.logic.GunBarrelDefinition;
import com.pycoder.taczintetra.logic.GunDefinition;
import com.pycoder.taczintetra.logic.GunFeedDefinition;
import com.pycoder.taczintetra.logic.GunMaterialDefinition;
import com.pycoder.taczintetra.logic.AmmoRecipeDefinition;
import com.pycoder.taczintetra.logic.RepairAgentDefinition;
import com.pycoder.taczintetra.logic.ResourceChannelDefinition;
import com.pycoder.taczintetra.logic.GunProfileCache;
import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.config.ModuleConfigManager;
import com.pycoder.taczintetra.registry.ModItems;
import com.pycoder.taczintetra.item.ModularGunItem;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;

/** Reads public gun definitions on every server datapack reload. */
public final class DefinitionReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Logger LOGGER = LogUtils.getLogger();
    private volatile Map<ResourceLocation, GunDefinition> guns = Map.of();
    private volatile Map<ResourceLocation, GunBarrelDefinition> barrels = Map.of();
    private volatile Map<ResourceLocation, GunFeedDefinition> feeds = Map.of();
    private volatile Map<ResourceLocation, GunMaterialDefinition> materials = Map.of();
    private volatile Map<ResourceLocation, AmmoRecipeDefinition> ammoRecipes = Map.of();
    private volatile Map<ResourceLocation, RepairAgentDefinition> repairAgents = Map.of();
    private volatile Map<ResourceLocation, ResourceChannelDefinition> resourceChannels = Map.of();

    public DefinitionReloadListener() {
        // Definitions are shipped under data/taczintetra/<type>/... . The
        // namespace already scopes this listener, so adding a second
        // taczintetra directory here would make every definition invisible.
        super(GSON, "");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager,
                         ProfilerFiller profiler) {
        GunProfileCache.clear();
        // The editable catalog is outside the datapack resource map; refresh it explicitly
        // so profile, heat and attachment changes become visible after /reload.
        TaCZinTetra.MODULE_CONFIG = ModuleConfigManager.loadOrCreate(FMLPaths.CONFIGDIR.get());
        ((ModularGunItem) ModItems.STARTER_PISTOL.get()).refreshConfiguredDurability();
        if (Boolean.getBoolean("taczintetra.dev_automation")) {
            TaCZinTetra.MODULE_CONFIG.bodies().stream()
                    .filter(body -> body.id().equals("pistol"))
                    .findFirst()
                    .ifPresent(body -> LOGGER.info("TaCZinTetra dev config reload snapshot: pistolRpm={}",
                            body.stats().getOrDefault("rpm", 0.0)));
        }
        Map<ResourceLocation, GunDefinition> nextGuns = new LinkedHashMap<>();
        Map<ResourceLocation, GunBarrelDefinition> nextBarrels = new LinkedHashMap<>();
        Map<ResourceLocation, GunFeedDefinition> nextFeeds = new LinkedHashMap<>();
        Map<ResourceLocation, GunMaterialDefinition> nextMaterials = new LinkedHashMap<>();
        Map<ResourceLocation, AmmoRecipeDefinition> nextAmmoRecipes = new LinkedHashMap<>();
        Map<ResourceLocation, RepairAgentDefinition> nextRepairAgents = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceChannelDefinition> nextResourceChannels = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : resources.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject object = entry.getValue().getAsJsonObject();
            String path = entry.getKey().getPath();
            if (path.startsWith("gun_body/")) {
                nextGuns.put(strip(entry.getKey(), "gun_body/"), GunDefinition.from(object));
            } else if (path.startsWith("gun_barrel/")) {
                nextBarrels.put(strip(entry.getKey(), "gun_barrel/"), GunBarrelDefinition.from(object));
            } else if (path.startsWith("gun_feed/")) {
                nextFeeds.put(strip(entry.getKey(), "gun_feed/"), GunFeedDefinition.from(object));
            } else if (path.startsWith("gun_material/")) {
                nextMaterials.put(strip(entry.getKey(), "gun_material/"), GunMaterialDefinition.from(object));
            } else if (path.startsWith("ammo_recipe/")) {
                if (!object.has("cost")) {
                    LOGGER.warn("Ammo recipe {} has no cost; treating it as free", entry.getKey());
                }
                nextAmmoRecipes.put(strip(entry.getKey(), "ammo_recipe/"), AmmoRecipeDefinition.from(object));
            } else if (path.startsWith("repair_agent/")) {
                nextRepairAgents.put(strip(entry.getKey(), "repair_agent/"), RepairAgentDefinition.from(object));
            } else if (path.startsWith("resource_channel/")) {
                nextResourceChannels.put(strip(entry.getKey(), "resource_channel/"), ResourceChannelDefinition.from(object));
            }
        }
        guns = immutable(nextGuns);
        barrels = immutable(nextBarrels);
        feeds = immutable(nextFeeds);
        materials = immutable(nextMaterials);
        ammoRecipes = immutable(nextAmmoRecipes);
        repairAgents = immutable(nextRepairAgents);
        resourceChannels = immutable(nextResourceChannels);
    }

    private static ResourceLocation strip(ResourceLocation id, String prefix) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), id.getPath().substring(prefix.length()));
    }

    private static <T> Map<ResourceLocation, T> immutable(Map<ResourceLocation, T> values) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Map<ResourceLocation, GunDefinition> guns() { return guns; }
    public Map<ResourceLocation, GunBarrelDefinition> barrels() { return barrels; }
    public Map<ResourceLocation, GunFeedDefinition> feeds() { return feeds; }
    public Map<ResourceLocation, GunMaterialDefinition> materials() { return materials; }
    public Map<ResourceLocation, AmmoRecipeDefinition> ammoRecipes() { return ammoRecipes; }
    public Map<ResourceLocation, RepairAgentDefinition> repairAgents() { return repairAgents; }
    public Map<ResourceLocation, ResourceChannelDefinition> resourceChannels() { return resourceChannels; }
}
