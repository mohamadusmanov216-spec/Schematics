package sbuild.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.WireConnection;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * Places wire and derives side connections from neighbors.
 */
public final class RedstoneWirePlacementStrategy extends AbstractBlockPlacementStrategy {
    @Override
    public boolean supports(PlacementContext context) {
        return context.desiredState().isOf(Blocks.REDSTONE_WIRE);
    }

    @Override
    public Direction determinePlayerLookDirection(PlacementContext context) {
        return context.player().getHorizontalFacing();
    }

    @Override
    public PlacementPlan createPlan(PlacementContext context, Direction lookDirection, Direction faceToClick) {
        BlockState state = context.desiredState();
        for (Direction direction : Direction.Type.HORIZONTAL) {
            BlockPos neighborPos = context.targetPos().offset(direction);
            BlockState neighbor = context.world().getBlockState(neighborPos);
            WireConnection connection = neighbor.isAir() ? WireConnection.NONE : WireConnection.SIDE;
            state = switch (direction) {
                case NORTH -> state.with(Properties.NORTH_WIRE_CONNECTION, connection);
                case SOUTH -> state.with(Properties.SOUTH_WIRE_CONNECTION, connection);
                case EAST -> state.with(Properties.EAST_WIRE_CONNECTION, connection);
                case WEST -> state.with(Properties.WEST_WIRE_CONNECTION, connection);
                default -> state;
            };
        }
        return new PlacementPlan(state.with(Properties.POWER, 0), lookDirection, faceToClick);
    }
}
