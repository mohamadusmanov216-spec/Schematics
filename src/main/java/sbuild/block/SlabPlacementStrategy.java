package sbuild.block;

import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.Direction;

/**
 * Places slab in correct top/bottom/double mode.
 */
public final class SlabPlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().getBlock() instanceof SlabBlock;
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        SlabType type = context.desiredState().contains(Properties.SLAB_TYPE)
            ? context.desiredState().get(Properties.SLAB_TYPE)
            : SlabType.BOTTOM;
        return new PlacementPlan(
            context.desiredState().with(Properties.SLAB_TYPE, type),
            lookDirection,
            faceToClick
        );
    }
}
