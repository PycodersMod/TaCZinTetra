package com.pycoder.taczintetra.client;

/** Keeps the isolated survival probe from advancing on a stale or dead integrated-server player. */
final class SurvivalProbeRecoveryPolicy {
    private SurvivalProbeRecoveryPolicy() {
    }

    static boolean shouldRecover(boolean serverPlayerAlive, boolean recoveryAlreadyRequested) {
        return !serverPlayerAlive && !recoveryAlreadyRequested;
    }

    static boolean shouldResetAfterRecovery(boolean clientPlayerAlive, boolean serverPlayerAlive,
                                            boolean recoveryWasRequested) {
        return clientPlayerAlive && serverPlayerAlive && recoveryWasRequested;
    }
}
