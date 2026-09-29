package com.pycoder.taczintetra.config;

import com.pycoder.taczintetra.TaCZinTetra;
import com.pycoder.taczintetra.logic.GunProfileResolver;
import com.pycoder.taczintetra.item.GunModuleSlots;
import net.minecraft.world.item.ItemStack;
import se.mickelus.tetra.items.modular.IModularItem;
import se.mickelus.tetra.module.ItemModule;

import java.util.Set;

/** Reads the selected Tetra module slots without putting balance values in item code. */
public final class TetraItemStackProfileResolver {
    private TetraItemStackProfileResolver() {
    }

    public static GunProfileResolver.ResolvedGunProfile resolve(IModularItem item, ItemStack stack,
                                                                  String bodySlot, String barrelSlot,
                                                                  String magazineSlot) {
        GunProfileResolver.ResolvedGunProfile result = tryResolve(item, stack, bodySlot, barrelSlot, magazineSlot);
        return result == null ? empty() : result;
    }

    public static GunProfileResolver.ResolvedGunProfile tryResolve(IModularItem item, ItemStack stack,
                                                                     String bodySlot, String barrelSlot,
                                                                     String magazineSlot) {
        if (item == null || stack == null || stack.isEmpty()) return null;
        SelectedModules modules = selectedModules(item, stack, bodySlot, barrelSlot, magazineSlot);
        if (modules == null) return null;
        TetraModuleSelection body = modules.body();
        TetraModuleSelection barrel = modules.barrel();
        TetraModuleSelection magazine = modules.magazine();
        if (!ModuleConfigProfileResolver.isSelectionValid(TaCZinTetra.MODULE_CONFIG,
                body.variantId(), barrel.variantId(), magazine.variantId(),
                body.materialId(), barrel.materialId(), magazine.materialId())) return null;
        var profile = ModuleConfigProfileResolver.resolve(TaCZinTetra.MODULE_CONFIG,
                body.variantId(), barrel.variantId(), magazine.variantId(), body.materialId(),
                barrel.materialId(), magazine.materialId(),
                Math.max(0, item.getHonedCount(stack)));
        profile = ModuleConfigProfileResolver.applySpecialInlays(TaCZinTetra.MODULE_CONFIG, profile,
                specialTraits(item, stack));
        return ModuleConfigProfileResolver.applyAttachments(TaCZinTetra.MODULE_CONFIG, profile,
                attachmentId(item, stack, GunModuleSlots.STOCK),
                attachmentId(item, stack, GunModuleSlots.GRIP),
                attachmentId(item, stack, GunModuleSlots.OPTIC));
    }

