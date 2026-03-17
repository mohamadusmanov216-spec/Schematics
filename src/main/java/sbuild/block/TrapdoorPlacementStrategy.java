package sbuild.block;

import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Places trapdoors with expected side and half.
 */
public final class TrapdoorPlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().getBlock() instanceof TrapdoorBlock;
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        BlockHalf half = context.desiredState().contains(Properties.BLOCK_HALF)
            ? context.desiredState().get(Properties.BLOCK_HALF)
            : BlockHalf.BOTTOM;
        return new PlacementPlan(
            context.desiredState()
                .with(Properties.HORIZONTAL_FACING, lookDirection)
                .with(Properties.BLOCK_HALF, half),
            lookDirection,
            faceToClick
        );
    }
}
