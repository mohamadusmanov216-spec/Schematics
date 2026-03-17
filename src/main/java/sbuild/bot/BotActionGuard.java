package sbuild.bot;

/**
 * Tracks action lifecycle and bounded retry/timeout semantics.
 */
final class BotActionGuard {
    enum FailureDecision {
        RETRY,
        FAIL
    }

    private final int maxRetries;
    private Object activeAction;
    private long actionStartedAt;
    private int retryCount;

    BotActionGuard(int maxRetries, long nowTick) {
        this.maxRetries = maxRetries;
        this.actionStartedAt = nowTick;
    }

    void begin(Object action, long nowTick) {
        if (activeAction == action) {
            return;
        }
        activeAction = action;
        actionStartedAt = nowTick;
        retryCount = 0;
    }

    boolean isTimedOut(long nowTick, int timeoutTicks) {
        if (activeAction == null) {
            return false;
        }
        return nowTick - actionStartedAt > timeoutTicks;
    }

    FailureDecision onFailure(long nowTick) {
        if (retryCount < maxRetries) {
            retryCount++;
            actionStartedAt = nowTick;
            return FailureDecision.RETRY;
        }
        reset(nowTick);
        return FailureDecision.FAIL;
    }

    void reset(long nowTick) {
        activeAction = null;
        retryCount = 0;
        actionStartedAt = nowTick;
    }

    int retryCount() {
        return retryCount;
    }
}
