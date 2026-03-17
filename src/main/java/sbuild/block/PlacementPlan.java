package sbuild.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;

/**
 * Prepared plan for a placement attempt.
 */
public record PlacementPlan(
    BlockState blockState,
    Direction lookDirection,
    Direction faceToClick
) {
}
