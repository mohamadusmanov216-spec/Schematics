package sbuild.block;

import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Immutable input context used by block placement strategies.
 */
public record PlacementContext(
    ServerWorld world,
    ServerPlayerEntity player,
    BlockPos targetPos,
    BlockState desiredState
) {
    public PlacementContext {
        targetPos = targetPos.toImmutable();
    }
}
