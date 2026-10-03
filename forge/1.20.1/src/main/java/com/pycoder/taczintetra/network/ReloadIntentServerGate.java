package com.pycoder.taczintetra.network;

/** 服务端重放请求门禁；实际装填结算仍由独立步骤处理。 */
public final class ReloadIntentServerGate {
    private long mainHandSequence;
    private long offHandSequence;
    private long mainHandBeforeReservation;
    private long offHandBeforeReservation;

    public boolean accept(ReloadIntentMessage intent, int heldStackIdentity, boolean hasGun,
                          boolean otherHandReloading, boolean broken, boolean overheatLocked) {
        long last = intent != null && intent.offHand() ? offHandSequence : mainHandSequence;
        if (!ReloadIntentPolicy.canAccept(intent, heldStackIdentity, last, hasGun,
                otherHandReloading, broken, overheatLocked)) {
            return false;
        }
        if (intent.offHand()) {
            offHandBeforeReservation = offHandSequence;
            offHandSequence = intent.sequence();
        } else {
            mainHandBeforeReservation = mainHandSequence;
            mainHandSequence = intent.sequence();
        }
        return true;
    }

    /** 生命周期启动失败时，回滚最近一次资源预留。 */
    public void rollback(ReloadIntentMessage intent) {
        if (intent == null) return;
        if (intent.offHand() && offHandSequence == intent.sequence()) {
            offHandSequence = offHandBeforeReservation;
        } else if (!intent.offHand() && mainHandSequence == intent.sequence()) {
            mainHandSequence = mainHandBeforeReservation;
        }
    }
}
