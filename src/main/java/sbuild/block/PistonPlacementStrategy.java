package sbuild.block;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Places pistons with facing towards push direction.
 */
public final class PistonPlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().isOf(Blocks.PISTON) || context.desiredState().isOf(Blocks.STICKY_PISTON);
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        return new PlacementPlan(
            context.desiredState().with(Properties.FACING, lookDirection),
            lookDirection,
            faceToClick
        );
    }
}
