package com.pycoder.taczintetra.network;

import com.pycoder.taczintetra.config.ModuleConfig;

/** 足够稳定的会话摘要，用于拒绝未提示的客户端/服务端配置差异。 */
public final class ConfigDigest {
    private ConfigDigest() { }

    public static String of(ModuleConfig config) {
        return Integer.toUnsignedString(config == null ? 0 : config.hashCode(), 16);
    }
}
