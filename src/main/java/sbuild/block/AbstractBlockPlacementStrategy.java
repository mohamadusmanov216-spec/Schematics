package sbuild.block;

import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.Direction;

/**
 * Base helper implementation for orientation-aware placement strategies.
 */
public abstract class AbstractBlockPlacementStrategy implements BlockPlacementStrategy {
    @Override
    public PlacementContext determinePlacementContext(PlacementContext rawContext) {
        return rawContext;
    }

    @Override
    public Direction determineFaceToClick(PlacementContext context, Direction lookDirection) {
        return lookDirection.getOpposite();
    }

    @Override
    public boolean performPlacement(PlacementContext context, PlacementPlan plan) {
        ServerWorld world = context.world();
        if (!world.isAir(context.targetPos())) {
            return false;
        }
        context.player().setYaw(yawFromDirection(plan.lookDirection()));
        return world.setBlockState(context.targetPos(), plan.blockState());
    }

    @Override
    public boolean validateResult(PlacementContext context, PlacementPlan plan) {
        BlockState placed = context.world().getBlockState(context.targetPos());
        if (placed.getBlock() != plan.blockState().getBlock()) {
            return false;
        }
        for (Property<?> property : plan.blockState().getProperties()) {
            if (!placed.contains(property)) {
                return false;
            }
            if (!placed.get(property).equals(plan.blockState().get(property))) {
                return false;
            }
        }
        return true;
    }

    protected float yawFromDirection(Direction direction) {
        return switch (direction) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case EAST -> -90.0F;
            default -> 0.0F;
        };
    }
}
