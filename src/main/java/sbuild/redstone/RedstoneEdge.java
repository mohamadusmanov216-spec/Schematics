package sbuild.redstone;

import net.minecraft.util.math.Direction;

/**
 * Directed redstone dependency connection.
 */
public record RedstoneEdge(
    RedstoneNode from,
    RedstoneNode to,
    Direction direction,
    int weight,
    boolean repeaterReset
) {
}
