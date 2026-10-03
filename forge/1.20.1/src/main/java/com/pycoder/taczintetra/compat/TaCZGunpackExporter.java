package com.pycoder.taczintetra.compat;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** 将内置的最小 TaCZ 内容包导出到 TaCZ 原生外部内容包目录。 */
public final class TaCZGunpackExporter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String RESOURCE_PREFIX = "/assets/tacz/custom/taczintetra/";
    private static final String PACK_DIRECTORY = "taczintetra";
    private static final String[] FILES = {
            "gunpack.meta.json",
            "data/taczintetra/data/guns/modular_gun_data.json",
            "data/taczintetra/index/guns/modular_gun.json",
            "assets/tacz/display/guns/modular_gun_display.json",
            "assets/taczintetra/gunpack_info.json",
            "assets/taczintetra/lang/zh_cn.json",
            "assets/taczintetra/lang/en_us.json"
    };

    private TaCZGunpackExporter() {
    }

    public static void export() {
        Path packRoot = FMLPaths.GAMEDIR.get().resolve("tacz").resolve(PACK_DIRECTORY);
        try {
            for (String relative : FILES) {
                Path target = packRoot.resolve(relative);
                Files.createDirectories(target.getParent());
                try (InputStream source = TaCZGunpackExporter.class.getResourceAsStream(RESOURCE_PREFIX + relative)) {
                    if (source == null) {
                        throw new IOException("missing bundled resource " + relative);
                    }
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            LOGGER.info("Exported built-in TaCZ gunpack to {}", packRoot);
        } catch (IOException exception) {
            LOGGER.warn("Unable to export built-in TaCZ gunpack to {}", packRoot, exception);
        }
    }
}
