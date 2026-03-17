package sbuild.block;

import net.minecraft.block.Blocks;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Places comparators with expected mode and orientation.
 */
public final class ComparatorPlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().isOf(Blocks.COMPARATOR);
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing().getOpposite();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        ComparatorMode mode = context.desiredState().contains(Properties.COMPARATOR_MODE)
            ? context.desiredState().get(Properties.COMPARATOR_MODE)
            : ComparatorMode.COMPARE;
        return new PlacementPlan(
            context.desiredState().with(Properties.HORIZONTAL_FACING, lookDirection).with(Properties.COMPARATOR_MODE, mode),
            lookDirection,
            faceToClick
        );
    }
}
