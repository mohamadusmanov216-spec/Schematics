package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Handles block breaking operations for bot corrections.
 */
public final class BreakController {
    public boolean breakBlock(ServerWorld world, ServerPlayerEntity player, BlockPos pos) {
        if (world.isAir(pos)) {
            return true;
        }
        return world.breakBlock(pos, true, player);
    }
}
