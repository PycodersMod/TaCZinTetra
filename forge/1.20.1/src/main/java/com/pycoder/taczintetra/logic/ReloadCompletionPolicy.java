package com.pycoder.taczintetra.logic;

/** 类事务式换弹结果；调用者仅在动画完成后应用。 */
public final class ReloadCompletionPolicy {
    private ReloadCompletionPolicy() {
    }

    public static Result commit(boolean completed, int resourceAmount, int loadedAmmo,
                                int paidBatches) {
        if (!completed) {
            return new Result(Math.max(0, resourceAmount), Math.max(0, loadedAmmo), 0);
        }
        int batches = Math.max(0, paidBatches);
        int remainingResources = saturateNonNegative((long) Math.max(0, resourceAmount) - batches);
        int settledAmmo = saturateNonNegative((long) Math.max(0, loadedAmmo) + batches);
        return new Result(remainingResources, settledAmmo, batches);
    }

    private static int saturateNonNegative(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, value));
    }

    public record Result(int resourceAmount, int loadedAmmo, int paidBatches) {
    }
}
