package com.pycoder.taczintetra.logic;

/** 判定一组外部扣款能否安全开始。 */
public final class ExternalResourceTransactionPolicy {
    private ExternalResourceTransactionPolicy() {
    }

    public static boolean canBegin(int externalOperations, boolean allSupportRollback) {
        if (externalOperations <= 1) return true;
        return allSupportRollback;
    }
}
