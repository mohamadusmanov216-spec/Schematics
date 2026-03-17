package sbuild.block;

import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.StairShape;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Places stairs with consistent half/shape/facing.
 */
public final class StairsPlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().getBlock() instanceof StairsBlock;
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing().getOpposite();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        BlockHalf half = context.desiredState().contains(Properties.BLOCK_HALF)
            ? context.desiredState().get(Properties.BLOCK_HALF)
            : BlockHalf.BOTTOM;
        return new PlacementPlan(
            context.desiredState()
                .with(Properties.HORIZONTAL_FACING, lookDirection)
                .with(Properties.BLOCK_HALF, half)
                .with(Properties.STAIR_SHAPE, StairShape.STRAIGHT),
            lookDirection,
            faceToClick
        );
    }
}
