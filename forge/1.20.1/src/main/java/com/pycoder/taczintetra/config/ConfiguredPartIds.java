package com.pycoder.taczintetra.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** 将已配置的成品部件资源 ID 转换为本地注册表路径。 */
public final class ConfiguredPartIds {
    private static final String LOCAL_NAMESPACE = "taczintetra";

    private ConfiguredPartIds() { }

    public static List<String> localPaths(ModuleConfig config) {
        LinkedHashSet<String> paths = new LinkedHashSet<>();
        if (config == null) return List.of();
        config.materials().forEach(material -> material.physicalPartItems().forEach(id -> {
            int separator = id.indexOf(':');
            if (separator <= 0 || separator == id.length() - 1) return;
            if (LOCAL_NAMESPACE.equals(id.substring(0, separator))) paths.add(id.substring(separator + 1));
        }));
        return List.copyOf(new ArrayList<>(paths));
    }
}
