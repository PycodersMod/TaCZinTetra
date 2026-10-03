package com.pycoder.taczintetra.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddonApiContractTest {
    @Test
    void exposesAllConfiguredResourceChannelCategories() {
        assertEquals(6, ResourceChannelType.values().length);
        assertTrue(ResourceChannelType.valueOf("ITEM") != null);
        assertTrue(ResourceChannelType.valueOf("CHEMICAL") != null);
    }

    @Test
    void adaptersAreIndependentOfThirdPartyModClasses() {
        assertTrue(ResourceInsertionAdapter.class.isInterface());
        assertTrue(ExternalCapabilityAdapter.class.isInterface());
    }

    @Test
    void adapterRegistryIsReversibleAndDeduplicates() {
        ResourceInsertionAdapter adapter = new ResourceInsertionAdapter() {
            public ResourceChannelType channelType() { return ResourceChannelType.ITEM; }
            public boolean supports(net.minecraft.world.item.ItemStack resource) { return false; }
            public int insert(net.minecraft.world.item.ItemStack gun, net.minecraft.world.item.ItemStack resource, int limit) { return 0; }
        };
        AddonAdapterRegistry.register(adapter);
        AddonAdapterRegistry.register(adapter);
        assertEquals(1, AddonAdapterRegistry.itemAdapters().stream().filter(value -> value == adapter).count());
        AddonAdapterRegistry.unregister(adapter);
        assertTrue(AddonAdapterRegistry.itemAdapters().stream().noneMatch(value -> value == adapter));
    }

    @Test
    void scopedExternalRegistrationIsRemovedWhenClosed() {
        ExternalCapabilityAdapter adapter = new ExternalCapabilityAdapter() {
            public ResourceChannelType channelType() { return ResourceChannelType.ITEM; }
            public boolean canExtract(net.minecraft.world.entity.LivingEntity holder, String resourceId, int amount) {
                return false;
            }
            public int extract(net.minecraft.world.entity.LivingEntity holder, String resourceId, int amount) {
                return 0;
            }
        };
        AddonAdapterRegistry.Registration registration = AddonAdapterRegistry.registerScoped(adapter);
        try {
            assertTrue(AddonAdapterRegistry.externalAdapters().stream().anyMatch(value -> value == adapter));
        } finally {
            registration.close();
        }
        assertTrue(AddonAdapterRegistry.externalAdapters().stream().noneMatch(value -> value == adapter));
    }

    @Test
    void resourceInsertionServiceConsumesAddonAcceptedRemainder() throws Exception {
        String source = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ResourceInsertionService.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("resource.shrink(accepted)"));
    }

    @Test
    void externalCapabilityAdapterExposesAvailableAmountForAuthoritativeReload() throws Exception {
        assertTrue(java.util.Arrays.stream(ExternalCapabilityAdapter.class.getMethods())
                .anyMatch(method -> method.getName().equals("available")));
        String source = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeSettlementService.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("ExternalCapabilityAdapter"));
        assertTrue(source.contains("extract"));
    }

    @Test
    void externalReloadSettlementPreflightsAllAdaptersBeforeExtraction() throws Exception {
        String source = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeSettlementService.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("canExtract(holder"));
        assertTrue(source.indexOf("canExtract(holder") < source.indexOf("adapter.extract(holder"));
    }

    @Test
    void emptySupportedExternalResourceDoesNotFallBackToStaleItemNbt() throws Exception {
        String service = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ExternalResourceService.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        String settlement = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ReloadRuntimeSettlementService.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        String hud = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/client/ModularGunHudHandler.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(service.contains("filter(adapter -> adapter.supports(resourceId))"));
        assertTrue(settlement.contains("adapter == null"));
        assertTrue(hud.contains("ExternalResourceService.adapterFor"));
    }

    @Test
    void supportedAddonInsertionFailureDoesNotFallBackToItemNbt() throws Exception {
        String source = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ResourceInsertionService.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        int supportBranch = source.indexOf("adapter.supports(resource)");
        int fallbackLookup = source.indexOf("BuiltInRegistries.ITEM.getKey", supportBranch);
        assertTrue(supportBranch >= 0);
        assertTrue(fallbackLookup > supportBranch);
        assertTrue(source.indexOf("return 0", supportBranch) < fallbackLookup);
    }

    @Test
    void devAutomationContainsAnIsolatedCapabilitySimulation() throws Exception {
        String source = Files.readString(java.nio.file.Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(source.contains("ExternalCapabilityAdapter"));
        assertTrue(source.contains("ExternalResourceService.available"));
        assertTrue(source.contains("ExternalResourceService.extract"));
        assertTrue(source.contains("dev addon capability simulation"));
    }
}
