package com.pycoder.taczintetra.resource;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlaceholderResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");

    @Test
    void allRequestedItemModelsUseTransparentPlaceholder() throws Exception {
        List<String> ids = new java.util.ArrayList<>(List.of("starter_pistol", "pistol_body", "pistol_barrel", "pistol_magazine", "pistol_optic", "pistol_stock", "pistol_grip"));
        for (String material : List.of("wood", "stone", "iron", "gold", "netherite")) {
            ids.add(material + "_body");
            ids.add(material + "_barrel");
            ids.add(material + "_magazine");
        }
        for (String id : ids) {
            String model = Files.readString(ROOT.resolve("assets/taczintetra/models/item/" + id + ".json"), StandardCharsets.UTF_8);
            assertTrue(model.contains("taczintetra:item/blank"), id);
        }
        Path placeholder = ROOT.resolve("assets/taczintetra/textures/item/blank.png");
        assertTrue(Files.exists(placeholder));
        BufferedImage image = ImageIO.read(placeholder.toFile());
        assertTrue(image != null && image.getWidth() > 0 && image.getHeight() > 0);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                assertTrue((image.getRGB(x, y) >>> 24) == 0,
                        "placeholder pixel is not transparent at " + x + "," + y);
            }
        }
    }

    @Test
    void chineseNamesUseRequestedTerminology() throws Exception {
        String lang = Files.readString(ROOT.resolve("assets/taczintetra/lang/zh_cn.json"), StandardCharsets.UTF_8);
        assertTrue(lang.contains("TaCZ模块化枪"));
        assertTrue(lang.contains("\"枪管\""));
        assertTrue(lang.contains("\"枪身\""));
        assertTrue(lang.contains("\"弹匣\""));
        assertTrue(lang.contains("\"倍镜\""));
        assertTrue(lang.contains("\"枪托\""));
        assertTrue(lang.contains("\"握把\""));
    }
}
