package com.pycoder.taczintetra.runtime;

import com.pycoder.taczintetra.network.ReloadIntentMessage;

/** 将已解析的客户端输入转换为仅用于传输的意图。 */
public final class ReloadIntentFactory {
    private ReloadIntentFactory() {
    }

    public static ReloadIntentMessage create(GunInputRouter.Decision decision,
                                             int stackIdentity, int sequence) {
        if (decision == null) {
            throw new IllegalArgumentException("decision");
        }
        return new ReloadIntentMessage(decision.routeReloadToOffHand(),
                !decision.fillReload(), stackIdentity, sequence);
    }
}
