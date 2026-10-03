package com.pycoder.taczintetra.network;

/** 仅在服务端验证换弹意图的传输层不变量。 */
public final class ReloadIntentValidator {
    private ReloadIntentValidator() {
    }

    public static boolean isValid(ReloadIntentMessage message) {
        return message != null && message.stackIdentity() >= 0 && message.sequence() > 0;
    }
}
