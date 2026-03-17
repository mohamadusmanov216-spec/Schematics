package sbuild.block;

import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Places repeaters with horizontal facing and delay.
 */
public final class RepeaterPlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().isOf(Blocks.REPEATER);
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing().getOpposite();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        int delay = context.desiredState().contains(Properties.DELAY) ? context.desiredState().get(Properties.DELAY) : 1;
        return new PlacementPlan(
            context.desiredState().with(Properties.HORIZONTAL_FACING, lookDirection).with(Properties.DELAY, delay),
            lookDirection,
            faceToClick
        );
    }
}
