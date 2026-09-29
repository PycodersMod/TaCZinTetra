package com.pycoder.taczintetra.logic;

/** Decides whether a set of external debits can be safely started. */
public final class ExternalResourceTransactionPolicy {
    private ExternalResourceTransactionPolicy() {
    }

    public static boolean canBegin(int externalOperations, boolean allSupportRollback) {
        if (externalOperations <= 1) return true;
        return allSupportRollback;
    }
}
