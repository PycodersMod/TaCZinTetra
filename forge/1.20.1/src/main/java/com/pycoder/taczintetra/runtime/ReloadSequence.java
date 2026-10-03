package com.pycoder.taczintetra.runtime;

/** 生成正数装填意图序号，并避免有符号溢出。 */
public final class ReloadSequence {
    private ReloadSequence() {
    }

    public static int next(int current) {
        return current >= Integer.MAX_VALUE ? 1 : Math.max(0, current) + 1;
    }
}
