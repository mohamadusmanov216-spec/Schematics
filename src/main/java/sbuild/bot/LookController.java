package sbuild.bot;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Controls camera rotation for precise bot actions.
 */
public final class LookController {
    public void lookAt(ServerPlayerEntity player, Vec3d point) {
        Vec3d eyePos = player.getEyePos();
        Vec3d dir = point.subtract(eyePos);
        double horizontal = Math.sqrt(dir.x * dir.x + dir.z * dir.z);

        float yaw = (float) (MathHelper.atan2(dir.z, dir.x) * (180.0F / Math.PI)) - 90.0F;
        float pitch = (float) (-(MathHelper.atan2(dir.y, horizontal) * (180.0F / Math.PI)));

        player.setYaw(yaw);
        player.setPitch(pitch);
        player.setHeadYaw(yaw);
        player.setBodyYaw(yaw);
    }

    public void lookAtBlock(ServerPlayerEntity player, BlockPos blockPos) {
        lookAt(player, Vec3d.ofCenter(blockPos));
    }
}