    /**
     * Single authoritative profile entry point for runtime consumers. The
     * selected Tetra modules and their material are read from the ItemStack;
     * fast-changing state is supplied explicitly and never stored in the
     * static profile cache key.
     */
    public static GunProfileResolver.ResolvedGunProfile resolveComplete(IModularItem item, ItemStack stack,
                                                                         RuntimeState runtime) {
        var staticProfile = tryResolve(item, stack, GunModuleSlots.BODY,
                GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (staticProfile == null) return GunProfileResolver.resolveProfiles(null, null, null);
        RuntimeState state = runtime == null ? RuntimeState.empty() : runtime.sanitized();
        return new GunProfileResolver.ResolvedGunProfile(staticProfile.fireControl(),
                staticProfile.feed(), staticProfile.projectile(), staticProfile.handling(),
                new GunProfileResolver.ThermalProfile(state.heat(), state.overheatLocked()),
                staticProfile.visual());
    }

    public static SelectedModules selectedModules(IModularItem item, ItemStack stack,
                                                   String bodySlot, String barrelSlot, String magazineSlot) {
        if (item == null || stack == null || stack.isEmpty()) return null;
        TetraModuleSelection body = selection(item.getModuleFromSlot(stack, bodySlot), stack);
        TetraModuleSelection barrel = selection(item.getModuleFromSlot(stack, barrelSlot), stack);
        TetraModuleSelection magazine = selection(item.getModuleFromSlot(stack, magazineSlot), stack);
        if (body == null || barrel == null || magazine == null) return null;
        return new SelectedModules(body, barrel, magazine);
    }

    public static boolean isTwoHanded(IModularItem item, ItemStack stack) {
        SelectedModules modules = selectedModules(item, stack, GunModuleSlots.BODY,
                GunModuleSlots.BARREL, GunModuleSlots.MAGAZINE);
        if (modules == null) return false;
        return TaCZinTetra.MODULE_CONFIG.bodies().stream()
                .filter(body -> body.id().equals(modules.body().variantId()))
                .findFirst().map(ModuleConfig.Body::twoHanded).orElse(false);
    }

    /** Returns the validated special-inlay IDs currently stored on this gun. */
    public static Set<String> specialTraits(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return Set.of();
        return SpecialInlayPolicy.resolve(stack.getOrCreateTag(), TaCZinTetra.MODULE_CONFIG).activeIds();
    }

    /** Reads both explicit inlay NBT and the real Tetra special slot. */
    public static Set<String> specialTraits(IModularItem item, ItemStack stack) {
        if (item == null || stack == null || stack.isEmpty()) return Set.of();
        var requested = new java.util.LinkedHashSet<>(specialTraits(stack));
        try {
            if (item.getModuleFromSlot(stack, GunModuleSlots.SPECIAL) != null) requested.add("socket");
        } catch (RuntimeException ignored) {
            // An incomplete module tree is treated as having no special slot.
        }
        return SpecialInlayPolicy.resolve(requested, TaCZinTetra.MODULE_CONFIG,
                hasModule(item, stack, GunModuleSlots.SPECIAL)).activeIds();
    }

    private static TetraModuleSelection selection(ItemModule module, ItemStack stack) {
        if (module == null) return null;
        String key = module.getKey();
        if (key == null) return null;
        // A Tetra 6.17 module instance is registered by its complete module
        // path (for example taczintetra/body/pistol). The server-side module
        // data may not have a selected VariantData yet, so derive the model
        // id from that path and only use VariantData when it is available.
        String variantKey = key.substring(key.lastIndexOf('/') + 1) + "/";
        return TetraModuleSelection.from(key, variantKey,
                stack.getOrCreateTag().getString(key + "_material"));
    }

    private static boolean hasModule(IModularItem item, ItemStack stack, String slot) {
        try {
            ItemModule module = item.getModuleFromSlot(stack, slot);
            // Tetra 6.17 can leave VariantData unresolved on the logical server while
            // the module is already installed. Presence of the slot module is the
            // authoritative attachment signal; requiring VariantData made stock/grip/
            // optic work on the client but silently disappear from server profiles.
            return module != null;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static String attachmentId(IModularItem item, ItemStack stack, String slot) {
        try {
            ItemModule module = item.getModuleFromSlot(stack, slot);
            if (module == null || module.getKey() == null) return null;
            String key = module.getKey();
            int slash = key.lastIndexOf('/');
            String id = slash < 0 ? key : key.substring(slash + 1);
            return id.isBlank() || "starter".equals(id) ? slot.substring(slot.lastIndexOf('/') + 1) : id;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static GunProfileResolver.ResolvedGunProfile empty() {
        return ModuleConfigProfileResolver.resolve(ModuleConfig.empty(), "", "", "");
    }

    public record SelectedModules(TetraModuleSelection body, TetraModuleSelection barrel,
                                  TetraModuleSelection magazine) { }

    /** Runtime-only fields intentionally kept outside static module identity. */
    public record RuntimeState(float heat, boolean overheatLocked, int currentAmmo,
                               int resourceAmount, Set<String> specialTraits) {
        public RuntimeState {
            specialTraits = specialTraits == null ? Set.of() : Set.copyOf(specialTraits);
        }

        public RuntimeState sanitized() {
            float safeHeat = Float.isFinite(heat) ? Math.max(0, heat) : 0;
            return new RuntimeState(safeHeat, overheatLocked, Math.max(0, currentAmmo),
                    Math.max(0, resourceAmount), specialTraits);
        }

        public static RuntimeState empty() {
            return new RuntimeState(0, false, 0, 0, Set.of());
        }
    }
}
