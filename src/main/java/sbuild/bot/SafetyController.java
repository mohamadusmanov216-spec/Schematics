package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/**
 * Performs safety checks before bot actions are executed.
 */
public final class SafetyController {
    public boolean isSafeTarget(ServerWorld world, ServerPlayerEntity player, BlockPos target) {
        if (!world.getWorldBorder().contains(target)) {
            return false;
        }
        if (!world.isChunkLoaded(target)) {
            return false;
        }

        int topY = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ()) + 1;
        return target.getY() > world.getBottomY() + 1 && target.getY() < topY;
    }

    public boolean shouldRecover(ServerPlayerEntity player) {
        return player.isGliding() || player.fallDistance > 6.0F;
    }
}
