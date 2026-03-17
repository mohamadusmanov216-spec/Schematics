package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Tries to recover bot/player from invalid runtime conditions.
 */
public final class RecoveryController {
    public void recover(ServerWorld world, ServerPlayerEntity player) {
        player.setVelocity(0.0D, 0.0D, 0.0D);
        player.velocityModified = true;
        player.fallDistance = 0.0F;

        BlockPos safe = findSafeGround(world, player.getBlockPos());
        player.requestTeleport(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);
    }

    private BlockPos findSafeGround(ServerWorld world, BlockPos from) {
        int minY = world.getBottomY() + 1;
        for (int y = from.getY(); y >= minY; y--) {
            BlockPos candidate = new BlockPos(from.getX(), y, from.getZ());
            if (!world.isAir(candidate) && world.isAir(candidate.up())) {
                return candidate.up();
            }
        }
        return world.getSpawnPos();
    }
}
