package com.pycoder.taczintetra.logic;

/** 将部分创建的弹丸批次换算回独立弹数。 */
public final class ShotSettlementPolicy {
    private ShotSettlementPolicy() {
    }

    public static int consumedRounds(int requestedRounds, int requestedProjectiles,
                                     int createdProjectiles) {
        if (requestedRounds <= 0 || requestedProjectiles <= 0 || createdProjectiles <= 0) {
            return 0;
        }
        int safeCreated = Math.min(requestedProjectiles, createdProjectiles);
        long rounded = ((long) safeCreated * requestedRounds + requestedProjectiles - 1L)
                / requestedProjectiles;
        return (int) Math.max(0, Math.min(requestedRounds, rounded));
    }
}
