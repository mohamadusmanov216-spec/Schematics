package sbuild.redstone;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * A redstone component node in dependency graph.
 */
public record RedstoneNode(
    BlockPos pos,
    BlockState state,
    Type type
) {
    public enum Type {
        WIRE,
        REPEATER,
        COMPARATOR,
        OBSERVER,
        POWER_SOURCE,
        CONSUMER,
        OTHER
    }

    public RedstoneNode {
        pos = pos.toImmutable();
    }
}
