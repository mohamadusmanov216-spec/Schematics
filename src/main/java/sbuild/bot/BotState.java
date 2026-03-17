package sbuild.bot;

import net.minecraft.util.math.BlockPos;

/**
 * Immutable runtime snapshot for a builder bot.
 */
public record BotState(Status status, BlockPos targetPos, String message, long tick) {
    public enum Status {
        IDLE,
        MOVING,
        LOOKING,
        PLACING,
        BREAKING,
        SCAFFOLDING,
        RECOVERING,
        ERROR
    }

    public static BotState idle(long tick) {
        return new BotState(Status.IDLE, null, "idle", tick);
    }

    public BotState with(Status nextStatus, BlockPos nextTarget, String nextMessage, long nextTick) {
        return new BotState(nextStatus, nextTarget, nextMessage, nextTick);
    }
}
