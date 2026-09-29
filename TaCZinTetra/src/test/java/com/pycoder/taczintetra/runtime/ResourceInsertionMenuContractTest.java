package com.pycoder.taczintetra.runtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceInsertionMenuContractTest {
    @Test
    void standardMenuMixinHandlesRightClickWithCarriedResource() throws Exception {
        String mixins = Files.readString(Path.of("src/main/resources/taczintetra.mixins.json"), StandardCharsets.UTF_8);
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/mixin/AbstractContainerMenuMixin.java"), StandardCharsets.UTF_8);
        assertTrue(mixins.contains("AbstractContainerMenuMixin"));
        assertTrue(source.contains("ClickType.PICKUP"));
        assertTrue(source.contains("button != 1"));
        assertTrue(source.contains("AbstractContainerMenu menu"));
        assertTrue(source.contains("menu.getCarried()"));
        assertTrue(source.contains("menu.setCarried(carried)"));
        assertTrue(source.contains("menu.getSlot(slotId)"));
        assertTrue(source.contains("method = {\"clicked\", \"m_150399_\"}"));
        assertTrue(source.contains("IndexOutOfBoundsException"));
        assertTrue(source.contains("target.isActive()"));
        assertTrue(source.contains("target.mayPickup(player)"));
        assertTrue(source.contains("target.mayPlace(carried)"));
        assertTrue(source.contains("ResourceInsertionService.insert"));
        assertTrue(source.contains("target.setChanged()"));
        String service = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/runtime/ResourceInsertionService.java"), StandardCharsets.UTF_8);
        assertTrue(service.contains("recipe.cost().containsKey(resourceId)"));
        assertTrue(service.contains("AmmoId"));
        assertTrue(service.contains("ResourceLocation.tryParse"));
    }

    @Test
    void devAutomationExercisesARealChestMenuPath() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("SimpleContainer"));
        assertTrue(source.contains("ChestMenu.threeRows"));
        assertTrue(source.contains("containerMenu.clicked"));
        assertTrue(source.contains("ClickType.PICKUP"));
        assertTrue(source.contains("standard container insertion"));
        assertTrue(source.contains("data.remove(STANDARD_CONTAINER_PROBE)"));
        int containerProbe = source.indexOf("probeStandardContainerInsertion(serverPlayer, stack)");
        int craftedGate = source.indexOf("getPersistentData().getBoolean(WORKBENCH_CRAFTED)");
        assertTrue(containerProbe >= 0 && craftedGate >= 0 && containerProbe < craftedGate,
                "standard container probe must run before the workbench-only probes");
    }

    @Test
    void devAutomationKeepsSophisticatedBackpacksOptionalAndExercisesItsMenuWhenPresent() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/pycoder/taczintetra/client/DevServerAutomation.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("sophisticatedbackpacks"), "optional backpack item id must remain data-driven");
        assertTrue(source.contains("PENDING_BACKPACK_CLIENT_PROBES"));
        assertTrue(source.contains("broadcastFullState"));
        assertTrue(source.contains("client probe"));
        assertTrue(source.contains("probeSophisticatedBackpackInsertion(serverPlayer, stack)"));
        assertTrue(source.contains("SOPHISTICATED_BACKPACK_PROBE"));
        assertTrue(source.contains("container != player.getInventory()"));
        assertTrue(source.contains("player.containerMenu.clicked(targetSlot, 1, ClickType.PICKUP, player)"));
        assertTrue(source.contains("player.closeContainer()"));
    }
}
