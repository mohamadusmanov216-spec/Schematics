package sbuild.block;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Places hoppers with output direction based on click face.
 */
public final class HopperPlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().isOf(Blocks.HOPPER);
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing().getOpposite();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        Direction outputDirection = faceToClick == Direction.UP ? Direction.DOWN : faceToClick;
        return new PlacementPlan(
            context.desiredState().with(Properties.HOPPER_FACING, outputDirection),
            lookDirection,
            faceToClick
        );
    }
}
