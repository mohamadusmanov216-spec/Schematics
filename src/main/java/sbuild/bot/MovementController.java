package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Handles low-level movement updates towards a target block.
 */
public final class MovementController {
    public boolean moveTowards(ServerPlayerEntity player, BlockPos target, double speedPerTick) {
        Vec3d playerPos = player.getPos();
        Vec3d targetCenter = Vec3d.ofCenter(target);
        Vec3d delta = targetCenter.subtract(playerPos);
        double distance = delta.length();

        if (distance < 0.35D) {
            player.setVelocity(Vec3d.ZERO);
            return true;
        }

        Vec3d step = delta.normalize().multiply(Math.min(speedPerTick, distance));
        player.setVelocity(step.x, player.getVelocity().y, step.z);
        player.velocityModified = true;
        return false;
    }
}
